package org.ruoyi.chat.service.coding.harness.model;

import java.util.Map;
import java.util.UUID;

public record HarnessEvent(
    int schemaVersion,
    String eventId,
    String sessionId,
    String runId,
    long sequence,
    long timestamp,
    String type,
    String stepId,
    String toolCallId,
    String approvalId,
    Map<String, Object> data
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public HarnessEvent {
        data = data == null ? Map.of() : Map.copyOf(data);
        if (schemaVersion < 1 || eventId == null || eventId.isBlank()
            || sessionId == null || sessionId.isBlank() || runId == null || runId.isBlank()
            || sequence < 0 || timestamp <= 0 || type == null || type.isBlank()) {
            throw new IllegalArgumentException("Invalid Harness event");
        }
    }

    public static HarnessEvent draft(String sessionId, String runId, String type,
                                     String stepId, String toolCallId, String approvalId,
                                     Map<String, Object> data, long now) {
        return draftWithId(UUID.randomUUID().toString(), sessionId, runId, type, stepId,
            toolCallId, approvalId, data, now);
    }

    /** Creates a replay-stable draft for a durable outbox. */
    public static HarnessEvent draftWithId(String eventId, String sessionId, String runId,
                                           String type, String stepId, String toolCallId,
                                           String approvalId, Map<String, Object> data, long now) {
        return new HarnessEvent(CURRENT_SCHEMA_VERSION, eventId, sessionId,
            runId, 0, now, type, stepId, toolCallId, approvalId, data);
    }

    public HarnessEvent withSequence(long newSequence) {
        return new HarnessEvent(schemaVersion, eventId, sessionId, runId, newSequence, timestamp,
            type, stepId, toolCallId, approvalId, data);
    }
}
