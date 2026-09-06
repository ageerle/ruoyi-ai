package org.ruoyi.ipd.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * P3-1.1 KPI 得分纯函数计算器（不依赖 mapper / DB，便于单测）
 *
 * <p>核心算法：
 * <ul>
 *   <li>综合得分 = functional × w + shared × (1 − w)，w 默认 0.6（功能 60%）</li>
 *   <li>共担权重 (1 − w) 不得低于 0.3，低于时拒绝保存</li>
 *   <li>市场 PM 四项功能 KPI：需求准确率 / 窗口命中率 / 场景竞争力 / 竞品情报，各 15%</li>
 *   <li>研发 PM 四项功能 KPI：上市准时率 / 质量缺陷率 / 技术创新度 / 一次性实现率，各 15%</li>
 *   <li>四项 KPI 总权重 = 60%（与 w 对应），各项 15%</li>
 * </ul>
 *
 * <p>AC：AC-KPI-01/02/03/04/15；BR：BR-KPI-01/02/03/06。
 * <p>不读写数据库；服务层 KpiRecordService 调用本类完成字段校验与算法计算。
 */
public final class KpiScoreCalculator {

    /** 功能 KPI 总权重（默认 60%） */
    public static final BigDecimal DEFAULT_FUNCTIONAL_WEIGHT = new BigDecimal("0.60");

    /** 共担 KPI 权重下限（30%），低于时拒绝保存 */
    public static final BigDecimal MIN_SHARED_WEIGHT = new BigDecimal("0.30");

    /** 市场 PM 四项功能 KPI 名称（用于录入校验） */
    public static final List<String> MARKET_PM_FUNCTIONAL_FIELDS = List.of(
        "requirementAccuracy",   // 需求准确率
        "windowHitRate",         // 窗口命中率
        "scenarioCompetitiveness",// 场景竞争力
        "competitorIntelligence" // 竞品情报
    );

    /** 研发 PM 四项功能 KPI 名称 */
    public static final List<String> RD_PM_FUNCTIONAL_FIELDS = List.of(
        "launchOnTimeRate",      // 上市准时率
        "qualityDefectRate",     // 质量缺陷率
        "techInnovation",        // 技术创新度
        "firstPassYield"         // 一次性实现率
    );

    /** 单项功能 KPI 默认权重 15% */
    public static final BigDecimal SINGLE_FIELD_WEIGHT = new BigDecimal("0.15");

    /** 窗口命中率偏差容忍（天）：偏差 ≤ 30 天线性衰减 */
    public static final int WINDOW_HIT_TOLERANCE_DAYS = 30;

    /** 每超过 1 天扣 1 分（窗口命中率 0~100） */
    public static final BigDecimal WINDOW_HIT_PENALTY_PER_DAY = BigDecimal.ONE;

    private KpiScoreCalculator() {}

