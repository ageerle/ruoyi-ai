package org.ruoyi.chat.service.coding.harness.context;

import java.util.ArrayList;
import java.util.List;

/** Durable-friendly retry guard. Working messages and checkpoints are not stored here. */
public record CompactionControl(
    int consecutiveFailures,
    List<String> attemptedEmergencyOverflowIds
) {

    public static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int MAX_TRACKED_OVERFLOWS = 64;

    public CompactionControl {
        attemptedEmergencyOverflowIds = attemptedEmergencyOverflowIds == null
            ? List.of() : List.copyOf(attemptedEmergencyOverflowIds);
        if (consecutiveFailures < 0 || consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
            throw new IllegalArgumentException("Invalid compaction failure count");
        }
    }

    public static CompactionControl initial() {
        return new CompactionControl(0, List.of());
    }

    public boolean circuitOpen() {
        return consecutiveFailures >= MAX_CONSECUTIVE_FAILURES;
    }

    public boolean emergencyAttempted(String overflowId) {
        return overflowId != null && attemptedEmergencyOverflowIds.contains(overflowId);
    }

    public CompactionControl failed(String overflowId) {
        return new CompactionControl(Math.min(MAX_CONSECUTIVE_FAILURES, consecutiveFailures + 1),
            rememberOverflow(overflowId));
    }

    public CompactionControl succeeded(String overflowId) {
        return new CompactionControl(0, rememberOverflow(overflowId));
    }

    public CompactionControl resetCircuit() {
        return new CompactionControl(0, attemptedEmergencyOverflowIds);
    }

    private List<String> rememberOverflow(String overflowId) {
        if (overflowId == null || attemptedEmergencyOverflowIds.contains(overflowId)) {
            return attemptedEmergencyOverflowIds;
        }
        List<String> next = new ArrayList<>(attemptedEmergencyOverflowIds);
        next.add(overflowId);
        if (next.size() > MAX_TRACKED_OVERFLOWS) {
            next = new ArrayList<>(next.subList(next.size() - MAX_TRACKED_OVERFLOWS, next.size()));
        }
        return List.copyOf(next);
    }
}
