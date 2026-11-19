package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** Bounded unified diff produced without external diff or text-conversion drivers. */
public record GitDiffResult(boolean staged, int contextLines, String content, int returnedBytes,
                            boolean truncated) {

    public GitDiffResult {
        if (contextLines < 0 || content == null || returnedBytes < 0) {
            throw new IllegalArgumentException("Invalid git diff result");
        }
    }
}
