package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 检测并落库请求（P3-8.1；commit 模式）。
 *
 * <p>commit 模式下，调用 NegativeFeedbackService.create 落库（triggerType + attribution + month）。
 * <p>month 必填，格式 YYYY-MM。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DetectionCommitReq(
    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    String month,
    BigDecimal tierDeltaOverride,
    Integer bonusDisqualifyOverride
) {
}