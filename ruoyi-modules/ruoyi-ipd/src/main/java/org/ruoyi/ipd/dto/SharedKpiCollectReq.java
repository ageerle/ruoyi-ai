package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** K01-K04 共担 KPI 月度归集请求（P3-1.2）。 */
public record SharedKpiCollectReq(
    @NotNull Long projectId,
    @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "period 必须为 YYYY-MM")
    String period,
    @NotBlank @DecimalMin(value = "0.00", message = "实际销售额不能为负")
    String actualSales,
    @NotBlank @DecimalMin(value = "0.01", message = "目标销售额必须大于 0")
    String targetSales,
    @NotBlank @DecimalMin(value = "0.00", message = "实际渠道数不能为负")
    String actualChannels,
    @NotNull @Positive(message = "目标渠道数必须大于 0") Integer targetChannels,
    @NotNull @PositiveOrZero Integer promoters,
    @NotNull @PositiveOrZero Integer detractors,
    @NotNull @PositiveOrZero Integer npsSampleSize,
    @NotNull @PositiveOrZero Integer landedScenarios,
    @NotNull @Positive(message = "规划场景数必须大于 0") Integer plannedScenarios
) {
}
