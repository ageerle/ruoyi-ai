package org.ruoyi.ipd.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * P4-4.1 津贴台账导出行（AC-INC-34）
 *
 * <p>格式规范：
 * <ul>
 *   <li>金额字段统一「元（人民币）」保留 2 位（半进位）</li>
 *   <li>列头中文（前端 Excel 模板直接使用）</li>
 *   <li>导出列含 11 列，固定顺序，便于前后端对账</li>
 * </ul>
 *
 * <p>导出范围：同月同项目过滤，与列表页 {@code GET /api/v1/report/project-summary?month=...&projectId=...} 同范围同筛选（BR-INC-15）。
 */
public record AllowanceExportRow(
    String month,
    Long personId,
    String personName,
    String employeeNo,
    Long projectId,
    String projectCode,
    String projectName,
    String lockedLevel,
    BigDecimal baseAmount,
    BigDecimal finalAmount,
    String capApplied,
    String stopReason,
    Date stopStartDate,
    Long ledgerId,
    Date createdAt
) {
}
