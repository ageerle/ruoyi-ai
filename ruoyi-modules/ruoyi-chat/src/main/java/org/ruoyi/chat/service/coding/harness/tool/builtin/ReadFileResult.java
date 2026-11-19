package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** Bounded file page together with identity metadata needed for optimistic writes. */
public record ReadFileResult(
    String path,
    long sizeBytes,
    String sha256,
    boolean binary,
    String encoding,
    String content,
    int offset,
    int returnedLines,
    int startLine,
    int endLine,
    int totalLines,
    boolean truncated
) {
}
