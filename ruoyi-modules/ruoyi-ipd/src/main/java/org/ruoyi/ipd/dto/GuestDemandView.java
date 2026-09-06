package org.ruoyi.ipd.dto;

import java.util.Date;

/**
 * P4-1.2 游客需求公开视图（页39；AC-REQ-04 查询码隔离）。
 * <p>仅返回 queryCode + 状态 + 受理时间 + 标题摘要；不暴露内部 ID / 人员 / 备注。
 */
public record GuestDemandView(
    String queryCode,
    String status,
    Date acceptedAt,
    Date routedAt,
    String titleSummary
) {
}
