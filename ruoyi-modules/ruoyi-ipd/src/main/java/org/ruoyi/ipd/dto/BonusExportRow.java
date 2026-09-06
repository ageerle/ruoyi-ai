package org.ruoyi.ipd.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * P4-4.1 奖金台账导出行（AC-INC-34）
 *
 * <p>格式规范：
 * <ul>
 *   <li>金额字段统一「元（人民币）」保留 2 位</li>
 *   <li>列头中文</li>
 *   <li>导出列含 13 列，含状态/系数/计算时间</li>
 * </ul>
 *
 * <p>导出范围：同项目过滤（奖金池按项目维度），与列表页 list 同范围（BR-INC-15）。
 * <p>脱敏：金额字段不回显关键凭证（apiKey/jwt 等），仅导出业务字段。
 */
public record BonusExportRow(
    Long poolId,
    Long projectId,
    String projectCode,
    String projectName,
    BigDecimal targetSales,
    BigDecimal poolRate,
    BigDecimal basePool,
    BigDecimal coefficient,
    BigDecimal achievementRate,
    BigDecimal tierCoefficient,
    BigDecimal finalPool,
    String status,
    Date calculatedAt,
    Date distributedAt
) {
}
