package org.ruoyi.ipd.dto;

import org.ruoyi.ipd.domain.Project;

import java.util.Date;

/**
 * 项目列表项视图（P1-9.2 增量）——在 Project 基础上加 scenarioDaysRemaining / lastActivityAt 派生字段。
 *
 * <p>业务规则：
 * <ul>
 *   <li>scenarioDaysRemaining = 14 - (today - lastActivityAt) days；
 *       仅当 source=LEGACY 且 catchupStatus=IN_PROGRESS 时计算；其它情况 = null</li>
 *   <li>criticalThreshold = 3：当 scenarioDaysRemaining ≤ 3 时，前端横幅告警 + 后端通知 MARKET_PM + PRODUCT_LEADER</li>
 *   <li>lastActivityAt 计算 = max of stage_action.update_time / kpi.update_time / gate_review.update_time
 *       （deliverable 当前未入 DDL，跳过；保持 14 维数据流可观测）</li>
 * </ul>
 */
public record ProjectListItemView(
    Project project,
    Date lastActivityAt,
    Integer scenarioDaysRemaining,
    Boolean critical) {
}