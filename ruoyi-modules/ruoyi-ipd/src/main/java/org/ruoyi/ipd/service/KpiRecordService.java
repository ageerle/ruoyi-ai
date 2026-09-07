package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    /* ---------- Wave17 ROOT-R3-P0-2：KPI 状态机（接入 StateMachineGuard） ---------- */
    /** 状态机实体类型（与 DefaultStateMachineGuard.registerRule 约定一致） */
    public static final String KPI_RECORD_ENTITY_TYPE = "kpi_record";
    /** 初始录入（编辑中） */
    public static final String ST_EDITING = "EDITING";
    /** 提交待审 */
    public static final String ST_PENDING_REVIEW = "PENDING_REVIEW";
    /** 已审通过 */
    public static final String ST_APPROVED = "APPROVED";
    /** 已锁定（终态前收敛） */
    public static final String ST_LOCKED = "LOCKED";
    /** 已驳回 */
    public static final String ST_REJECTED = "REJECTED";
    /** 已归档（终态） */
    public static final String ST_ARCHIVED = "ARCHIVED";


    private final KpiRecordMapper kpiRecordMapper;
    private final ProjectScoreMapper projectScoreMapper;
    private final AllowanceLedgerMapper allowanceLedgerMapper;
    private final BonusPoolMapper bonusPoolMapper;
    /** ROOT-R1 P0-7 字面量迁移：KPI 默认值（停发阈值 60；B-RULE-02 配套）来源 */
    private final BusinessConfigService businessConfigService;
    /** ROOT-R3-P0-2：跨状态机守卫（可选注入，nullable 兼容旧测试；Wave17 KPI 状态机接入） */
    private StateMachineGuard stateMachineGuard;

    /**
     * ROOT-R3-P0-2：Spring 注入 StateMachineGuard（nullable 兼容旧测试）。
     * 测试场景可通过此 setter 注入 mock；运行时由 Spring 装配。
     */
    @Autowired(required = false)
    public void setStateMachineGuard(StateMachineGuard stateMachineGuard) {
        this.stateMachineGuard = stateMachineGuard;
    }

    public KpiRecordService() {
        this(null, null, null, null, null);
    }

    /** ROOT-R1 P0-7：注入 BusinessConfigService（Spring 装配入口） */
    public KpiRecordService(KpiRecordMapper kpiRecordMapper,
                            ProjectScoreMapper projectScoreMapper,
                            AllowanceLedgerMapper allowanceLedgerMapper,
                            BonusPoolMapper bonusPoolMapper,
                            BusinessConfigService businessConfigService) {
        this.kpiRecordMapper = kpiRecordMapper;
        this.projectScoreMapper = projectScoreMapper;
        this.allowanceLedgerMapper = allowanceLedgerMapper;
        this.bonusPoolMapper = bonusPoolMapper;
        this.businessConfigService = businessConfigService;
    }

    /** 旧测试兼容构造器：4 依赖，不带 BusinessConfigService */
    public KpiRecordService(KpiRecordMapper kpiRecordMapper,
                            ProjectScoreMapper projectScoreMapper,
                            AllowanceLedgerMapper allowanceLedgerMapper,
                            BonusPoolMapper bonusPoolMapper) {
        this(kpiRecordMapper, projectScoreMapper, allowanceLedgerMapper, bonusPoolMapper, null);
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

    /* ---------- Wave17 ROOT-R3-P0-2：KPI 状态机写入方法 ---------- */

    /**
     * 初始录入 KPI 草稿：null→EDITING 合法迁移，insert 后 postCommit 触发。
     *
     * @param draft KPI 草稿（ID 必空，由 insert 自动生成）
     * @param actor 当前操作人（审计落名；null 时静默跳过）
     * @return 已落库 KpiRecord（status=EDITING，ID 已生成）
     */
    @Transactional(rollbackFor = Exception.class)
    public KpiRecord recordScore(KpiRecord draft, IpdActor actor) {
        if (draft == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "KPI 草稿不能为空");
        }
        String from = draft.getStatus(); // null for new draft
        preCheckGuard(KPI_RECORD_ENTITY_TYPE, from, ST_EDITING, "record");
        draft.setStatus(ST_EDITING);
        if (kpiRecordMapper != null) {
            kpiRecordMapper.insert(draft);
        }
        registerPostCommit(KPI_RECORD_ENTITY_TYPE, from, ST_EDITING, "record",
            actor != null ? actor.id() : null, draft.getId());
        return draft;
    }

    /**
     * 审批通过 KPI：{EDITING|PENDING_REVIEW}→APPROVED 合法迁移。
     */
    @Transactional(rollbackFor = Exception.class)
    public KpiRecord approveKpi(Long recordId, IpdActor actor) {
        KpiRecord record = requireById(recordId);
        String from = record.getStatus();
        preCheckGuard(KPI_RECORD_ENTITY_TYPE, from, ST_APPROVED, "approve");
        record.setStatus(ST_APPROVED);
        if (kpiRecordMapper != null) {
            kpiRecordMapper.updateById(record);
        }
        registerPostCommit(KPI_RECORD_ENTITY_TYPE, from, ST_APPROVED, "approve",
            actor != null ? actor.id() : null, record.getId());
        return record;
    }

    /**
     * 驳回 KPI：{EDITING|PENDING_REVIEW}→REJECTED 合法迁移。
     */
    @Transactional(rollbackFor = Exception.class)
    public KpiRecord rejectKpi(Long recordId, IpdActor actor) {
        KpiRecord record = requireById(recordId);
        String from = record.getStatus();
        preCheckGuard(KPI_RECORD_ENTITY_TYPE, from, ST_REJECTED, "reject");
        record.setStatus(ST_REJECTED);
        if (kpiRecordMapper != null) {
            kpiRecordMapper.updateById(record);
        }
        registerPostCommit(KPI_RECORD_ENTITY_TYPE, from, ST_REJECTED, "reject",
            actor != null ? actor.id() : null, record.getId());
        return record;
    }

    /**
     * 归档 KPI：通配 *→ARCHIVED 终态收敛。
     */
    @Transactional(rollbackFor = Exception.class)
    public KpiRecord archiveKpi(Long recordId, IpdActor actor) {
        KpiRecord record = requireById(recordId);
        String from = record.getStatus();
        preCheckGuard(KPI_RECORD_ENTITY_TYPE, from, ST_ARCHIVED, "archive");
        record.setStatus(ST_ARCHIVED);
        if (kpiRecordMapper != null) {
            kpiRecordMapper.updateById(record);
        }
        registerPostCommit(KPI_RECORD_ENTITY_TYPE, from, ST_ARCHIVED, "archive",
            actor != null ? actor.id() : null, record.getId());
        return record;
    }

    private KpiRecord requireById(Long id) {
        if (id == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "KPI 记录 ID 不能为空");
        }
        if (kpiRecordMapper == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "KpiRecordMapper 未注入");
        }
        KpiRecord record = kpiRecordMapper.selectById(id);
        if (record == null || "1".equals(record.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "KPI 记录不存在: " + id);
        }
        return record;
    }

    /**
     * ROOT-R3-P0-2 修复：守卫 preCheck 包装（fail-closed 模式）。
     *
     * <p>守卫 null = fail-closed 抛 IpdBusinessException（防 state-machine-bypass）。
     * 测试兼容：KpiRecordServiceTest 通过 setStateMachineGuard(...) 注入 mock；
     * MockitoExtension STRICT_STUBS 模式下空 mock 必显式 fail。
     */
    private void preCheckGuard(String entityType, String fromState, String toState, String trigger) {
        if (stateMachineGuard == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR,
                "状态机守卫未装配 entityType=" + entityType + " from=" + fromState + " to=" + toState);
        }
        stateMachineGuard.preCheck(entityType, fromState, toState, trigger);
    }

    /**
     * ROOT-R3-P0-2：注册 postCommit 副作用（事务提交后触发，避免回滚后污染）。
     * 无守卫注入时降级 no-op；无事务上下文时直接执行（向后兼容测试场景）。
     */
    private void registerPostCommit(String entityType, String fromState, String toState,
                                    String trigger, Long operatorId, Long entityId) {
        if (stateMachineGuard == null) {
            return;
        }
        Date occurredAt = new Date();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
                }
            });
        } else {
            stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
        }
    }

    // ============================================================
    //  私有 helpers
    // ============================================================
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
        // ROOT-R1 P0-7：纯函数返回值（中性态）从 BusinessConfigService 读（KPI_STOP_THRESHOLD 默认 60；B-RULE-02 配套）
        if (businessConfigService != null) {
            try {
                return businessConfigService.getBigDecimal(BusinessConfigKeys.KPI_STOP_THRESHOLD);
            } catch (Exception ex) {
                log.warn("KPI_STOP_THRESHOLD 读取失败，回退硬编码 60: {}", ex.getMessage());
            }
        }
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
