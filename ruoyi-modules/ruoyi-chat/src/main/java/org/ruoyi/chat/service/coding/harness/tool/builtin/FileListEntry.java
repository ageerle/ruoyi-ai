package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** Deterministic workspace-relative directory entry. */
public record FileListEntry(String path, String type, long sizeBytes) {
}
