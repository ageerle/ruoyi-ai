package org.ruoyi.chat.service.coding.harness.loop.protocol;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;
import org.ruoyi.chat.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.chat.service.coding.harness.model.HarnessUsage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Projection of one assistant tool-call batch. Results are always ordered by the
 * assistant's source call order, regardless of execution completion order.
 */
public record ToolBatchProjection(
    HarnessMessage assistantMessage,
    List<HarnessToolCall> calls,
    List<HarnessMessage> orderedResults,
    List<HarnessToolCall> missingCalls,
    List<ToolProtocolViolation> violations
) {

    public ToolBatchProjection {
        if (assistantMessage == null || assistantMessage.role() != HarnessMessageRole.ASSISTANT) {
            throw new IllegalArgumentException("A tool batch requires its assistant message");
        }
        calls = calls == null ? List.of() : List.copyOf(calls);
        orderedResults = orderedResults == null ? List.of() : List.copyOf(orderedResults);
        missingCalls = missingCalls == null ? List.of() : List.copyOf(missingCalls);
        violations = violations == null ? List.of() : List.copyOf(violations);
        if (calls.isEmpty()) {
            throw new IllegalArgumentException("A tool batch cannot be empty");
        }
    }

    public boolean complete() {
        return missingCalls.isEmpty() && orderedResults.size() == calls.size()
            && violations.isEmpty();
    }

    /** Generates missing result slots in source order with no random or process-local identity. */
    public List<HarnessMessage> synthesizeMissing(SyntheticToolResultReason reason,
                                                  long firstSequence, long timestamp) {
        List<HarnessMessage> results = new ArrayList<>(missingCalls.size());
        for (int index = 0; index < missingCalls.size(); index++) {
            HarnessToolCall call = missingCalls.get(index);
            long sequence;
            try {
                sequence = Math.addExact(firstSequence, index);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Synthetic result sequence overflow", exception);
            }
            results.add(synthesizeMissing(call.toolCallId(), reason, sequence, timestamp));
        }
        return List.copyOf(results);
    }

    /** Generates exactly one durable result slot for a currently missing source call. */
    public HarnessMessage synthesizeMissing(String toolCallId, SyntheticToolResultReason reason,
                                            long sequence, long timestamp) {
        if (reason == null || sequence < 0 || timestamp <= 0) {
            throw new IllegalArgumentException("Invalid synthetic result request");
        }
        HarnessToolCall call = missingCalls.stream()
            .filter(candidate -> candidate.toolCallId().equals(toolCallId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Tool call is not missing from this batch: " + toolCallId));
        String messageId = stableResultId(assistantMessage, call);
        return new HarnessMessage(HarnessMessage.CURRENT_SCHEMA_VERSION, messageId,
            assistantMessage.sessionId(), assistantMessage.runId(), sequence,
            HarnessMessageRole.TOOL, reason.message(), null, List.of(), call.toolCallId(),
            call.toolName(), true, HarnessUsage.empty(),
            Map.of("synthetic", true, "syntheticReason", reason.name(),
                "syntheticCode", reason.code(),
                "sourceAssistantMessageId", assistantMessage.messageId()),
            timestamp);
    }

    /** Replays a durable control receipt instead of falsely claiming cancellation/non-execution. */
    public HarnessMessage synthesizeCommitted(String toolCallId, String committedResult,
                                               String effectId, long sequence, long timestamp) {
        HarnessToolCall call = requireMissing(toolCallId, sequence, timestamp);
        if (committedResult == null || committedResult.isBlank()
            || effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("Committed tool result identity is required");
        }
        return new HarnessMessage(HarnessMessage.CURRENT_SCHEMA_VERSION,
            stableResultId(assistantMessage, call), assistantMessage.sessionId(),
            assistantMessage.runId(), sequence, HarnessMessageRole.TOOL, committedResult, null,
            List.of(), call.toolCallId(), call.toolName(), false, HarnessUsage.empty(),
            Map.of("code", "CONTROL_COMMITTED", "effectId", effectId,
                "synthetic", true, "syntheticReason", "COMMITTED",
                "sourceAssistantMessageId", assistantMessage.messageId()), timestamp);
    }

    /** Closes a malformed but uniquely addressable call with the validator-required marker. */
    public HarnessMessage synthesizeInvalid(String toolCallId, String detail,
                                             long sequence, long timestamp) {
        HarnessToolCall call = requireMissing(toolCallId, sequence, timestamp);
        String message = detail == null || detail.isBlank()
            ? "Tool arguments must be exactly one JSON object" : detail;
        String content = "{\"ok\":false,\"code\":\"invalid_tool_call\",\"message\":\""
            + escapeJson(message) + "\"}";
        return new HarnessMessage(HarnessMessage.CURRENT_SCHEMA_VERSION,
            stableResultId(assistantMessage, call), assistantMessage.sessionId(),
            assistantMessage.runId(), sequence, HarnessMessageRole.TOOL, content, null,
            List.of(), call.toolCallId(), call.toolName(), true, HarnessUsage.empty(),
            Map.of("code", "invalid_tool_call", "synthetic", true,
                "syntheticReason", "INVALID",
                "sourceAssistantMessageId", assistantMessage.messageId()), timestamp);
    }

    private HarnessToolCall requireMissing(String toolCallId, long sequence, long timestamp) {
        if (sequence < 0 || timestamp <= 0) {
            throw new IllegalArgumentException("Invalid synthetic result request");
        }
        return missingCalls.stream()
            .filter(candidate -> candidate.toolCallId().equals(toolCallId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Tool call is not missing from this batch: " + toolCallId));
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String stableResultId(HarnessMessage assistant, HarnessToolCall call) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "harness-synthetic-tool-result-v1");
            update(digest, assistant.sessionId());
            update(digest, assistant.runId());
            update(digest, assistant.messageId());
            update(digest, call.toolCallId());
            return "synthetic-tool-result-" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
