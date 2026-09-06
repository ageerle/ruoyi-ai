package org.ruoyi.ipd.dto;

/**
 * P1-3.3：版本列表轻量视图（不含 mediumtext content，只带长度）。
 */
public record SopTemplateListItem(Long id, String actionCode, String title, Integer version,
                                  String status, Integer contentLen) {
}
