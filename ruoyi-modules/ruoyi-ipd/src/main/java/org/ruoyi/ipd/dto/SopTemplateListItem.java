package org.ruoyi.ipd.dto;

/**
 * P1-3.3 SOP 模板轻量视图（保留必要字段供前端版本列表展示）。
 */
public record SopTemplateListItem(Long id, String templateCode, String templateName,
                                  Long version, String status, String category,
                                  java.util.Date effectiveFrom, java.util.Date effectiveTo) {
}