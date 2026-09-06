package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.KpiRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * P3-1.1 KPI 录入与综合得分计算服务
 *
 * <p>AC：AC-KPI-01/02/03/04/15；BR：BR-KPI-01/02/03/06。
 * <p>核心规则：
 * <ul>
 *   <li>功能 60% + 共担 40% 默认权重（w = 0.6）</li>
 *   <li>共担权重 (1 − w) 不得低于 30%（低于时拒绝保存）</li>
 *   <li>市场 PM / 研发 PM 各四项功能 KPI，每项 15%，四项合计 60%</li>
 *   <li>偏差天数 = |实际上市 − 计划窗口边界|</li>
 * </ul>
 *
 * <p>P3-1.1 单卡暂不写审计（避免与兄弟 dirty 的 AuditLogService 编译失败冲突）；写入由 P3-1.2 共担 KPI 归集卡完成。
 * <p>本卡覆盖 AC-KPI-01/02/03/04/15 字段校验与算法计算，调用方按需扩展落审计。
 */
@Slf4j
@Service
public class KpiRecordService {

    public KpiRecordService() {
    }

    /**
     * AC-KPI-01：保存 KPI 权重配置，共担权重不得低于 30%。
     *
     * @param functionalWeight 功能 KPI 权重（0~1）
     * @return 校验通过后返回 effective functionalWeight（默认 0.6）
     * @throws IpdBusinessException 共担权重 < 30% 时抛出
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
     *
     * @param pmRole  "MARKET_PM" 或 "RD_PM"
     * @param weights 四项功能 KPI 权重（顺序见 {@link KpiScoreCalculator} 常量）
     * @throws IpdBusinessException 校验失败
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
     * AC-KPI-04：综合得分计算（功能 × w + 共担 × (1 − w)）。
     *
     * @param functional 功能 KPI 得分（0~100）
     * @param shared     共担 KPI 得分（0~100）
     * @param w          功能权重（默认 0.6）
     */
    public BigDecimal computeComprehensive(BigDecimal functional, BigDecimal shared, BigDecimal w) {
        try {
            return KpiScoreCalculator.comprehensive(functional, shared, w);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * AC-KPI-15：偏差天数计算（实际上市日期 vs 计划窗口）。
     */
    public int computeDeviationDays(long actualLaunchDate, long plannedWindowStart, long plannedWindowEnd) {
        try {
            return KpiScoreCalculator.deviationDays(actualLaunchDate, plannedWindowStart, plannedWindowEnd);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * AC-KPI-15：市场窗口命中率得分（基于偏差天数）。
     */
    public BigDecimal computeWindowHitRate(int deviationDays) {
        try {
            return KpiScoreCalculator.windowHitRate(deviationDays);
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(e.getMessage());
        }
    }

    /**
     * 录入功能 KPI 草稿（P3-1.1 单卡仅做占位；写入由 P3-1.2 完成）。
     */
    public KpiRecord draftFunctionalEntry(KpiRecord draft) {
        return draft;
    }
}
