package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;

import java.util.List;

/** A role-preserving request. Previous summary is separate from newly archived raw messages. */
public record SummaryRequest(
    ContextPins pins,
    String previousCheckpointId,
    String previousSummary,
    List<HarnessMessage> messages,
    long fromSequence,
    long toSequence,
    long targetSummaryTokens,
    String modelIdentity,
    long sourceUsageTimestamp,
    boolean emergency
) {

    public SummaryRequest {
        if (pins == null || messages == null || messages.isEmpty()
            || fromSequence <= 0 || toSequence < fromSequence || targetSummaryTokens < 0
            || modelIdentity == null || modelIdentity.isBlank() || sourceUsageTimestamp < 0) {
            throw new IllegalArgumentException("Invalid summary request");
        }
        messages = List.copyOf(messages);
        previousSummary = previousSummary == null ? "" : previousSummary;
        previousCheckpointId = previousCheckpointId == null || previousCheckpointId.isBlank()
            ? null : previousCheckpointId;
    }
}
