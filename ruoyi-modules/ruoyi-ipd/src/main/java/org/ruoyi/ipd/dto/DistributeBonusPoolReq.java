package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * P3-4.4：奖金分配请求（前端 P0-10.34）
 *
 * <p>业务规则：ZK-IPD §三.2.4
 * <ul>
 *   <li>marketShare ∈ [40%, 65%]</li>
 *   <li>rdShare     ∈ [35%, 60%]</li>
 *   <li>marketShare + rdShare = 100%（容差 0.0001）</li>
 * </ul>
 *
 * @param marketShare 市场 PM 分配比例（0.40~0.65）
 * @param rdShare     研发 PM 分配比例（0.35~0.60）
 */
public record DistributeBonusPoolReq(
    @NotNull(message = "市场 PM 分配比例不能为空")
    @DecimalMin(value = "0.40", message = "市场 PM 比例须 ≥ 40%")
    @DecimalMax(value = "0.65", message = "市场 PM 比例须 ≤ 65%")
    BigDecimal marketShare,

    @NotNull(message = "研发 PM 分配比例不能为空")
    @DecimalMin(value = "0.35", message = "研发 PM 比例须 ≥ 35%")
    @DecimalMax(value = "0.60", message = "研发 PM 比例须 ≤ 60%")
    BigDecimal rdShare
) {
}
