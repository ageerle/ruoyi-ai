package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
 *
 * <p>SEC-REV-round3 Bug#7（中危 under-validated-input）：
 * 原 DTO 缺 Bean Validation，仅在 service 层做软校验（triggerType/月份格式），
 * 但 projectId 必填、triggerType 长度上限、triggerEvidence 长度上限等均无声明。
 * 补 @NotNull/@NotBlank/@Pattern/@Size 后由 Spring @Valid 自动触发校验，非法请求 400 拒绝。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegativeFeedbackCreateReq(
    @NotNull(message = "projectId 不能为空")
    Long projectId,

    @NotBlank(message = "triggerType 不能为空")
    @Pattern(regexp = "REWORK_EXCEEDED|QUALITY_ACCIDENT|SPEC_PILE_COPY|MISSED_MARKET_WINDOW",
        message = "triggerType 必须为 REWORK_EXCEEDED / QUALITY_ACCIDENT / SPEC_PILE_COPY / MISSED_MARKET_WINDOW 之一")
    @Size(max = 32, message = "triggerType 长度不能超过 32")
    String triggerType,

    @Size(max = 1000, message = "triggerEvidence 长度不能超过 1000")
    String triggerEvidence,

    @NotBlank(message = "triggerMonth 不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "triggerMonth 必须为 YYYY-MM 格式")
    String triggerMonth,

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "recoveryMonth 必须为 YYYY-MM 格式")
    String recoveryMonth
) { }