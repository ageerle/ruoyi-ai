package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.util.List;

/** Bounded result returned by list_files and glob_files. */
public record FileListResult(
    String basePath,
    String glob,
    List<FileListEntry> entries,
    boolean truncated
) {
    public FileListResult {
        entries = List.copyOf(entries);
    }
}
