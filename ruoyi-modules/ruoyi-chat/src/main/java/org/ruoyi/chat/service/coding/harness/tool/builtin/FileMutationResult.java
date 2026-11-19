package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** Durable identity and concise change summary returned after an atomic mutation. */
public record FileMutationResult(
    String path,
    String sha256,
    long sizeBytes,
    boolean created,
    String summary
) {
}
