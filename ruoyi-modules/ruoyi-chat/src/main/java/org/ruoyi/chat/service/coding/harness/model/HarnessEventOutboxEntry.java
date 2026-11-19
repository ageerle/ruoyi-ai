package org.ruoyi.chat.service.coding.harness.model;

import java.util.Objects;

/**
 * One stable event draft committed in the same snapshot as its control-plane mutation.
 *
 * <p>The draft deliberately has sequence {@code 0}. Only the event ledger may assign a durable
 * sequence. Keeping the complete draft in the run snapshot makes an append/acknowledgement crash
 * window replayable by {@link HarnessEvent#eventId()}.</p>
 */
public record HarnessEventOutboxEntry(
    int schemaVersion,
    HarnessEvent event,
    long enqueuedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public HarnessEventOutboxEntry {
        Objects.requireNonNull(event, "event");
        if (schemaVersion < 1 || event.sequence() != 0 || enqueuedAt <= 0
            || enqueuedAt < event.timestamp()) {
            throw new IllegalArgumentException("Invalid Harness event outbox entry");
        }
    }

    public static HarnessEventOutboxEntry create(HarnessEvent event, long now) {
        return new HarnessEventOutboxEntry(CURRENT_SCHEMA_VERSION, event, now);
    }

    public boolean belongsTo(String sessionId, String runId) {
        return event.sessionId().equals(sessionId) && event.runId().equals(runId);
    }
}
