package org.ruoyi.ipd.dto;

import java.util.List;

/**
 * P1-5.2：项目阶段门禁必做集可解释视图。
 *
 * @param projectId     项目 ID
 * @param level         项目等级 S/A/B
 * @param stage         查询阶段
 * @param configVersion 配置版本标识（A 级为规范化配置串；S/B 为目录指纹）
 * @param items         本阶段必做项清单
 */
public record GateChecklistView(
    Long projectId,
    String level,
    String stage,
    String configVersion,
    List<GateChecklistItem> items
) {
}
