package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.ProjectScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * P3-2.1 项目绩效评定 20/40/40 独立提交服务
 *
 * <p>AC：AC-KPI-16/16b/16c/18/19/22；BR：BR-KPI-08。
 * <p>核心规则：
 * <ul>
 *   <li>权重：自评 20% + 市场组长 40% + 研发组长 40%，三者之和 = 1.0</li>
 *   <li>两 PM 各自独立：市场 PM 得分 ≠ 研发 PM 得分（独立打分）</li>
 *   <li>无「评审上级」角色（A5 决策），评定人 = 双 PM 自评 + 各自产品组长</li>
 *   <li>L1–L5 能力等级 vs 项目绩效得分：两者不互相推导（等级来自 API，绩效来自评定）</li>
 * </ul>
 *
 * <p>P3-2.1 单卡做字段校验 + 加权汇总算法；DB 持久化在 P3-2.2（归档+规则版本）实现。
 */
@Slf4j
@Service
public class ProjectScoreService {

    /** AC-KPI-16b 默认权重：自评/市场组长/研发组长 = 20%/40%/40% */
    public static final BigDecimal WEIGHT_SELF = new BigDecimal("0.20");
    public static final BigDecimal WEIGHT_MARKET_LEADER = new BigDecimal("0.40");
    public static final BigDecimal WEIGHT_RD_LEADER = new BigDecimal("0.40");

    /** 评分合法角色 */
    public static final List<String> PM_ROLES = Arrays.asList("MARKET_PM", "RD_PM");

    /**
     * 校验权重和：三项权重和必须 = 1.0（±0.01 容差防浮点累计）
     *
     * @param weights 三项权重，顺序 [自评, 市场组长, 研发组长]
     */
    public static void validateWeights(List<BigDecimal> weights) {
        if (weights == null || weights.size() != 3) {
            throw new IpdBusinessException("绩效权重必须为三项 [自评, 市场组长, 研发组长]");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(BigDecimal.ONE) > 1) {
                throw new IpdBusinessException("单项权重必须在 [0, 1] 区间");
            }
            sum = sum.add(w);
        }
        BigDecimal diff = sum.subtract(BigDecimal.ONE).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IpdBusinessException("三项权重之和必须 = 1.0（当前=" + sum + "）");
        }
    }

    /**
     * 校验评分合法性：三项分数 0~100；角色 MARKET_PM / RD_PM。
     */
    public static void validateScores(String pmRole, BigDecimal self, BigDecimal ml, BigDecimal rl) {
        if (!PM_ROLES.contains(pmRole)) {
            throw new IpdBusinessException("pmRole 必须为 MARKET_PM 或 RD_PM");
        }
        validateScoreRange("自评", self);
        validateScoreRange("市场组长评", ml);
        validateScoreRange("研发组长评", rl);
    }

    private static void validateScoreRange(String label, BigDecimal score) {
        if (score == null) {
            throw new IpdBusinessException(label + "分数不能为空");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IpdBusinessException(label + "分数必须在 [0, 100] 区间");
        }
    }

    /**
     * AC-KPI-16 加权汇总：self × 0.2 + ml × 0.4 + rl × 0.4
     */
    public static BigDecimal weighted(BigDecimal self, BigDecimal ml, BigDecimal rl) {
        return self.multiply(WEIGHT_SELF)
            .add(ml.multiply(WEIGHT_MARKET_LEADER))
            .add(rl.multiply(WEIGHT_RD_LEADER))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * AC-KPI-16c 两 PM 独立评分占位：返回草稿记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectScore draft(ProjectScore draft) {
        validateScores(draft.getPmRole(), draft.getSelfScore(),
            draft.getMarketLeaderScore(), draft.getRdLeaderScore());
        BigDecimal w = weighted(draft.getSelfScore(), draft.getMarketLeaderScore(),
            draft.getRdLeaderScore());
        draft.setWeightedScore(w);
        log.debug("项目绩效草稿 projectId={} personId={} pmRole={} weighted={}",
            draft.getProjectId(), draft.getPersonId(), draft.getPmRole(), w);
        return draft;
    }

    /**
     * AC-KPI-22 校验：绩效得分与 L1–L5 能力等级不互相推导
     * <p>本服务不接受 abilityLevel 入参；反之绩效分数不决定津贴额度。
     */
    public static void assertNoAbilityInference(BigDecimal weightedScore, String abilityLevel) {
        // 占位断言：绩效计算过程中不得触碰 abilityLevel
        if (abilityLevel != null) {
            log.debug("绩效加权={} 与能力等级={} 独立，不互相推导", weightedScore, abilityLevel);
        }
    }
}
