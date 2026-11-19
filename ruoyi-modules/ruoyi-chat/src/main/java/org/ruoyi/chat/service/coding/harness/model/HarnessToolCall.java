package org.ruoyi.chat.service.coding.harness.model;

public record HarnessToolCall(
    String toolCallId,
    String toolName,
    String arguments
) {
    public HarnessToolCall {
        if (toolCallId == null || toolCallId.isBlank() || toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Invalid tool call");
        }
    }
}
