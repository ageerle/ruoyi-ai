package org.ruoyi.ipd.dto;

import org.ruoyi.ipd.domain.Project;

import java.util.List;

/**
 * P1-9.1：单条存量导入结果。
 *
 * @param project     落库项目
 * @param markedCodes 被标「历史缺失」的动作编码
 */
public record LegacyImportResult(
    Project project,
    List<String> markedCodes
) {
}
