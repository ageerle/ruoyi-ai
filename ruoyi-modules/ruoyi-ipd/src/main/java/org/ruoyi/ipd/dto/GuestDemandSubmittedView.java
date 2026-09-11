package org.ruoyi.ipd.dto;

/**
 * P4-1.1 游客提交成功视图：仅返回 8 位查询码与初始状态，不暴露内部 ID / 人员信息（BR-REQ-03）。
 */
public record GuestDemandSubmittedView(String code, String status) {
}
