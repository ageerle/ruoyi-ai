package org.ruoyi.chat.service.coding.harness.store;

import org.ruoyi.chat.service.coding.harness.model.HarnessRunState;

import java.util.List;

/**
 * One stable, read-only page of durable run snapshots used by startup recovery.
 * {@code nextCursor} is opaque to callers and is {@code null} when the scan is exhausted.
 */
public record HarnessRunScanPage(List<HarnessRunState> runs, String nextCursor) {

    public HarnessRunScanPage {
        runs = runs == null ? List.of() : List.copyOf(runs);
        if (nextCursor != null && nextCursor.isBlank()) {
            throw new IllegalArgumentException("Recovery scan cursor cannot be blank");
        }
    }

    public boolean hasMore() {
        return nextCursor != null;
    }
}
