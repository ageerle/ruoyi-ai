package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * P3-4.4：奖金池核算请求（前端 P0-10.34）
 *
 * <p>业务规则：BR-INC-04 + ZK-IPD §三.2.1/§三.2.5；调用方传实际回款 + 销售达成率 + 个人绩效系数，
 * 服务层按 {@code actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient}
 * 计算 finalPool，状态 = DRAFT。
 *
 * @param projectId          项目 ID（必填）
 * @param actualReceipts     上市后连续 6 个月实际回款净额（≥0）
 * @param achievementRate    销售达成率 %（可空，null = 中性 1.0；与 tierCoefficient 二选一即可）
 * @param personalCoefficient 个人绩效系数（可空，null = 中性 1.0）
 * @param poolRate           奖金池比例（可空，默认 0.05）
 */
public record ComputeBonusPoolReq(
    @NotNull(message = "项目 ID 不能为空") Long projectId,
    @NotNull(message = "实际回款金额不能为空")
    @DecimalMin(value = "0", message = "实际回款金额不能为负")
    BigDecimal actualReceipts,

    @DecimalMin(value = "0", message = "销售达成率不能为负")
    BigDecimal achievementRate,

    @DecimalMin(value = "0", message = "个人绩效系数不能为负")
    BigDecimal personalCoefficient,

    @DecimalMin(value = "0", message = "奖金池比例不能为负")
    BigDecimal poolRate
) {
}
