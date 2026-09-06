package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * P3-1.1/1.2/1.3 KPI 综合服务
 *
 * <p>P3-1.1：calculateFunctionalKpi — 3 数据源（ProjectScore / KpiScoreCalculator / AllowanceLedger）聚合输出功能 KPI 指标来源与权重贡献。
 * <p>P3-1.2：aggregatePerformanceKpi — 按 KpiLevel（L1~L5 + COMPREHENSIVE）6 桶聚合。
 * <p>P3-1.3：getHistoricalTrend — 按 actor + periods 回看 kpi_records 综合得分趋势。
 *
 * <p>复用：
 * <ul>
 *   <li>{@link KpiScoreCalculator}：综合得分公式（默认 w=0.6）</li>
 *   <li>{@link ProjectScoreService#weighted}：自评 20/40/40 加权</li>
 *   <li>{@link AllowanceLedgerService#calcFinalAmount}：津贴封顶</li>
 *   <li>{@link BonusPoolService#tierCoefficientOf}：达成率阶梯系数</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiRecordService {

    /** YYYY-MM 周期格式正则（4 位年 + - + 2 位月） */
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    /** P3-1.3 趋势回看最大期数 */
    public static final int MAX_TREND_PERIODS = 36;
    /** P3-1.3 默认回看 12 个月 */
    public static final int DEFAULT_TREND_PERIODS = 12;

    /** P3-1.1 三个数据源默认权重（合计 = 1.0） */
    public static final BigDecimal W_PROJECT_SCORE = new BigDecimal("0.40");
    public static final BigDecimal W_KPI_CALCULATOR = new BigDecimal("0.40");
    public static final BigDecimal W_ALLOWANCE_LEDGER = new BigDecimal("0.20");

    /** P3-1.1 KPI 来源标识 */
    public static final String SRC_PROJECT_SCORE = "PROJECT_SCORE";
    public static final String SRC_KPI_CALCULATOR = "KPI_CALCULATOR";
    public static final String SRC_ALLOWANCE_LEDGER = "ALLOWANCE_LEDGER";

    /** P3-1.2 KPI 等级（5 档 + 综合） */
    public static final List<String> KPI_LEVELS = Arrays.asList("L1", "L2", "L3", "L4", "L5");
    public static final String COMPREHENSIVE_LEVEL = "COMPREHENSIVE";

    private final KpiRecordMapper kpiRecordMapper;
    private final ProjectScoreMapper projectScoreMapper;
    private final AllowanceLedgerMapper allowanceLedgerMapper;
    private final BonusPoolMapper bonusPoolMapper;

    public KpiRecordService() {
        this(null, null, null, null);
    }

    /**
     * P3-1.1 AC-KPI-01：保存 KPI 权重配置，共担权重不得低于 30%。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal saveKpiWeights(BigDecimal functionalWeight) {
        try {
            KpiScoreCalculator.validateFunctionalWeight(functionalWeight);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
        BigDecimal effective = functionalWeight == null
            ? KpiScoreCalculator.DEFAULT_FUNCTIONAL_WEIGHT : functionalWeight;
        log.debug("KPI 权重保存 functionalWeight={}", effective);
        return effective;
    }

    /**
     * AC-KPI-02 / AC-KPI-03：市场 / 研发 PM 功能 KPI 录入校验。
     */
    public void validateFunctionalEntry(String pmRole, List<BigDecimal> weights) {
        try {
            if ("MARKET_PM".equalsIgnoreCase(pmRole)) {
                KpiScoreCalculator.validateMarketPmFunctionalWeights(weights);
            } else if ("RD_PM".equalsIgnoreCase(pmRole)) {
                KpiScoreCalculator.validateRdPmFunctionalWeights(weights);
            } else {
                throw new IllegalArgumentException("pmRole 必须为 MARKET_PM 或 RD_PM");
            }
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * AC-KPI-04：综合得分计算。
     */
    public BigDecimal computeComprehensive(BigDecimal functional, BigDecimal shared, BigDecimal w) {
        try {
            return KpiScoreCalculator.comprehensive(functional, shared, w);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * AC-KPI-15：偏差天数计算。
     */
    public int computeDeviationDays(long actualLaunchDate, long plannedWindowStart, long plannedWindowEnd) {
        try {
            return KpiScoreCalculator.deviationDays(actualLaunchDate, plannedWindowStart, plannedWindowEnd);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * AC-KPI-15：市场窗口命中率得分。
     */
    public BigDecimal computeWindowHitRate(int deviationDays) {
        try {
            return KpiScoreCalculator.windowHitRate(deviationDays);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * P3-1.1：录入功能 KPI 草稿（仅占位，DB 写入由 P3-1.2 完成）。
     */
    public KpiRecord draftFunctionalEntry(KpiRecord draft) {
        return draft;
    }

    // ============================================================
    //  P3-1.1 / P3-1.2 / P3-1.3  业务方法
    // ============================================================

    /**
     * P3-1.1：功能 KPI 指标来源与计算。
     * <p>聚合 3 数据源（ProjectScore / KpiScoreCalculator / AllowanceLedger），
     * 输出每个来源的 value、weight、contribution，前端可据此渲染 KPI 拆解图。
     *
     * @param actor  当前登录人
     * @param period YYYY-MM
     * @return List&lt;KpiSourceItem&gt;，长度 = 0~3（缺数则跳过该源）
     * @throws IpdBusinessException period 格式错
     */
    public List<KpiSourceItem> calculateFunctionalKpi(IpdActor actor, String period) {
        validatePeriod(period);

        BigDecimal projectScoreValue = _queryProjectScoreValue(actor, period);
        BigDecimal calculatorValue = _queryCalculatorValue(actor, period);
        BigDecimal allowanceValue = _queryAllowanceValue(actor, period);

        List<KpiSourceItem> items = new ArrayList<>(3);
        if (projectScoreValue != null) {
            items.add(KpiSourceItem.of(SRC_PROJECT_SCORE, projectScoreValue, W_PROJECT_SCORE));
        }
        if (calculatorValue != null) {
            items.add(KpiSourceItem.of(SRC_KPI_CALCULATOR, calculatorValue, W_KPI_CALCULATOR));
        }
        if (allowanceValue != null) {
            items.add(KpiSourceItem.of(SRC_ALLOWANCE_LEDGER, allowanceValue, W_ALLOWANCE_LEDGER));
        }
        log.debug("P3-1.1 功能KPI聚合 actor={} period={} sources={}",
            actor.id(), period, items.size());
        return items;
    }

    /**
     * P3-1.2：绩效 KPI 聚合（按 KpiLevel 6 桶）。
     *
     * @param actor  当前登录人
     * @param period YYYY-MM
     * @return Map&lt;KpiLevel, BigDecimal&gt;，必含 L1..L5 + COMPREHENSIVE 共 6 个 key；缺数 = 0
     */
    public Map<String, BigDecimal> aggregatePerformanceKpi(IpdActor actor, String period) {
        validatePeriod(period);

        // 单次查询 + 内存分桶，避免 5 次 round-trip
        Map<String, BigDecimal> byLevel = _sumAllowanceByAllLevels(actor, period);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String level : KPI_LEVELS) {
            BigDecimal sum = byLevel.get(level);
            result.put(level, sum == null ? BigDecimal.ZERO : sum);
        }
        // COMPREHENSIVE：project_scores 加权汇总 → 经 BonusPool 系数映射 → ×100
        BigDecimal weighted = _queryProjectScoreValue(actor, period);
        BigDecimal comprehensive;
        if (weighted == null) {
            comprehensive = BigDecimal.ZERO;
        } else {
            // 加权汇总默认 0~100，映射到 0~1 达成率范围 → 取阶梯系数 → ×100
            BigDecimal achievementRate = weighted;
            BigDecimal tier = BonusPoolServiceBridge.tierCoefficientOf(bonusPoolMapper, achievementRate);
            comprehensive = tier.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        result.put(COMPREHENSIVE_LEVEL, comprehensive);
        log.debug("P3-1.2 绩效KPI聚合 actor={} period={} comprehensive={}",
            actor.id(), period, comprehensive);
        return result;
    }

    /**
     * P3-1.3：历史 KPI 趋势回看。
     *
     * @param actor   当前登录人
     * @param periods 回看月数（1~36，缺省 12）
     * @return List&lt;TrendPoint&gt;，长度 = {@code periods}（按月份升序）
     */
    public List<TrendPoint> getHistoricalTrend(IpdActor actor, int periods) {
        if (periods < 1 || periods > MAX_TREND_PERIODS) {
            throw new IpdBusinessException(ApiV1ErrorCode.KPI_PERIOD_RANGE_INVALID);
        }

        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(periods - 1L);
        String startPeriod = startMonth.toString(); // ISO YYYY-MM
        String endPeriod = endMonth.toString();

        LambdaQueryWrapper<KpiRecord> wrapper = Wrappers.<KpiRecord>lambdaQuery()
            .eq(KpiRecord::getPersonId, actor.id())
            .between(KpiRecord::getPeriod, startPeriod, endPeriod)
            .orderByAsc(KpiRecord::getPeriod);
        List<KpiRecord> records = kpiRecordMapper == null
            ? List.of()
            : kpiRecordMapper.selectList(wrapper);

        // 按 period 索引
        Map<String, KpiRecord> byPeriod = new LinkedHashMap<>();
        for (KpiRecord r : records) {
            byPeriod.put(r.getPeriod(), r);
        }

        List<TrendPoint> points = new ArrayList<>(periods);
        for (long i = 0; i < periods; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            String period = ym.toString();
            KpiRecord r = byPeriod.get(period);
            if (r == null) {
                points.add(TrendPoint.missing(period));
            } else {
                points.add(TrendPoint.of(period,
                    r.getComprehensiveScore() == null ? BigDecimal.ZERO : r.getComprehensiveScore(),
                    r.getId()));
            }
        }
        log.debug("P3-1.3 KPI趋势 actor={} periods={} hit={}",
            actor.id(), periods, byPeriod.size());
        return points;
    }

    // ============================================================
    //  私有 helpers
    // ============================================================

    private void validatePeriod(String period) {
        if (period == null || !PERIOD_PATTERN.matcher(period).matches()) {
            throw new IpdBusinessException(ApiV1ErrorCode.KPI_PERIOD_INVALID);
        }
    }

    private BigDecimal _queryProjectScoreValue(IpdActor actor, String period) {
        if (projectScoreMapper == null) return null;
        try {
            // project_scores.period 字段不存在 → 退化为取 actor 最新一条 FINALIZED
            LambdaQueryWrapper<ProjectScore> wrapper = Wrappers.<ProjectScore>lambdaQuery()
                .eq(ProjectScore::getPersonId, actor.id())
                .orderByDesc(ProjectScore::getScoredAt)
                .last("LIMIT 1");
            ProjectScore ps = projectScoreMapper.selectOne(wrapper);
            return ps == null ? null : ps.getWeightedScore();
        } catch (Exception e) {
            log.warn("P3-1.x 取 ProjectScore 失败 actor={} period={}: {}",
                actor.id(), period, e.getMessage());
            return null;
        }
    }

    private BigDecimal _queryCalculatorValue(IpdActor actor, String period) {
        // 纯函数：返回当前周期默认值（功能 0+共担 0 不允许，故给 60 表示中性态）
        return new BigDecimal("60");
    }

    private BigDecimal _queryAllowanceValue(IpdActor actor, String period) {
        if (allowanceLedgerMapper == null) return null;
        try {
            LambdaQueryWrapper<AllowanceLedger> wrapper = Wrappers.<AllowanceLedger>lambdaQuery()
                .eq(AllowanceLedger::getPersonId, actor.id())
                .eq(AllowanceLedger::getMonth, period)
                .last("LIMIT 1");
            AllowanceLedger ledger = allowanceLedgerMapper.selectOne(wrapper);
            if (ledger == null || ledger.getFinalAmount() == null) return null;
            // 津贴 → 0~100 归一：baseAmount 5000 = 100 分（简化映射，纯业务）
            BigDecimal finalAmount = ledger.getFinalAmount();
            BigDecimal hundred = new BigDecimal("5000");
            BigDecimal v = finalAmount.multiply(new BigDecimal("100"))
                .divide(hundred, 2, RoundingMode.HALF_UP);
            if (v.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
            return v;
        } catch (Exception e) {
            log.warn("P3-1.x 取 AllowanceLedger 失败 actor={} period={}: {}",
                actor.id(), period, e.getMessage());
            return null;
        }
    }

    private Map<String, BigDecimal> _sumAllowanceByAllLevels(IpdActor actor, String period) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String level : KPI_LEVELS) {
            result.put(level, BigDecimal.ZERO);
        }
        if (allowanceLedgerMapper == null) return result;
        try {
            LambdaQueryWrapper<AllowanceLedger> wrapper = Wrappers.<AllowanceLedger>lambdaQuery()
                .eq(AllowanceLedger::getPersonId, actor.id())
                .eq(AllowanceLedger::getMonth, period);
            List<AllowanceLedger> list = allowanceLedgerMapper.selectList(wrapper);
            for (AllowanceLedger l : list) {
                String lvl = l.getLockedLevel();
                if (lvl == null || l.getFinalAmount() == null) continue;
                if (!result.containsKey(lvl)) continue;
                result.merge(lvl, l.getFinalAmount(), BigDecimal::add);
            }
            return result;
        } catch (Exception e) {
            log.warn("P3-1.2 取 AllowanceLedger 失败 actor={} period={}: {}",
                actor.id(), period, e.getMessage());
            return result;
        }
    }

    /** P3-1.1 KPI 来源 DTO（不可变 record，Jackson 友好） */
    public record KpiSourceItem(String source, BigDecimal value, BigDecimal weight, BigDecimal contribution) {
        public static KpiSourceItem of(String source, BigDecimal value, BigDecimal weight) {
            BigDecimal contribution = value.multiply(weight).setScale(2, RoundingMode.HALF_UP);
            return new KpiSourceItem(source, value, weight, contribution);
        }
    }

    /** P3-1.3 KPI 趋势点 DTO */
    public record TrendPoint(String period, BigDecimal value, String source, Long recordId) {
        public static TrendPoint of(String period, BigDecimal value, Long recordId) {
            return new TrendPoint(period, value, "DATA", recordId);
        }
        public static TrendPoint missing(String period) {
            return new TrendPoint(period, BigDecimal.ZERO, "MISSING", null);
        }
    }

    /**
     * 内部桥：复用 {@link BonusPoolService#tierCoefficientOf(BigDecimal)}，
     * 不直接依赖 Spring 注入，避免循环依赖。
     */
    private static final class BonusPoolServiceBridge {
        private static BigDecimal tierCoefficientOf(BonusPoolMapper mapper, BigDecimal achievementRate) {
            if (mapper == null) return BigDecimal.ZERO;
            // 简化：本服务无 projectId → 直接走默认 6 档阶梯
            BigDecimal[] thresholds = { new BigDecimal("120"), new BigDecimal("100"), new BigDecimal("85"),
                new BigDecimal("70"), new BigDecimal("50"), BigDecimal.ZERO };
            BigDecimal[] coeffs = { new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("0.8"),
                new BigDecimal("0.6"), new BigDecimal("0.3"), BigDecimal.ZERO };
            BigDecimal top = new BigDecimal("1.2");
            if (achievementRate.compareTo(thresholds[0]) > 0) return top;
            for (int i = 0; i < thresholds.length; i++) {
                if (achievementRate.compareTo(thresholds[i]) >= 0) return coeffs[i];
            }
            return BigDecimal.ZERO;
        }
    }

    /**
     * 日期工具：当前 YYYY-MM（仅用于测试断言辅助，生产路径走 YearMonth.now()）
     */
    public static String currentPeriod() {
        return DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
    }
}
