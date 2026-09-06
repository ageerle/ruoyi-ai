package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P3-8.2 负反馈认定/解除请求
 *
 * <p>{@code decision} = APPROVE 走 PENDING_DECISION → EXECUTED；
 * <p>{@code decision} = REJECT 走 PENDING_DECISION → REJECTED；
 * <p>{@code decision} = LIFT 走 EXECUTED → LIFTED（恢复 bonusEligible）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegativeFeedbackDecisionReq(
    String decision,
    String comment
) { }