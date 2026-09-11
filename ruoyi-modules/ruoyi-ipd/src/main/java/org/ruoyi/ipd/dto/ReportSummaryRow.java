package org.ruoyi.ipd.dto;

import java.math.BigDecimal;

/**
 * P4-4.1 项目绩效汇总行（AC-INC-34 项目级口径）
 *
 * <p>单项目一行，金额单位为「元（人民币）」保留 2 位。
 * <p>字段语义：
 * <ul>
 *   <li>{@code allowanceFinalAmount}：该项目下全体人员当月津贴 finalAmount 合计（多项目叠加后）</li>
 *   <li>{@code bonusFinalPool}：该项目已落库奖金池 finalPool 合计（按 BonusPool.status 取 DRAFT/CONFIRMED/DISTRIBUTED）</li>
 *   <li>{@code avgWeightedScore}：该项目下项目绩效评定 weightedScore 平均值（按 personId × pmRole 去重）</li>
 * </ul>
 *
 * <p>不重复计数口径：
 * <ul>
 *   <li>allowance_ledgers 自然键 = (personId, projectId, month) —— SELECT SUM(finalAmount) GROUP BY projectId WHERE month=?</li>
 *   <li>bonus_pools 自然键 = projectId —— SELECT SUM(finalPool) GROUP BY projectId WHERE status IN (...)</li>
 *   <li>project_scores 自然键 = (projectId, personId, pmRole) —— SELECT AVG(weightedScore) GROUP BY projectId WHERE status='CONFIRMED'</li>
 * </ul>
 */
public record ReportSummaryRow(
    Long projectId,
    String projectCode,
    String projectName,
    String month,
    BigDecimal allowanceFinalAmount,
    BigDecimal bonusFinalPool,
    BigDecimal avgWeightedScore,
    Integer allowanceRowCount,
    Integer bonusRowCount,
    Integer scoreRowCount
) {
}
