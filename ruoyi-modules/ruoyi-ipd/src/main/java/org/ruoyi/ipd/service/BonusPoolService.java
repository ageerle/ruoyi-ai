package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
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
 * Controller {@link #compute(Long, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal)}
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

    /**
     * 兼容构造器：仅注入 BonusPoolMapper 的旧测试入口。
     */
    public BonusPoolService(BonusPoolMapper bonusPoolMapper) {
        this(bonusPoolMapper, null);
    }

    /**
     * Spring 装配入口（双 Mapper 注入）；多构造器必须显式指定，否则上下文无法实例化。
     */
    @Autowired
    public BonusPoolService(BonusPoolMapper bonusPoolMapper, ProjectMapper projectMapper) {
        this.bonusPoolMapper = bonusPoolMapper;
        this.projectMapper = projectMapper;
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

    /** P3-4.2 BR-INC-04：默认 poolRate = 5% */
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
     * <p>当前实现：硬编码默认 0.0500（SystemConfigService 接入由 P0-3.3 完成后接管）。
     */
    public BigDecimal readActivePoolRate() {
        return DEFAULT_CONFIG_POOL_RATE;
    }

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
        return actualReceipts.multiply(DEFAULT_POOL_RATE).multiply(levelCoefficient);
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
        BigDecimal rate = (poolRate != null) ? poolRate : DEFAULT_POOL_RATE;
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
        return actualReceipts.multiply(DEFAULT_POOL_RATE)
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
        BigDecimal rate = (poolRate != null) ? poolRate : DEFAULT_POOL_RATE;
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
     *   <li>status = DRAFT；不写审计（compute 是纯计算入口，审计由 freeze/distribute 触发）</li>
     * </ul>
     *
     * @param projectId           项目 ID
     * @param actualReceipts      实际回款金额（≥0）
     * @param achievementRate     销售达成率（%，null = 中性）
     * @param personalCoefficient 个人绩效系数（null = 1.0）
     * @param poolRate            奖金池比例（null = 0.05）
     * @return 新建 BonusPool（id 已生成，status=DRAFT）
     */
    @Transactional(rollbackFor = Exception.class)
    public BonusPool compute(Long projectId,
                             BigDecimal actualReceipts,
                             BigDecimal achievementRate,
                             BigDecimal personalCoefficient,
                             BigDecimal poolRate) {
        if (projectId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "项目 ID 不能为空");
        }
        if (actualReceipts == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际回款金额不能为空");
        }
        if (actualReceipts.compareTo(BigDecimal.ZERO) < 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "实际回款金额不能为负");
        }
        BonusPool pool = buildPoolFromProjectWithAchievement(
            projectId, actualReceipts, achievementRate, personalCoefficient, new Date(), poolRate);
        // buildPoolFromProjectWithAchievement 已写 status="DRAFT"，此处冗余置位显式契约
        pool.setStatus(STATUS_DRAFT);
        bonusPoolMapper.insert(pool);
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
        pool.setStatus(STATUS_CONFIRMED);
        bonusPoolMapper.updateById(pool);
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
     */
    private void appendAudit(IpdActor actor, String action, Long entityId, String reason) {
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
            .createTime(new Date())
            .build();
        try {
            auditLogService.append(draft);
        } catch (RuntimeException ex) {
            // 审计失败不阻塞业务（v3 TS-08 注释：业务失败不回滚审计；对称地审计失败不回滚业务）
        }
    }
}
