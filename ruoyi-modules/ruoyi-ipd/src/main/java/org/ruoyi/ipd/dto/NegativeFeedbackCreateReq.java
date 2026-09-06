package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P3-8.2 负反馈录入请求（BR-INC-10）
 *
 * <p>前端页 P0-10.36 录入表单：
 * <ul>
 *   <li>选择触发情形（4 选 1）</li>
 *   <li>填写触发证据</li>
 *   <li>选择生效月份 YYYY-MM</li>
 * </ul>
 *
 * <p>主责方 / 连带方映射 / 执行动作由 service 层根据 triggerType 自动推导（服务端权威）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegativeFeedbackCreateReq(
    Long projectId,
    String triggerType,
    String triggerEvidence,
    String triggerMonth,
    String recoveryMonth
) { }