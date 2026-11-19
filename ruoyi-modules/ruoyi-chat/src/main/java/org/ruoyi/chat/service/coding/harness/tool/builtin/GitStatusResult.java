package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.util.List;

/** Bounded, machine-readable projection of the current lease-root worktree. */
public record GitStatusResult(List<GitStatusEntry> entries, boolean clean, boolean truncated) {

    public GitStatusResult {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (clean && (!entries.isEmpty() || truncated)) {
            throw new IllegalArgumentException("A clean status cannot contain entries or truncated output");
        }
        if (!clean && !truncated && entries.isEmpty()) {
            throw new IllegalArgumentException("A complete empty status must be clean");
        }
    }
}
