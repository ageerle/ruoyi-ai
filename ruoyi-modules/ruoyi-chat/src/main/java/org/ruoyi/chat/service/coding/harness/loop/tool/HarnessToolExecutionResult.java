package org.ruoyi.chat.service.coding.harness.loop.tool;

/** Normalized result of invoking one registered LangChain4j tool method. */
public record HarnessToolExecutionResult(
    String callId,
    String toolName,
    boolean error,
    String code,
    String content,
    long durationMillis
) {
    public HarnessToolExecutionResult {
        if (callId == null || callId.isBlank() || toolName == null || toolName.isBlank()
            || code == null || code.isBlank() || content == null || durationMillis < 0) {
            throw new IllegalArgumentException("Invalid tool execution result");
        }
    }
}
