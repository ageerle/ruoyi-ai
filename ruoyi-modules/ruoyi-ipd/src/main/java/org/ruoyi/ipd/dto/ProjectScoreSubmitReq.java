package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 项目绩效单组件提交请求；更正也使用同一入口并生成新版本（P3-2.2）。 */
public record ProjectScoreSubmitReq(
    @NotNull Long projectId,
    @NotNull Long personId,
    @NotBlank String componentType,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal score,
    @Size(max = 500) String reason
) {
}
