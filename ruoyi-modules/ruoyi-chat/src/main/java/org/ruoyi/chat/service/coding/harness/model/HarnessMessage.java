package org.ruoyi.chat.service.coding.harness.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lossless application ledger message; separate adapters construct LangChain4j messages. */
public record HarnessMessage(
    int schemaVersion,
    String messageId,
    String sessionId,
    String runId,
    long sequence,
    HarnessMessageRole role,
    String content,
    String thinking,
    List<HarnessToolCall> toolCalls,
    String toolCallId,
    String toolName,
    boolean toolError,
    HarnessUsage usage,
    Map<String, Object> metadata,
    long timestamp
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public HarnessMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        usage = usage == null ? HarnessUsage.empty() : usage;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (schemaVersion < 1 || messageId == null || messageId.isBlank()
            || sessionId == null || sessionId.isBlank() || runId == null || runId.isBlank()
            || sequence < 0 || role == null || timestamp <= 0) {
            throw new IllegalArgumentException("Invalid Harness message");
        }
        if (role == HarnessMessageRole.TOOL && (toolCallId == null || toolCallId.isBlank())) {
            throw new IllegalArgumentException("Tool result must reference a tool call");
        }
    }

    public static HarnessMessage draft(String sessionId, String runId, HarnessMessageRole role,
                                       String content, String thinking, List<HarnessToolCall> toolCalls,
                                       String toolCallId, String toolName, boolean toolError,
                                       HarnessUsage usage, Map<String, Object> metadata, long now) {
        return new HarnessMessage(CURRENT_SCHEMA_VERSION, UUID.randomUUID().toString(), sessionId,
            runId, 0, role, content, thinking, toolCalls, toolCallId, toolName, toolError,
            usage, metadata, now);
    }

    public HarnessMessage withSequence(long newSequence) {
        return new HarnessMessage(schemaVersion, messageId, sessionId, runId, newSequence, role,
            content, thinking, toolCalls, toolCallId, toolName, toolError, usage, metadata, timestamp);
    }
}
