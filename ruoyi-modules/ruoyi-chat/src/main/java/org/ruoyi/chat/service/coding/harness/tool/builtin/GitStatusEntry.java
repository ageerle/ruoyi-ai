package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** One porcelain-v1 worktree status entry. Paths are workspace-relative and never shell-decoded. */
public record GitStatusEntry(String status, String path, String originalPath) {

    public GitStatusEntry {
        if (status == null || status.length() != 2) {
            throw new IllegalArgumentException("Git status must contain the two porcelain status columns");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Git status path is required");
        }
    }
}
