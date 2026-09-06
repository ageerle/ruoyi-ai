package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 奖金池服务（P3-4.2/4.3；AC-INC-16/17/18/19/20/21；BR-INC-04/05/06；ZK-IPD-2026-09-06-补）
 *
 * <p>核心规则（AC-INC-17/17b~17h + AC-INC-18~21 + ZK-IPD Prompt §三.2.1）：
 * <ul>
 *   <li>AC-INC-16：奖金池基数 = 目标销售额 × 5%（bonus.poolRate），不是实际/回款</li>
 *   <li>AC-INC-17~21：达成率阶梯系数，严格按 {@code 达成率 ≥ 阈值} 从高到低匹配；
 *       区间下端点含、上端点不含；不允许浮点等值判定（不写浮点字面量等值 / 不引入容差参数）</li>
 *   <li>AC-INC-17h：默认六档 [{Infinity,1.2},{120,1.0},{100,1.0},{85,0.8},{70,0.6},{50,0.3},{0,0.0}]</li>
 *   <li>AC-INC-20：达成率 60% 命中 0.3 档，触发复盘检讨提醒（reviewRequired=true）</li>
 *   <li>AC-INC-21：达成率 45% 命中 0 档，不发放；已发月度津贴不追回（独立规则）</li>
 *   <li><b>ZK-IPD §三.2.1</b>：奖金池 = 上市后连续 6 个月<b>实际回款</b>金额 × 5% × <b>项目 S/A/B 差异化系数</b>（coefficient，非 tierCoefficient）</li>
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
     * Spring 装配入口（双 Mapper 注入）。
     */
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
     * AC-INC-16：奖金池基数 = 目标销售额 × bonus.poolRate（默认 0.05）
     */
    public BigDecimal calculateBasePool(BigDecimal targetSales, BigDecimal poolRate) {
        if (targetSales == null || targetSales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = (poolRate != null) ? poolRate : new BigDecimal("0.05");
        return targetSales.multiply(rate);
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
     * 一次性计算并填充 BonusPool 实体的派生字段
     */
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
}
