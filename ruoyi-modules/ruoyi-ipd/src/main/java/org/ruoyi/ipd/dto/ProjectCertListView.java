package org.ruoyi.ipd.dto;

import org.ruoyi.ipd.domain.ProjectCertItem;

import java.util.List;

/**
 * P1-7.1：项目认证清单视图（含未知市场提示）。
 *
 * @param projectId       项目
 * @param catalogVersion  最近一次 AUTO 快照版本（可空）
 * @param unknownMarkets  无法映射的目标市场 token
 * @param items           清单项
 */
public record ProjectCertListView(
    Long projectId,
    String catalogVersion,
    List<String> unknownMarkets,
    List<ProjectCertItem> items
) {
}
