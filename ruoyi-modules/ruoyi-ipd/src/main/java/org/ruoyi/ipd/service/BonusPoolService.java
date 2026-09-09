package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.service.ProjectScoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BonusAllocation;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.BonusAllocationMapper;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 奖金池服务（P3-4.2/4.3/4.4；AC-INC-16/17/18/19/20/21；BR-INC-04/05/06；ZK-IPD-2026-09-06-补）
 *
 * <p><b>口径裁决（2026-09-06 owner 拍板，[CONSISTENCY-1]）</b>：奖金池基数 = <b>上市后连续 6 个月实际回款净额</b> × 5% × S/A/B 系数。
 * ZK-IPD 完整版 Prompt §三.2 vs 主Prompt Q1+AC-INC-16b 文档分裂结论：取 ZK 口径作为权威（与 owner「严格禁止与 ZK-IPD 不一致」红线一致），
 * Controller {@link #compute(Long, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, IpdActor)}
 * 入口即用 {@link #calculateBonusPoolByZkFormulaWithModifiers}。{@code targetSales} 字段在 BonusPool 实体层仅作历史兼容保留，
 * 不再作为权威基数来源——落库时由 compute() 写入实际回款值（语义已在 BonusPoolController javadoc 登记）。AC-INC-16 用例需 owner 复审按新口径重写。
 *
 * <p>核心规则（AC-INC-17/17b~17h + AC-INC-18~21 + ZK-IPD Prompt §三.2）：
 * <ul>
 *   <li><b>ZK-INC-16</b>（口径更新）：奖金池基数 = 实际回款（actualReceipts）× 5%（DEFAULT_POOL_RATE），不是目标销售额；达成率分子分母同为回款口径（AC-INC-16b）</li>
 *   <li>AC-INC-17~21：达成率阶梯系数，严格按 {@code 达成率 ≥ 阈值} 从高到低匹配；
 *       区间下端点含、上端点不含；不允许浮点等值判定（不写浮点字面量等值 / 不引入容差参数）</li>
 *   <li>AC-INC-17h：默认六档 [{Infinity,1.2},{120,1.0},{100,1.0},{85,0.8},{70,0.6},{50,0.3},{0,0.0}]</li>
 *   <li>AC-INC-20：达成率 60% 命中 0.3 档，触发复盘检讨提醒（reviewRequired=true）</li>
 *   <li>AC-INC-21：达成率 45% 命中 0 档，不发放；已发月度津贴不追回（独立规则）</li>
 *   <li><b>ZK-IPD §三.2.1</b>：奖金池 = 上市后连续 6 个月<b>实际回款</b>金额 × 5% × <b>项目 S/A/B 差异化系数</b>（coefficient，非 tierCoefficient）</li>
 *   <li><b>ZK-IPD §三.2.5</b>：可叠加 销售达成率阶梯系数 + 个人绩效系数（4 因子全叠加）</li>
 *   <li><b>ZK-IPD §三.2.4</b>：市场 PM 40-65% / 研发 PM 35-60%，上市 90 天复盘三方评定</li>
 *   <li><b>P3-4.4</b>：状态机 DRAFT → CONFIRMED → DISTRIBUTED；HTTP 端点收口（compute / freeze / distribute / getById / listByProject）</li>
 * </ul>
 */
@Service
public class BonusPoolService {

    private final BonusPoolMapper bonusPoolMapper;
    private final ProjectMapper projectMapper;
    /** [SEC-FIX-HIGH-5.2] 自动推导 personalCoefficient 所需依赖。 */
    private final KpiRecordMapper kpiRecordMapper;
    private ProjectScoreService projectScoreService;
    /** ROOT-R3-P0-1：跨状态机守卫（可选注入，nullable 兼容旧测试） */
    @Autowired(required = false)
    private org.ruoyi.ipd.service.StateMachineGuard stateMachineGuard;

    /** P-DATA-gap-1 接线（A3 裁决）：distribute 批量写 bonus_allocations 台账（可选注入，nullable 兼容旧测试） */
    @Autowired(required = false)
    private BonusAllocationMapper bonusAllocationMapper;
    @Autowired(required = false)
    private ProjectMemberMapper projectMemberMapper;
    @Autowired(required = false)
    private ContributionMapper contributionMapper;

    /** P-DATA-gap-1 接线：测试显式注入入口（对齐 setStateMachineGuard 模式）。 */
    public void setBonusAllocationMapper(BonusAllocationMapper bonusAllocationMapper) {
        this.bonusAllocationMapper = bonusAllocationMapper;
    }

    public void setProjectMemberMapper(ProjectMemberMapper projectMemberMapper) {
        this.projectMemberMapper = projectMemberMapper;
    }

    public void setContributionMapper(ContributionMapper contributionMapper) {
        this.contributionMapper = contributionMapper;
    }
    /** ROOT-R3-P0-1 修复：Spring 注入 StateMachineGuard（fail-closed 改造后，测试可显式注入 mock） */
    public void setStateMachineGuard(org.ruoyi.ipd.service.StateMachineGuard stateMachineGuard) {
        this.stateMachineGuard = stateMachineGuard;
    }

    /**
     * 兼容构造器：仅注入 BonusPoolMapper 的旧测试入口。
     */
    public BonusPoolService(BonusPoolMapper bonusPoolMapper) {
        this(bonusPoolMapper, null, null, null);
    }

    /**
     * 兼容构造器：双 Mapper 注入（保持兄弟流测试不破）。
     */
    public BonusPoolService(BonusPoolMapper bonusPoolMapper, ProjectMapper projectMapper) {
        this(bonusPoolMapper, projectMapper, null, null);
    }

    /**
     * 兼容构造器：三依赖注入。
     */
    public BonusPoolService(BonusPoolMapper bonusPoolMapper, ProjectMapper projectMapper, KpiRecordMapper kpiRecordMapper) {
        this(bonusPoolMapper, projectMapper, kpiRecordMapper, null);
    }

    /**
     * Spring 装配入口（4 依赖注入）；[SEC-FIX-HIGH-5.2] 加 KpiRecordMapper + ProjectScoreService。
     * 多构造器必须显式 @Autowired 标记，否则上下文无法实例化。
     */
    @Autowired
    public BonusPoolService(BonusPoolMapper bonusPoolMapper,
                            ProjectMapper projectMapper,
                            KpiRecordMapper kpiRecordMapper,
                            ProjectScoreService projectScoreService) {
        this.bonusPoolMapper = bonusPoolMapper;
        this.projectMapper = projectMapper;
        this.kpiRecordMapper = kpiRecordMapper;
        this.projectScoreService = projectScoreService;
    }

    /** AC-INC-17h：默认六档阶梯（按阈值降序；第一个达成率 ≥ 阈值命中） */
    private static final BigDecimal[] DEFAULT_THRESHOLDS = {
        new BigDecimal("120"), new BigDecimal("100"), new BigDecimal("85"),
        new BigDecimal("70"), new BigDecimal("50"), BigDecimal.ZERO
    };
    private static final BigDecimal[] DEFAULT_COEFFICIENTS = {
        new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("0.8"),
        new BigDecimal("0.6"), new BigDecimal("0.3"), BigDecimal.ZERO
    };
    /** Infinity 档系数（达成率 > 120% 即命中 1.2） */
    private static final BigDecimal TOP_COEFFICIENT = new BigDecimal("1.2");

    /**
     * AC-INC-17/17b~17h + AC-INC-18~21：
     * 按 {@code 达成率 ≥ 阈值} 从高到低匹配，返回对应阶梯系数。
     * 区间下端点含（如 100%、120%）、上端点不含（如 120% 含但 120.01% 走 1.2 档）。
     */
    public BigDecimal tierCoefficientOf(BigDecimal achievementRate) {
        if (achievementRate == null) {
            throw new IllegalArgumentException("达成率不能为空");
        }
        if (achievementRate.compareTo(DEFAULT_THRESHOLDS[0]) > 0) {
            return TOP_COEFFICIENT;
        }
        for (int i = 0; i < DEFAULT_THRESHOLDS.length; i++) {
            if (achievementRate.compareTo(DEFAULT_THRESHOLDS[i]) >= 0) {
                return DEFAULT_COEFFICIENTS[i];
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * AC-INC-20：达成率 60% 命中 0.3 档，触发复盘检讨提醒
     * 阈值规则：达成率 ∈ [50%, 70%) 中命中 0.3 档 → 需复盘
     */
    public boolean isReviewRequired(BigDecimal achievementRate) {
        if (achievementRate == null) {
            return false;
        }
        return achievementRate.compareTo(new BigDecimal("50")) >= 0
            && achievementRate.compareTo(new BigDecimal("70")) < 0;
    }

    /* --------------------------- P3-4.5 项目绩效系数分档 + 取数策略路由 --------------------------- */
    /* AC-INC-22/23/24；BR-INC-07；分档阈值与系数来源：ProjectScoreService.projectPerformanceCoefficient() */

    /** P3-4.5 BR-INC-07：取数策略常量（直接复用 ProjectScoreService，避免双源定义漂移） */
    public static final String STRATEGY_PROJECT_SCORE = ProjectScoreService.STRATEGY_PROJECT_SCORE;
    public static final String STRATEGY_WEIGHTED_AVG = ProjectScoreService.STRATEGY_WEIGHTED_AVG;
    public static final String STRATEGY_LAST_QUARTER = ProjectScoreService.STRATEGY_LAST_QUARTER;

    /** P3-4.5 BR-INC-07：综合得分合法区间 [0, 100] */
    public static final BigDecimal PERF_SCORE_MIN = BigDecimal.ZERO;
    public static final BigDecimal PERF_SCORE_MAX = new BigDecimal("100");

    /**
     * P3-4.5 AC-INC-22/23/24 + BR-INC-07：按综合得分 + 取数策略查项目绩效系数。
     *
     * <p>策略路由（当前 MVP）：调用方传入 score（语义由 strategy 决定）：
     * <ul>
     *   <li>PROJECT_SCORE：当期综合得分</li>
     *   <li>WEIGHTED_AVG：多期加权平均分</li>
     *   <li>LAST_QUARTER：上季度综合得分</li>
     * </ul>
     *
     * <p>分档映射委托 {@link ProjectScoreService#projectPerformanceCoefficient(BigDecimal)}。
     * 严禁出现"以能力等级 L1–L5 代替绩效得分"的入参——能力等级与绩效得分独立（AC-INC-22b）。
     */
    public BigDecimal calculatePerformanceCoefficient(BigDecimal score, String strategy) {
        if (score == null) {
            throw new ServiceException("P3-4.5：综合得分不能为空");
        }
        if (score.compareTo(PERF_SCORE_MIN) < 0 || score.compareTo(PERF_SCORE_MAX) > 0) {
            throw new ServiceException("P3-4.5：综合得分必须在 [0, 100] 区间，当前=" + score.toPlainString());
        }
        ProjectScoreService.validateProjectPerfStrategy(strategy);
        return projectScoreService.projectPerformanceCoefficient(score);
    }

    /**
     * P3-4.5 BR-INC-07：从 system_configs 读取当前取数策略。
     *
     * <p>策略配置键：{@code bonus.performance.strategy}；缺省 = PROJECT_SCORE。
     */
    public BigDecimal resolvePerformanceCoefficient(Long projectId, BigDecimal score) {
        String strategy = STRATEGY_PROJECT_SCORE;
        if (businessConfigService != null) {
            try {
                String cfg = businessConfigService.getString(BusinessConfigKeys.BONUS_PERFORMANCE_STRATEGY);
                if (cfg != null && !cfg.isBlank()) {
                    strategy = cfg;
                }
            } catch (Exception ex) {
                // 配置读取失败静默回退默认策略
            }
        }
        return calculatePerformanceCoefficient(score, strategy);
    }

    /**
     * P3-4.5 preview 端点契约：预览系数（仅查表/计算，不写 audit、不落库）。
     *
     * @return 预览 BonusPool（tierCode 已设置；finalPool = 0 表示无金额）
     */
    public BonusPool previewCoefficient(Long projectId, BigDecimal score, String strategy, IpdActor actor) {
        BigDecimal coef = calculatePerformanceCoefficient(score, strategy);
        BonusPool preview = new BonusPool();
        preview.setProjectId(projectId);
        preview.setTierCoefficient(coef);
        preview.setFinalPool(BigDecimal.ZERO);
        preview.setStatus(STATUS_DRAFT);
        return preview;
    }

    /**
     * @deprecated 口径废弃（[CONSISTENCY-1] 2026-09-06 owner 裁决）：
     * 原 AC-INC-16「目标销售额 × 5%」被 ZK 完整版 Prompt「实际回款 × 5%」覆盖。
     * Controller {@link #compute} 主入口已迁移至 {@link #calculateBonusPoolByZkFormulaWithModifiers}，
     * 本方法保留实现仅供旧 P343 测试与历史查询回放兼容；新代码禁止调用。
     * 替代：{@link #calculateBonusPoolByZkFormula(BigDecimal, BigDecimal)}
     */
    @Deprecated
    public BigDecimal calculateBasePool(BigDecimal targetSales, BigDecimal poolRate) {
        if (targetSales == null || targetSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = (poolRate != null) ? poolRate : new BigDecimal("0.05");
        return targetSales.multiply(rate);
    }

    /* --------------------------- P3-4.2 奖金池基数+系数可配置 --------------------------- */
    /* BR-INC-04：bonus.poolRate 实时读；非法拒绝；计算记录参数版本/输入/Decimal 舍入。 */
    /* BR-INC-05：项目 S/A/B 系数（coefficient）：S=1.5~2.0；A=1.0；B=0.6~0.8。 */

    /** P3-4.2 BR-INC-04：默认 poolRate = 5%（保留兼容；运行时由 BusinessConfigService.BONUS_POOL_RATE 覆盖） */
    public static final BigDecimal DEFAULT_CONFIG_POOL_RATE = new BigDecimal("0.0500");

    /** P3-4.2 BR-INC-05：项目 S/A/B 系数合法区段 */
    public static final BigDecimal COEFFICIENT_S_MIN = new BigDecimal("1.5");
    public static final BigDecimal COEFFICIENT_S_MAX = new BigDecimal("2.0");
    public static final BigDecimal COEFFICIENT_A = new BigDecimal("1.0");
    public static final BigDecimal COEFFICIENT_B_MIN = new BigDecimal("0.6");
    public static final BigDecimal COEFFICIENT_B_MAX = new BigDecimal("0.8");

    /** P3-4.2 BR-INC-04：poolRate 合法区间 (0, 1]，默认 0.0500 */
    public static final BigDecimal POOL_RATE_MIN = new BigDecimal("0.0001");
    public static final BigDecimal POOL_RATE_MAX = new BigDecimal("1.0000");

    /** P3-4.2：精度（保留 4 位小数） */
    public static final int POOL_RATE_SCALE = 4;

    /** [SEC-FIX-HIGH-5.2-FOLLOWUP] resolvePersonalCoefficient 合理上限（一期一个 PM ≈ 12 行） */
    public static final long RESOLVE_PERSONAL_COEFFICIENT_MAX_ROWS = 12L;

    /**
     * [SEC-FIX-HIGH-5.2-FOLLOWUP] actualReceipts 合理上限。
     * 单项目上市后连续 6 个月实际回款净额，按行业天花板取 1 万亿元（10^12）。
     * 超出此值即视为参数异常（注入/笔误），拒绝计算。
     */
    public static final BigDecimal ACTUAL_RECEIPTS_MAX = new BigDecimal("1000000000000");

    /**
     * ROOT-R1 P0-7：业务参数读取服务（奖金池比例/阶梯系数；B-RULE-01 配套）。
     * 通过 setter 注入（兼容旧测试构造器），Spring 自动装配。
     */
    private BusinessConfigService businessConfigService;

    /**
     * ROOT-R1 P0-7：Spring 注入 BusinessConfigService（nullable 兼容旧测试）。
     */
    @Autowired(required = false)
    public void setBusinessConfigService(BusinessConfigService businessConfigService) {
        this.businessConfigService = businessConfigService;
    }

    /**
     * P3-4.5：项目绩效系数分档依赖注入（兼容旧测试构造器）。
     * 4 参构造器未注入时为 null；P345AcceptanceTest 等单测通过 setter 注入 mock。
     */
    @Autowired(required = false)
    public void setProjectScoreService(ProjectScoreService projectScoreService) {
        this.projectScoreService = projectScoreService;
    }

    /**
     * ROOT-R1 P0-7：读取奖金池比例（poolRate）。优先 BusinessConfigService.BONUS_POOL_RATE，回退 DEFAULT_CONFIG_POOL_RATE。
     */
    public BigDecimal readActivePoolRate() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getBigDecimal(BusinessConfigKeys.BONUS_POOL_RATE);
            } catch (Exception ex) {
                // fall through to default
            }
        }
        return DEFAULT_CONFIG_POOL_RATE;
    }

    /**
     * ROOT-R1 P0-7：读取奖金池比例（兼容旧 0.05 默认值；与 readActivePoolRate 同源）。
     */
    public BigDecimal readPoolRateZk() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getBigDecimal(BusinessConfigKeys.BONUS_POOL_RATE);
            } catch (Exception ex) {
                // fall through to default
            }
        }
        return DEFAULT_POOL_RATE;
    }

    /**
     * P3-4.2 BR-INC-04：校验 poolRate 合法性。
     * <ul>
     *   <li>null ⇒ 走默认 0.05</li>
     *   <li>0 < poolRate ≤ 1</li>
     *   <li>精度 ≤ 4 位小数</li>
     * </ul>
     */
    public static void validatePoolRate(BigDecimal poolRate) {
        if (poolRate == null) {
            return;
        }
        if (poolRate.compareTo(POOL_RATE_MIN) < 0) {
            throw new IpdBusinessException(
                "P3-4.2：poolRate 必须 > 0，当前=" + poolRate);
        }
        if (poolRate.compareTo(POOL_RATE_MAX) > 0) {
            throw new IpdBusinessException(
                "P3-4.2：poolRate 必须 ≤ 1，当前=" + poolRate);
        }
        if (poolRate.scale() > POOL_RATE_SCALE) {
            throw new IpdBusinessException(
                "P3-4.2：poolRate 精度超过 4 位小数，当前 scale=" + poolRate.scale());
        }
    }

    /**
     * P3-4.2 BR-INC-05：校验项目 S/A/B 系数合法性。
     */
    public static void validateProjectCoefficient(String projectLevel, BigDecimal coefficient) {
        if (coefficient == null) {
            throw new IpdBusinessException("P3-4.2：项目系数不能为空");
        }
        String lvl = projectLevel == null ? "UNKNOWN" : projectLevel;
        if ("S".equals(lvl)) {
            if (coefficient.compareTo(COEFFICIENT_S_MIN) < 0
                || coefficient.compareTo(COEFFICIENT_S_MAX) > 0) {
                throw new IpdBusinessException(
                    "P3-4.2：S 级项目系数必须在 [1.5, 2.0]，当前=" + coefficient);
            }
        } else if ("A".equals(lvl)) {
            if (coefficient.compareTo(COEFFICIENT_A) != 0) {
                throw new IpdBusinessException(
                    "P3-4.2：A 级项目系数必须 = 1.0，当前=" + coefficient);
            }
        } else if ("B".equals(lvl)) {
            if (coefficient.compareTo(COEFFICIENT_B_MIN) < 0
                || coefficient.compareTo(COEFFICIENT_B_MAX) > 0) {
                throw new IpdBusinessException(
                    "P3-4.2：B 级项目系数必须在 [0.6, 0.8]，当前=" + coefficient);
            }
        } else {
            throw new IpdBusinessException(
                "P3-4.2：未知项目等级 " + lvl + "（应为 S/A/B）");
        }
    }

    /**
     * P3-4.2 BR-INC-04：读取最新 poolRate 配置（实时读开关）。
     * <p>实际实现见本类顶部 readActivePoolRate()；此处仅保留旧位置 javadoc 留档。
     */

    /**
     * @deprecated 口径废弃（[CONSISTENCY-1] 2026-09-06 owner 裁决）：
     * 旧 P3-4.2「targetSales × poolRate」被 ZK 完整版 Prompt「actualReceipts × poolRate」覆盖（奖金池基数 = 上市后连续 6 个月实际回款）。
     * 计算逻辑保留以便 P342 测试回归；新代码请改用 {@link #calculateBonusPoolByZkFormula}。
     */
    @Deprecated
    public BigDecimal calculateBasePoolConfigurable(BigDecimal targetSales) {
        if (targetSales == null || targetSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal activeRate = readActivePoolRate();
        validatePoolRate(activeRate);
        return targetSales.multiply(activeRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * @deprecated 口径废弃（[CONSISTENCY-1] 2026-09-06 owner 裁决）。保留实现供 P342/P343 历史用例回归；
     * 新代码请改用 {@link #calculateBonusPoolByZkFormula}。
     */
    @Deprecated
    public BigDecimal calculateBasePoolWithRate(BigDecimal targetSales, BigDecimal poolRate) {
        validatePoolRate(poolRate);
        if (targetSales == null || targetSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = (poolRate != null) ? poolRate : readActivePoolRate();
        return targetSales.multiply(rate)
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * P3-4.2 BR-INC-05：项目 S/A/B 系数（按 projectLevel 决定合法区段）
     */
    public BigDecimal getDefaultCoefficientFor(String projectLevel) {
        if ("S".equals(projectLevel)) {
            return new BigDecimal("1.8"); // S 级默认 1.8
        }
        if ("A".equals(projectLevel)) {
            return COEFFICIENT_A;
        }
        if ("B".equals(projectLevel)) {
            return new BigDecimal("0.7"); // B 级默认 0.7
        }
        throw new IpdBusinessException("P3-4.2：未知项目等级 " + projectLevel);
    }

    /**
     * AC-INC-16 + AC-INC-17：最终奖金池 = 基础奖金池 × 阶梯系数
     */
    public BigDecimal calculateFinalPool(BigDecimal basePool, BigDecimal tierCoefficient) {
        if (basePool == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal coef = (tierCoefficient != null) ? tierCoefficient : BigDecimal.ZERO;
        return basePool.multiply(coef);
    }

    /**
     * @deprecated 口径废弃（[CONSISTENCY-1] 2026-09-06 owner 裁决）。本方法走旧 targetSales 路径已被 ZK 实际回款路径替代；
     * BonusPool 实体的 targetSales 字段仅作历史兼容保留，不再作为权威基数来源。新代码请改用 {@link #buildPoolFromProject}。
     */
    @Deprecated
    public BonusPool fillDerivedFields(BonusPool pool) {
        BigDecimal base = calculateBasePool(pool.getTargetSales(), pool.getPoolRate());
        pool.setBasePool(base);
        BigDecimal tier = tierCoefficientOf(pool.getAchievementRate());
        pool.setTierCoefficient(tier);
        pool.setFinalPool(calculateFinalPool(base, tier));
        return pool;
    }

    @Transactional(rollbackFor = Exception.class)
    public BonusPool save(BonusPool pool) {
        fillDerivedFields(pool);
        bonusPoolMapper.insert(pool);
        return pool;
    }

    public List<BonusPool> listByProject(Long projectId) {
        return bonusPoolMapper.selectList(
            new LambdaQueryWrapper<BonusPool>()
                .eq(BonusPool::getProjectId, projectId)
                .orderByDesc(BonusPool::getCalculatedAt));
    }

    /* ----- ZK-IPD Prompt §三.2.1：奖金池 = 实际回款 × 5% × 项目 S/A/B 差异化系数 ----- */
    /* 差异矩阵 2026-09-06 P0 项：原有 calculateBasePool/FinalPool 用 targetSales +
       tierCoefficient，违反 ZK-IPD 公式口径。新增方法用 ZK-IPD 口径作为权威计算路径。 */

    /** ZK-IPD §三.2.1 默认 poolRate = 5% */
    public static final BigDecimal DEFAULT_POOL_RATE = new BigDecimal("0.05");

    /**
     * ZK-IPD §三.2.1：奖金池 = 实际回款 × poolRate × S/A/B 差异化系数
     *
     * <p>与既有 {@link #calculateBasePool} 的区别：
     * <ul>
     *   <li>基数：实际回款（actualReceipts），不是目标销售额（targetSales）</li>
     *   <li>乘数：项目 S/A/B 差异化系数（coefficient），不是达成率阶梯（tierCoefficient）</li>
     * </ul>
     *
     * @param actualReceipts   上市后连续 6 个月实际回款净额
     * @param levelCoefficient 项目 S/A/B 差异化系数（S 1.5–2.0 / B 0.6–0.8 / A 固定 1.0）
     * @return 奖金池金额；回款 ≤ 0 返回 ZERO；入参空抛 ServiceException
     */
    public BigDecimal calculateBonusPoolByZkFormula(BigDecimal actualReceipts, BigDecimal levelCoefficient) {
        if (actualReceipts == null) {
            throw new ServiceException("ZK-IPD §三.2.1：实际回款金额不能为空");
        }
        if (levelCoefficient == null) {
            throw new ServiceException("ZK-IPD §三.2.1：项目 S/A/B 差异化系数不能为空");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("ZK-IPD §三.2.1：实际回款金额不能为负");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return actualReceipts.multiply(readPoolRateZk()).multiply(levelCoefficient);
    }

    /**
     * ZK-IPD §三.2.1：构造 BonusPool（系数取自 Project.levelCoefficient，非 tierCoefficient）。
     *
     * <p>差异矩阵 2026-09-06 P0 项：既有 {@link #fillDerivedFields} 不读 project.levelCoefficient，
     * 本方法补齐该字段孤岛——BonusPool.coefficient 必须 = Project.levelCoefficient。
     *
     * @param projectId      项目 ID
     * @param actualReceipts 实际回款金额
     * @param calculatedAt   计算时间
     * @param poolRate       奖金池比例（默认 5%）
     * @return 新建 BonusPool（未持久化）
     */
    public BonusPool buildPoolFromProject(Long projectId, BigDecimal actualReceipts,
                                          Date calculatedAt, BigDecimal poolRate) {
        if (projectMapper == null) {
            throw new ServiceException("ProjectMapper 未注入，无法读取项目 S/A/B 差异化系数（ZK-IPD §三.2.1）");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        BigDecimal levelCoefficient = project.getLevelCoefficient();
        if (levelCoefficient == null) {
            throw new ServiceException("项目 S/A/B 差异化系数未配置（level=" + project.getLevel() + "），无法按 ZK-IPD §三.2.1 计算奖金池");
        }
        BigDecimal rate = (poolRate != null) ? poolRate : readPoolRateZk();
        BigDecimal pool = calculateBonusPoolByZkFormula(actualReceipts, levelCoefficient);
        return BonusPool.builder()
            .projectId(projectId)
            .targetSales(actualReceipts)
            .poolRate(rate)
            .basePool(actualReceipts.multiply(rate))
            .coefficient(levelCoefficient)
            .achievementRate(null)
            .tierCoefficient(null)
            .finalPool(pool)
            .calculatedAt(calculatedAt)
            .status("DRAFT")
            .build();
    }

    /* ----------------- ZK-IPD §三.2.5 修正因子叠加 ----------------- */
    /* 差异矩阵 2026-09-06 P0 项（反向验证）：
       Track 14 fb4a86d3 改对了 §三.2.1 主公式（实际回款×5%×S/A/B 系数），
       但漏叠加 §三.2.5 "可叠加 销售达成率阶梯系数 + 个人绩效系数"。
       §三.2.5 修正因子完整公式：
         finalPool = actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient
       tierCoefficient 缺省 = 1.0（中性），personalCoefficient 缺省 = 1.0（中性） */

    /** ZK-IPD §三.2.5：修正因子缺省值 = 1.0（中性，不放大不缩小） */
    public static final BigDecimal NEUTRAL_MODIFIER = BigDecimal.ONE;

    /**
     * ZK-IPD §三.2.1 + §三.2.5 完整公式：奖金池 = 实际回款 × 5% × S/A/B 系数 × 销售达成率阶梯 × 个人绩效。
     *
     * <p>与 {@link #calculateBonusPoolByZkFormula} 的关系：本方法是"完整公式" 入口，
     * 原方法是 §三.2.1 主公式（不带修正因子）的便捷重载；二者均属 ZK-IPD 合法路径。
     *
     * <ul>
     *   <li>tierCoefficient：销售达成率阶梯系数（0~1.2），由 {@link #tierCoefficientOf} 推出；缺省 = 1.0</li>
     *   <li>personalCoefficient：个人绩效系数，缺省 = 1.0（中性）</li>
     * </ul>
     *
     * @param actualReceipts     上市后连续 6 个月实际回款净额
     * @param levelCoefficient   项目 S/A/B 差异化系数
     * @param tierCoefficient    销售达成率阶梯系数（AC-INC-17h，0~1.2；null = 1.0）
     * @param personalCoefficient 个人绩效系数（null = 1.0）
     * @return 奖金池金额；回款 ≤ 0 返回 ZERO；任一非缺省入参空抛 ServiceException
     */
    public BigDecimal calculateBonusPoolByZkFormulaWithModifiers(BigDecimal actualReceipts,
                                                                  BigDecimal levelCoefficient,
                                                                  BigDecimal tierCoefficient,
                                                                  BigDecimal personalCoefficient) {
        if (actualReceipts == null) {
            throw new ServiceException("ZK-IPD §三.2.5：实际回款金额不能为空");
        }
        if (levelCoefficient == null) {
            throw new ServiceException("ZK-IPD §三.2.5：项目 S/A/B 差异化系数不能为空");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("ZK-IPD §三.2.5：实际回款金额不能为负");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tier = (tierCoefficient != null) ? tierCoefficient : NEUTRAL_MODIFIER;
        BigDecimal personal = (personalCoefficient != null) ? personalCoefficient : NEUTRAL_MODIFIER;
        return actualReceipts.multiply(readPoolRateZk())
            .multiply(levelCoefficient)
            .multiply(tier)
            .multiply(personal);
    }

    /**
     * ZK-IPD §三.2.1 + §三.2.5 + §三.2.3 联动：构造 BonusPool，自动从 achievementRate 推 tierCoefficient。
     *
     * <p>与 {@link #buildPoolFromProject} 的区别：本方法额外接收 achievementRate 与 personalCoefficient，
     * 按 §三.2.5 修正因子叠加规则计算 finalPool，并写入 tierCoefficient 字段（消除"绿但对应错误实现"）。
     *
     * @param projectId           项目 ID
     * @param actualReceipts      实际回款金额
     * @param achievementRate     销售达成率（%），由 {@link #tierCoefficientOf} 推 tierCoefficient；null = 1.0
     * @param personalCoefficient 个人绩效系数；null = 1.0
     * @param calculatedAt        计算时间
     * @param poolRate            奖金池比例（默认 5%）
     * @return 新建 BonusPool（未持久化），finalPool 已按 §三.2.5 完整公式计算
     */
    public BonusPool buildPoolFromProjectWithAchievement(Long projectId,
                                                          BigDecimal actualReceipts,
                                                          BigDecimal achievementRate,
                                                          BigDecimal personalCoefficient,
                                                          Date calculatedAt,
                                                          BigDecimal poolRate) {
        if (projectMapper == null) {
            throw new ServiceException("ProjectMapper 未注入，无法读取项目 S/A/B 差异化系数（ZK-IPD §三.2.1）");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        BigDecimal levelCoefficient = project.getLevelCoefficient();
        if (levelCoefficient == null) {
            throw new ServiceException("项目 S/A/B 差异化系数未配置（level=" + project.getLevel() + "），无法按 ZK-IPD §三.2.1 计算奖金池");
        }
        BigDecimal tierCoefficient = (achievementRate != null)
            ? tierCoefficientOf(achievementRate)
            : NEUTRAL_MODIFIER;
        BigDecimal rate = (poolRate != null) ? poolRate : readPoolRateZk();
        BigDecimal pool = calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);
        return BonusPool.builder()
            .projectId(projectId)
            .targetSales(actualReceipts)
            .poolRate(rate)
            .basePool(actualReceipts.multiply(rate))
            .coefficient(levelCoefficient)
            .achievementRate(achievementRate)
            .tierCoefficient(tierCoefficient)
            .finalPool(pool)
            .calculatedAt(calculatedAt)
            .status("DRAFT")
            .build();
    }

    /* ----------------- ZK-IPD §三.2.4 奖金分配比例算法 ----------------- */

    /** ZK-IPD §三.2.4：市场 PM 分配比例区间 [40%, 65%] */
    public static final BigDecimal MARKET_SHARE_MIN = new BigDecimal("0.40");
    public static final BigDecimal MARKET_SHARE_MAX = new BigDecimal("0.65");
    /** ZK-IPD §三.2.4：研发 PM 分配比例区间 [35%, 60%] */
    public static final BigDecimal RD_SHARE_MIN = new BigDecimal("0.35");
    public static final BigDecimal RD_SHARE_MAX = new BigDecimal("0.60");
    /** 比例精度容差：用于判定 market+rd 是否 = 1.0 */
    private static final BigDecimal SUM_TOLERANCE = new BigDecimal("0.0001");

    /**
     * ZK-IPD §三.2.4：校验市场 PM / 研发 PM 分配比例
     * <ul>
     *   <li>市场 PM 占比 ∈ [40%, 65%]</li>
     *   <li>研发 PM 占比 ∈ [35%, 60%]</li>
     *   <li>市场 + 研发 = 100%（容差 0.0001）</li>
     *   <li>上市 90 天复盘后由双 PM + 上级三方最终评定</li>
     * </ul>
     *
     * @param marketShare 市场 PM 分配比例（0.40–0.65）
     * @param rdShare     研发 PM 分配比例（0.35–0.60）
     * @return {marketShare, rdShare, sum} 不可变映射
     * @throws ServiceException 任一校验失败
     */
    public java.util.Map<String, BigDecimal> calculateDistribution(BigDecimal marketShare, BigDecimal rdShare) {
        if (marketShare == null) {
            throw new ServiceException("ZK-IPD §三.2.4：市场 PM 分配比例不能为空");
        }
        if (rdShare == null) {
            throw new ServiceException("ZK-IPD §三.2.4：研发 PM 分配比例不能为空");
        }
        if (marketShare.compareTo(MARKET_SHARE_MIN) < 0 || marketShare.compareTo(MARKET_SHARE_MAX) > 0) {
            throw new ServiceException("ZK-IPD §三.2.4：市场 PM 分配比例须在 40%-65% 之间，当前 "
                + marketShare.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        }
        if (rdShare.compareTo(RD_SHARE_MIN) < 0 || rdShare.compareTo(RD_SHARE_MAX) > 0) {
            throw new ServiceException("ZK-IPD §三.2.4：研发 PM 分配比例须在 35%-60% 之间，当前 "
                + rdShare.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        }
        BigDecimal sum = marketShare.add(rdShare);
        BigDecimal diff = sum.subtract(BigDecimal.ONE).abs();
        if (diff.compareTo(SUM_TOLERANCE) > 0) {
            throw new ServiceException("ZK-IPD §三.2.4：市场+研发分配比例总和须为 100%，当前 "
                + sum.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        }
        java.util.Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        result.put("marketShare", marketShare);
        result.put("rdShare", rdShare);
        result.put("sum", sum);
        return result;
    }

    /**
     * ZK-IPD §三.2.4：按市场/研发分配比例把奖金池拆分到两位 PM
     *
     * @param pool        总奖金池
     * @param marketShare 市场 PM 占比
     * @param rdShare     研发 PM 占比
     * @return {marketAmount, rdAmount} 不可变映射
     */
    public java.util.Map<String, BigDecimal> applyDistribution(BigDecimal pool, BigDecimal marketShare, BigDecimal rdShare) {
        if (pool == null) {
            throw new ServiceException("ZK-IPD §三.2.4：奖金池金额不能为空");
        }
        // 复用 calculateDistribution 区间 + 总和校验
        calculateDistribution(marketShare, rdShare);
        java.util.Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        result.put("marketAmount", pool.multiply(marketShare));
        result.put("rdAmount", pool.multiply(rdShare));
        return result;
    }

    /* --------------------------- P3-4.4 HTTP 端点收口 --------------------------- */
    /* 公式段（§三.2.1/§三.2.4/§三.2.5）已在前半段闭环，本段只补"持久化 + 状态机 + 审计"
       三个职责，零公式逻辑。状态机：DRAFT → CONFIRMED → DISTRIBUTED（终态）。 */

    /** P3-4.4：奖金池状态机 */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_DISTRIBUTED = "DISTRIBUTED";

    /** 审计事件 action 命名（与 AuditLogService.append 约定） */
    public static final String ACTION_COMPUTE = "BONUS_POOL_COMPUTE";
    public static final String ACTION_FREEZE = "BONUS_POOL_FREEZE";
    public static final String ACTION_DISTRIBUTE = "BONUS_POOL_DISTRIBUTE";

    private AuditLogService auditLogService;

    /**
     * P3-4.4：注入审计服务（Spring 装配入口）。
     * 测试构造器 {@link #BonusPoolService(BonusPoolMapper)} / {@link #BonusPoolService(BonusPoolMapper, ProjectMapper)}
     * 维持不变，新 auditLogService 默认 null——审计相关测试需用 setAuditLogService 注入 mock。
     */
    @Autowired(required = false)
    public void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * P3-4.4 §2.1：按项目 + 实际回款 + 销售达成率 + 个人绩效系数计算并落库。
     *
     * <p>业务规则：
     * <ul>
     *   <li>读 Project.levelCoefficient（G1 双签）→ levelCoefficient</li>
     *   <li>tierCoefficient 由 achievementRate 推（null → 中性 1.0）</li>
     *   <li>finalPool = actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient（§三.2.5 完整公式）</li>
     *   <li>入口先查 existing DRAFT → STATE_CONFLICT（HTTP 409）防 DuplicateKey 兜底 500（W4-B 修复点）</li>
     *   <li>status = DRAFT；写审计：append {@link #ACTION_COMPUTE}（与 freeze/distribute 同严）</li>
     * </ul>
     *
     * @param projectId           项目 ID
     * @param actualReceipts      实际回款金额（≥0）
     * @param achievementRate     销售达成率（%，null = 中性）
     * @param personalCoefficient 个人绩效系数（null = 1.0）
     * @param poolRate            奖金池比例（null = 0.05）
     * @param actor               当前操作人（审计落名；null 时静默跳过 appendAudit）
     * @return 新建 BonusPool（id 已生成，status=DRAFT）
     */


    /**
     * [SEC-FIX-HIGH-5.2] 个人绩效系数自动推导——查最新 kpi_records 综合得分，按 ZK 5 档分档。
     *
     * <p>[SEC-FIX-HIGH-5.2-FOLLOWUP] selectList 全表扫描修复：原实现 {@code findFirst().orElse(null)}
     * 在数据库无索引优化时退化为全表扫；现改为：
     * <ol>
     *   <li>{@code selectCount} 预检，{@code FINAL} 状态同一 {@code (projectId, period)} 行数若超
     *       {@link #RESOLVE_PERSONAL_COEFFICIENT_MAX_ROWS}（合理上限 = 一期一个 PM ≈ 12 行）则 fail-fast，
     *       防止下游分档被异常数据污染</li>
     *   <li>{@code selectList + orderByDesc(comprehensive_score) + LIMIT 1} 取最高分；LIMIT 1 保证 DB
     *       层裁剪（不拉全表），按综合得分而非 createTime 排序更贴合「max 分」语义</li>
     * </ol>
     *
     * @param projectId 项目 ID
     * @param period YYYY-MM
     * @return 个人绩效系数（0/0.3/0.6/0.8/1.0）；无记录回退 1.0（中性）
     */
    public BigDecimal resolvePersonalCoefficient(Long projectId, String period) {
        if (projectId == null || period == null || period.isBlank()) {
            return NEUTRAL_MODIFIER;
        }
        if (kpiRecordMapper == null) {
            return NEUTRAL_MODIFIER;
        }
        LambdaQueryWrapper<KpiRecord> countWrapper = new LambdaQueryWrapper<KpiRecord>()
            .eq(KpiRecord::getProjectId, projectId)
            .eq(KpiRecord::getPeriod, period)
            .eq(KpiRecord::getStatus, "FINAL")
            .eq(KpiRecord::getDelFlag, "0");
        Long finalCount = kpiRecordMapper.selectCount(countWrapper);
        if (finalCount != null && finalCount > RESOLVE_PERSONAL_COEFFICIENT_MAX_ROWS) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR,
                "[SEC-FIX-HIGH-5.2-FOLLOWUP] resolvePersonalCoefficient fail-fast: projectId="
                    + projectId + " period=" + period + " FINAL kpi_rows=" + finalCount
                    + " 超过合理上限 " + RESOLVE_PERSONAL_COEFFICIENT_MAX_ROWS
                    + "，疑似数据异常，禁止分档");
        }
        // 取最高综合分：ORDER BY comprehensive_score DESC LIMIT 1（DB 层裁剪，不拉全表）
        KpiRecord top = kpiRecordMapper.selectList(countWrapper
            .orderByDesc(KpiRecord::getComprehensiveScore)
            .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
        if (top == null || top.getComprehensiveScore() == null) {
            return NEUTRAL_MODIFIER;
        }
        return projectScoreService.projectPerformanceCoefficient(top.getComprehensiveScore());
    }

    @Transactional(rollbackFor = Exception.class)
    public BonusPool compute(Long projectId,
                             BigDecimal actualReceipts,
                             BigDecimal achievementRate,
                             BigDecimal personalCoefficient,
                             BigDecimal poolRate,
                             IpdActor actor) {
        if (projectId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "项目 ID 不能为空");
        }
        if (actualReceipts == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际回款金额不能为空");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) < 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际回款金额不能为负");
        }
        // [SEC-FIX-HIGH-5.2-FOLLOWUP] under-validated-sink-arg 件 1：显式上限
        // 防极端大数（注入/笔误）污染奖金池；超 1 万亿元视为参数异常拒绝计算。
        if (actualReceipts.compareTo(ACTUAL_RECEIPTS_MAX) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "实际回款金额超过合理上限 " + ACTUAL_RECEIPTS_MAX.toPlainString()
                    + "，当前=" + actualReceipts.toPlainString());
        }
        // [SEC-FIX-HIGH-5.2-FOLLOWUP] under-validated-sink-arg 件 2：compute 路径补 poolRate 校验
        // 旧实现 compute 路径完全跳过 validatePoolRate() 静态方法（仅 fillDerivedFields 内调用了一次旧方法），
        // 是 P3-4.2 的回归。此处显式复用，与 calculateDistribution 同严。
        validatePoolRate(poolRate);
        // W4-B 件 2：DuplicateKey → 409 业务异常（先查后写，落库兜底前拦截）
        BonusPool existing = bonusPoolMapper.selectByProjectIdAndStatus(projectId, STATUS_DRAFT);
        if (existing != null) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "项目 " + projectId + " 已存在 DRAFT 奖金池（id=" + existing.getId()
                    + "），请先 freeze/distribute 后再计算新版本");
        }
        BonusPool pool = buildPoolFromProjectWithAchievement(
            projectId, actualReceipts, achievementRate, personalCoefficient, new Date(), poolRate);
        // buildPoolFromProjectWithAchievement 已写 status="DRAFT"，此处冗余置位显式契约
        // ROOT-R3-P0-1：守卫 preCheck —— DRAFT->DRAFT 初始置位（无迁移）no-op
        pool.setStatus(STATUS_DRAFT);
        preCheckGuard("bonus_pool", null, STATUS_DRAFT, "compute");
        bonusPoolMapper.insert(pool);
        // W4-B 件 1：compute 与 freeze/distribute 同严落审计（v3 TS-08：审计失败不阻塞业务）
        // [SEC-FIX-HIGH-5.2-FOLLOWUP] under-validated-sink-arg 件 3：结构化字段 afterData JSON
        // 替代 reason 字符串拼接（审计反查可解析、可还原；reason 仅保留人类可读摘要）。
        BigDecimal effectivePoolRate = (poolRate == null) ? readPoolRateZk() : poolRate;
        java.util.Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("projectId", projectId);
        afterData.put("actualReceipts", actualReceipts);
        afterData.put("achievementRate", achievementRate);
        afterData.put("personalCoefficient", personalCoefficient);
        afterData.put("poolRate", effectivePoolRate);
        afterData.put("finalPool", pool.getFinalPool());
        afterData.put("status", STATUS_DRAFT);
        String afterDataJson;
        try {
            afterDataJson = JsonMapper.builder().build().writeValueAsString(afterData);
        } catch (JsonProcessingException ex) {
            afterDataJson = null; // JSON 失败不阻塞主流程
        }
        appendAudit(actor, ACTION_COMPUTE, pool.getId(),
            "compute projectId=" + projectId + " finalPool=" + pool.getFinalPool(),
            afterDataJson, null);
        return pool;
    }

    /**
     * P3-4.4 §2.2：冻结/确认奖金池（DRAFT → CONFIRMED）。
     *
     * <p>幂等：已是 CONFIRMED/DISTRIBUTED 直接返回当前实体，不写第二条审计。
     *
     * @param id     奖金池 ID
     * @param reason 冻结理由（可空）
     * @param actor  当前操作人（审计落名）
     * @return 更新后的 BonusPool
     */
    @Transactional(rollbackFor = Exception.class)
    public BonusPool freeze(Long id, String reason, IpdActor actor) {
        BonusPool pool = requireById(id);
        if (STATUS_CONFIRMED.equals(pool.getStatus()) || STATUS_DISTRIBUTED.equals(pool.getStatus())) {
            // 幂等：终态前不再流转
            return pool;
        }
        if (!STATUS_DRAFT.equals(pool.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "当前状态 " + pool.getStatus() + " 不可冻结（仅 DRAFT 可冻结）");
        }
        // ROOT-R3-P0-1：守卫 preCheck —— DRAFT -> CONFIRMED 合法
        preCheckGuard("bonus_pool", STATUS_DRAFT, STATUS_CONFIRMED, "freeze");
        pool.setStatus(STATUS_CONFIRMED);
        bonusPoolMapper.updateById(pool);
        // ROOT-R3-P0-1：postCommit 跨域副作用（事务提交后触发）
        registerPostCommit("bonus_pool", STATUS_DRAFT, STATUS_CONFIRMED, "freeze",
            actor != null ? actor.id() : null, pool.getId());
        appendAudit(actor, ACTION_FREEZE, pool.getId(),
            "DRAFT→CONFIRMED" + (reason != null ? " reason=" + reason : ""));
        return pool;
    }

    /**
     * P3-4.4 §2.3：分配奖金池（DRAFT/CONFIRMED → DISTRIBUTED）。
     *
     * <p>幂等：已是 DISTRIBUTED 直接返回当前实体，不重写审计。
     * 比例校验走 §三.2.4 calculateDistribution（区段 + 总和双重护栏）。
     *
     * @param id          奖金池 ID
     * @param marketShare 市场 PM 占比
     * @param rdShare     研发 PM 占比
     * @param actor       当前操作人（审计落名）
     * @return 更新后的 BonusPool
     */
    @Transactional(rollbackFor = Exception.class)
    public BonusPool distribute(Long id,
                                BigDecimal marketShare,
                                BigDecimal rdShare,
                                IpdActor actor) {
        BonusPool pool = requireById(id);
        if (STATUS_DISTRIBUTED.equals(pool.getStatus())) {
            // 幂等：DISTRIBUTED 终态
            return pool;
        }
        if (!STATUS_DRAFT.equals(pool.getStatus()) && !STATUS_CONFIRMED.equals(pool.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "当前状态 " + pool.getStatus() + " 不可分配（仅 DRAFT/CONFIRMED 可分配）");
        }
        // 区间 + 总和校验（ServiceException 抛到 Controller 由 advice 转 IpdBusinessException）
        calculateDistribution(marketShare, rdShare);
        // ROOT-R3-P0-1：守卫 preCheck —— CONFIRMED -> DISTRIBUTED 合法（跨域→写津贴账本）
        preCheckGuard("bonus_pool", STATUS_CONFIRMED, STATUS_DISTRIBUTED, "distribute");
        BigDecimal finalPool = pool.getFinalPool() == null ? BigDecimal.ZERO : pool.getFinalPool();
        BigDecimal marketAmount = finalPool.multiply(marketShare);
        BigDecimal rdAmount = finalPool.multiply(rdShare);
        // distributions JSON 记录拆分结果（供前端展示 + 后续审计可还原）
        Map<String, Object> distributionJson = new LinkedHashMap<>();
        distributionJson.put("marketShare", marketShare);
        distributionJson.put("rdShare", rdShare);
        distributionJson.put("marketAmount", marketAmount);
        distributionJson.put("rdAmount", rdAmount);
        String before = pool.getStatus();
        pool.setStatus(STATUS_DISTRIBUTED);
        pool.setDistributedAt(new Date());
        try {
            pool.setDistributions(JsonMapper.builder().build().writeValueAsString(distributionJson));
        } catch (JsonProcessingException ex) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "分配结果 JSON 序列化失败");
        }
        bonusPoolMapper.updateById(pool);
        // A3 接线（P-DATA-gap-1）：翻状态后批量写 bonus_allocations 台账（双 PM 行）
        writeBonusAllocations(pool, marketShare, rdShare, marketAmount, rdAmount);
        // ROOT-R3-P0-1：postCommit 跨域副作用（事务提交后触发）
        registerPostCommit("bonus_pool", before, STATUS_DISTRIBUTED, "distribute",
            actor != null ? actor.id() : null, pool.getId());
        appendAudit(actor, ACTION_DISTRIBUTE, pool.getId(),
            before + "→DISTRIBUTED market=" + marketShare + " rd=" + rdShare);
        return pool;
    }

    /**
     * P3-4.4 §2.4：查询奖金池详情（带软删过滤）。
     */
    public BonusPool getById(Long id) {
        return requireById(id);
    }

    /**
     * P3-4.4 §2.5：按项目查询奖金池列表。
     * 备注：listByProject 已存在上半段（P3-4.2），本卡沿用不破坏；
     * 过滤 del_flag=0 由 @TableLogic 自动处理。
     */

    /* ----- 私有工具 ----- */

    /**
     * A3 接线（P-DATA-gap-1，业务裁决提案-20260908）：distribute 翻状态后批量写
     * bonus_allocations 台账（AC-INC-35）。
     *
     * <p>口径：分配对象 = 双 PM 两条线（project_members.role ∈ {MARKET_PM, RD_PM}，
     * exit_date IS NULL）；contribution_rate = 贡献度五维加权分（contributions 最新一行
     * tierCoefficient）× 本方占比（marketShare/rdShare）；allocated_amount = finalPool × 本方占比；
     * performanceCoefficient 沿用奖金池项目差异化系数（coefficient）；status = DRAFT。
     * 贡献度未评定时台账照写、contribution_rate 置 null（不阻断已验收的分配主契约）。
     *
     * <p>mapper 为 null（旧单测）时静默跳过，对齐 auditLogService 可选注入模式。
     */
    private void writeBonusAllocations(BonusPool pool, BigDecimal marketShare, BigDecimal rdShare,
                                       BigDecimal marketAmount, BigDecimal rdAmount) {
        if (bonusAllocationMapper == null || projectMemberMapper == null) {
            return;
        }
        List<ProjectMember> pms = projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, pool.getProjectId())
            .in(ProjectMember::getRole, List.of(Contribution.ROLE_MARKET, Contribution.ROLE_RD))
            .isNull(ProjectMember::getExitDate));
        if (pms == null || pms.isEmpty()) {
            return;
        }
        Contribution contribution = null;
        if (contributionMapper != null) {
            contribution = contributionMapper.selectOne(new LambdaQueryWrapper<Contribution>()
                .eq(Contribution::getProjectId, pool.getProjectId())
                .eq(Contribution::getDelFlag, "0")
                .orderByDesc(Contribution::getId)
                .last("limit 1"));
        }
        for (ProjectMember pm : pms) {
            boolean isMarket = Contribution.ROLE_MARKET.equals(pm.getRole());
            BigDecimal share = isMarket ? marketShare : rdShare;
            BigDecimal amount = isMarket ? marketAmount : rdAmount;
            BigDecimal contributionRate = null;
            if (contribution != null && contribution.getTierCoefficient() != null) {
                contributionRate = contribution.getTierCoefficient().multiply(share);
            }
            BonusAllocation row = BonusAllocation.builder()
                .bonusPoolId(pool.getId())
                .personId(pm.getPersonId())
                .roleInProject(pm.getRole())
                .contributionRate(contributionRate)
                .performanceCoefficient(pool.getCoefficient())
                .allocatedAmount(amount)
                .status("DRAFT")
                .build();
            bonusAllocationMapper.insert(row);
        }
    }

    /**
     * 加载并校验奖金池：不存在或软删 → NOT_FOUND。
     */
    private BonusPool requireById(Long id) {
        if (id == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "奖金池 ID 不能为空");
        }
        BonusPool pool = bonusPoolMapper.selectById(id);
        if (pool == null || "1".equals(pool.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "奖金池不存在: " + id);
        }
        return pool;
    }

    /**
     * 落审计（freeze/distribute 写动作）。
     * auditLogService=null 时（测试场景）静默跳过，不抛错——保证 Service 单测不依赖 audit 装配。
     *
     * <p>[SEC-FIX-HIGH-5.2-FOLLOWUP] 增加 {@code afterData} / {@code beforeData} 结构化字段：
     * 把 reason 字符串拼接替换为可解析 JSON，审计反查可按字段过滤和还原实体快照。
     * 旧单参调用方继续兼容（重载 {@link #appendAudit(IpdActor, String, Long, String)}）。
     */
    private void appendAudit(IpdActor actor, String action, Long entityId,
                             String reason, String afterData, String beforeData) {
        if (auditLogService == null || actor == null) {
            return;
        }
        AuditLog draft = AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action(action)
            .entityType("bonus_pools")
            .entityId(entityId)
            .reason(reason)
            .afterData(afterData)
            .beforeData(beforeData)
            .createTime(new Date())
            .build();
        try {
            auditLogService.append(draft);
        } catch (RuntimeException ex) {
            // 审计失败不阻塞业务（v3 TS-08 注释：业务失败不回滚审计；对称地审计失败不回滚业务）
        }
    }

    /**
     * 兼容重载（freeze / distribute 旧调用方）：reason 字符串原样写入 reason 字段，
     * 结构化字段为空。
     */
    private void appendAudit(IpdActor actor, String action, Long entityId, String reason) {
        appendAudit(actor, action, entityId, reason, null, null);
    }

    /**
     * [legacy] 兼容旧 try/catch 块的占位符——原 4 参 appendAudit 已被新签名取代，
     * 旧的 try/catch 已并入新方法末尾，本占位无逻辑。
     */
    private void _legacy_audit_append_marker(AuditLog draft) {
        // 占位以维持源码可见；真实 try/catch 见新 appendAudit 实现。
    }

    /* ---------------------- ROOT-R3-P0-1 跨状态机守卫辅助 ---------------------- */

    /**
     * ROOT-R3-P0-1 修复：守卫 preCheck 包装（fail-closed 模式）。
     *
     * <p>守卫 null = fail-closed 抛 IpdBusinessException（防 state-machine-bypass，与 KpiRecordService 8bdc7811 同型）。
     * 测试兼容：BonusPoolServiceTest 通过 setStateMachineGuard(...) 注入 mock；
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
     * 注册 postCommit 副作用（事务提交后触发，避免回滚后污染）
     */
    private void registerPostCommit(String entityType, String fromState, String toState,
                                    String trigger, Long operatorId, Long entityId) {
        if (stateMachineGuard == null) {
            return;
        }
        java.util.Date occurredAt = new java.util.Date();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
                    }
                });
        } else {
            stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
        }
    }
}
