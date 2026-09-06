package org.ruoyi.ipd.dto;

import java.util.List;

/**
 * P1-9.1：批量导入逐行结果（错误行不污染成功行）。
 */
public record LegacyImportRowResult(
    int index,
    boolean ok,
    Long projectId,
    String error,
    List<String> markedCodes
) {
}
