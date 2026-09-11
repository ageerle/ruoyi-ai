package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * [SEC-FIX-HIGH-5.2] 自动推导 personalCoefficient 的奖金池核算请求（前端 P0-10.34 配套）
 *
 * <p>与 ComputeBonusPoolReq 区别：不接 personalCoefficient（由 service 从 kpi_records 推导），
 * 增加 period 必填（YYYY-MM，用于查询 kpi_records 当月综合得分）。
 *
 * @param projectId          项目 ID（必填）
 * @param actualReceipts     上市后连续 6 个月实际回款净额（≥0）
 * @param achievementRate    销售达成率 %（可空，null = 中性 1.0；与 tierCoefficient 二选一即可）
 * @param poolRate           奖金池比例（可空，默认 0.05）
 * @param period             考核周期 YYYY-MM（必填）
 */
public record AutoComputeBonusPoolReq(
    @NotNull(message = "projectId 不能为空")
    Long projectId,
    @NotNull(message = "actualReceipts 不能为空")
    @DecimalMin(value = "0", message = "actualReceipts 不能为负")
    BigDecimal actualReceipts,
    @DecimalMin(value = "0", message = "achievementRate 不能为负")
    BigDecimal achievementRate,
    @DecimalMin(value = "0", message = "poolRate 不能为负")
    BigDecimal poolRate,
    @NotBlank(message = "period 不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "period 格式必须为 YYYY-MM")
    String period
) { }