    /**
     * 计算综合得分。
     *
     * @param functional 功能 KPI 得分（0~100）
     * @param shared     共担 KPI 得分（0~100）
     * @param w          功能权重（0~1），默认 0.6
     * @return 综合得分 = functional × w + shared × (1 − w)，保留 2 位小数，HALF_UP
     */
    public static BigDecimal comprehensive(BigDecimal functional, BigDecimal shared, BigDecimal w) {
        if (functional == null || shared == null) {
            throw new IllegalArgumentException("功能 / 共担得分不能为空");
        }
        BigDecimal weight = w == null ? DEFAULT_FUNCTIONAL_WEIGHT : w;
        validateFunctionalWeight(weight);
        return functional.multiply(weight)
            .add(shared.multiply(BigDecimal.ONE.subtract(weight)))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 校验功能权重 w：共担权重 (1 − w) 不得低于 30%。
     *
     * @param w 功能权重
     * @throws IllegalArgumentException 校验失败
     */
    public static void validateFunctionalWeight(BigDecimal w) {
        if (w == null) {
            throw new IllegalArgumentException("功能 KPI 权重不能为空");
        }
        if (w.compareTo(BigDecimal.ZERO) <= 0 || w.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("功能 KPI 权重必须在 (0, 1] 区间");
        }
        BigDecimal sharedWeight = BigDecimal.ONE.subtract(w);
        if (sharedWeight.compareTo(MIN_SHARED_WEIGHT) < 0) {
            throw new IllegalArgumentException(
                "共担 KPI 权重不得低于 30%（当前共担=" + sharedWeight + "）");
        }
    }

    /**
     * 市场 PM 功能 KPI 录入校验：四项 + 总权重 = 60%。
     *
     * @param weights 四项权重（小数），顺序对应 MARKET_PM_FUNCTIONAL_FIELDS
     */
    public static void validateMarketPmFunctionalWeights(List<BigDecimal> weights) {
        validateFourFieldWeights(weights, MARKET_PM_FUNCTIONAL_FIELDS);
    }

    /**
     * 研发 PM 功能 KPI 录入校验：四项 + 总权重 = 60%。
     */
    public static void validateRdPmFunctionalWeights(List<BigDecimal> weights) {
        validateFourFieldWeights(weights, RD_PM_FUNCTIONAL_FIELDS);
    }

    private static void validateFourFieldWeights(List<BigDecimal> weights, List<String> fields) {
        if (weights == null || weights.size() != 4) {
            throw new IllegalArgumentException(
                fields.get(0).substring(0, 1).toUpperCase() + " PM 功能 KPI 必须录入四项（" + fields + "）");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("单项权重必须在 [0, 1] 区间");
            }
            sum = sum.add(w);
        }
        // 四项总和应等于功能 KPI 总权重（默认 60%）；允许 ±0.01 容差避免浮点累计误差
        BigDecimal diff = sum.subtract(DEFAULT_FUNCTIONAL_WEIGHT).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException(
                "四项功能 KPI 总权重必须等于功能 KPI 总权重（" + DEFAULT_FUNCTIONAL_WEIGHT + "），当前=" + sum);
        }
    }

    /**
     * AC-KPI-15：偏差天数计算（实际上市日期 vs 计划窗口）。
     *
     * @param actualLaunchDate   实际上市日期（毫秒）
     * @param plannedWindowStart 计划窗口起始（毫秒）
     * @param plannedWindowEnd   计划窗口截止（毫秒）
     * @return 偏差天数（绝对值）：落在窗口内为 0；早于窗口为窗口起点 − 实际；晚于窗口为实际 − 窗口终点
     */
    public static int deviationDays(long actualLaunchDate, long plannedWindowStart, long plannedWindowEnd) {
        if (plannedWindowStart > plannedWindowEnd) {
            throw new IllegalArgumentException("计划窗口起始不能晚于截止");
        }
        if (actualLaunchDate < plannedWindowStart) {
            return (int) ((plannedWindowStart - actualLaunchDate) / 86400_000L);
        }
        if (actualLaunchDate > plannedWindowEnd) {
            return (int) ((actualLaunchDate - plannedWindowEnd) / 86400_000L);
        }
        return 0;
    }

    /**
     * AC-KPI-15：市场窗口命中率得分。
     * 偏差 ≤ 0 天 → 100 分；偏差每超过 1 天扣 1 分；偏差 ≥ 30 天 → 70 分；偏差 > 30 天 → 0 分。
     * <p>规则：偏差 ≤ 30 天线性衰减，>30 天一律 0。
     *
     * @param deviationDays 偏差天数（绝对值，≥ 0）
     * @return 命中率 0~100
     */
    public static BigDecimal windowHitRate(int deviationDays) {
        if (deviationDays < 0) {
            throw new IllegalArgumentException("偏差天数不能为负");
        }
        if (deviationDays == 0) {
            return new BigDecimal("100");
        }
        if (deviationDays > WINDOW_HIT_TOLERANCE_DAYS) {
            return BigDecimal.ZERO;
        }
        // 偏差 d 在 (0, 30]：100 − d × 1
        return BigDecimal.valueOf(100L - deviationDays);
    }
}
