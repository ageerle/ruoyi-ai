package org.ruoyi.service.coding.harness.event;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.coding.harness.model.HarnessEventOutboxEntry;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.store.HarnessOptimisticLockException;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Publishes run-attached API events and durably acknowledges them after ledger admission. */
@Service
@Slf4j
public final class HarnessEventOutboxService {

    private static final int MAX_ACKNOWLEDGEMENT_RETRIES = 8;

    private final HarnessStore store;
    private final HarnessEventHub eventHub;

    public HarnessEventOutboxService(HarnessStore store, HarnessEventHub eventHub) {
        this.store = Objects.requireNonNull(store, "store");
        this.eventHub = Objects.requireNonNull(eventHub, "eventHub");
    }

    /**
     * Best-effort drain used after API commits and by the durable maintenance cursor.
     * Publication or acknowledgement failure leaves the complete draft in the run snapshot.
     */
    public HarnessRunState drainBestEffort(HarnessOwner owner, HarnessRunState observed) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(observed, "observed");
        if (!owner.equals(observed.owner())) {
            throw new IllegalArgumentException("Event outbox owner does not match the run");
        }

        HarnessRunState current = observed;
        int acknowledged = 0;
        while (acknowledged < HarnessRunState.MAX_EVENT_OUTBOX_ENTRIES) {
            try {
                current = store.findRun(owner, observed.sessionId(), observed.runId())
                    .orElse(current);
                if (current.eventOutbox().isEmpty()) {
                    ensureTerminalEventAfterBarrier(owner, current);
                    return current;
                }
                HarnessEventOutboxEntry entry = current.eventOutbox().get(0);
                eventHub.publishIdempotent(owner, entry.event());
                HarnessRunState next = acknowledge(owner, current, entry.event().eventId());
                if (next.eventOutbox().stream().anyMatch(candidate ->
                    candidate.event().eventId().equals(entry.event().eventId()))) {
                    return next;
                }
                current = next;
                acknowledged++;
            } catch (RuntimeException deferred) {
                log.warn("Deferred Harness event outbox for run {}", observed.runId(), deferred);
                return current;
            }
        }
        return current;
    }

    /**
     * Repairs a terminal state event only after the durable FIFO is empty. This deliberately does
     * not drain first: callers decide when retrying publication is appropriate, while the hub
     * performs a second snapshot check under its per-run publication lock.
     */
    public void ensureTerminalEventAfterBarrier(HarnessOwner owner, HarnessRunState observed) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(observed, "observed");
        HarnessRunState latest = store.findRun(owner, observed.sessionId(), observed.runId())
            .orElse(observed);
        if (latest.status().isTerminal() && latest.eventOutbox().isEmpty()) {
            eventHub.ensureTerminalEvent(owner, latest.sessionId(), latest.runId());
        }
    }

    private HarnessRunState acknowledge(HarnessOwner owner, HarnessRunState observed,
                                        String eventId) {
        HarnessRunState current = observed;
        for (int attempt = 0; attempt < MAX_ACKNOWLEDGEMENT_RETRIES; attempt++) {
            HarnessRunState latest = store.findRun(owner, observed.sessionId(), observed.runId())
                .orElseThrow(() -> new IllegalStateException(
                    "Run disappeared while acknowledging an event outbox entry"));
            boolean pending = latest.eventOutbox().stream().anyMatch(entry ->
                entry.event().eventId().equals(eventId));
            if (!pending) {
                return latest;
            }
            try {
                return store.saveRun(owner,
                    latest.acknowledgeEvent(eventId, System.currentTimeMillis()), latest.revision());
            } catch (HarnessOptimisticLockException conflict) {
                current = latest;
            }
        }
        return current;
    }
}
