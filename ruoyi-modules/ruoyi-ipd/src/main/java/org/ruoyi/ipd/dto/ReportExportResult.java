package org.ruoyi.ipd.dto;

import java.util.List;
import java.util.Map;

/**
 * P4-4.1 导出结果（BR-INC-15）
 *
 * <p>导出动作本身写一条 EXPORT_REPORT 审计（独立事务）。
 * <p>返回结构：
 * <ul>
 *   <li>{@code exportType} — 导出类型（allowance / bonus / project）</li>
 *   <li>{@code totalCount} — 命中行数</li>
 *   <li>{@code headers} — Excel 列头（中文）</li>
 *   <li>{@code rows} — 实际数据行（已转换为 Map 便于前端渲染）</li>
 *   <li>{@code filters} — 实际应用的筛选条件（month/projectId/productId 等）</li>
 * </ul>
 *
 * <p>金额单位：「元（人民币）」保留 2 位半进位。
 */
public record ReportExportResult(
    String exportType,
    int totalCount,
    List<String> headers,
    List<Map<String, Object>> rows,
    Map<String, Object> filters,
    String exportedBy,
    String exportedAt
) {
}
