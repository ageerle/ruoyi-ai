package org.ruoyi.chat.service.coding.harness.model;

/** One durable, zero-based inclusive source range inspected by a tool call. */
public record HarnessReadSpan(
    String toolCallId,
    int startLine,
    int endLine,
    String sha256
) {

    public HarnessReadSpan {
        if (toolCallId == null || toolCallId.isBlank() || startLine < 0 || endLine < startLine) {
            throw new IllegalArgumentException("Invalid Harness read span");
        }
        sha256 = sha256 == null ? "" : sha256;
    }

    public boolean overlaps(int requestedStart, int requestedEnd) {
        return requestedStart <= endLine && requestedEnd >= startLine;
    }
}
