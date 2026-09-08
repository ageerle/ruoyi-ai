package org.ruoyi.service.coding.harness.context;

/** Token-based retention policy; it deliberately contains no message-count threshold. */
public record ContextEnginePolicy(
    long summaryHeadroomTokens,
    long minimumRetainedTokens
) {

    public ContextEnginePolicy {
        if (summaryHeadroomTokens < 0 || minimumRetainedTokens < 0) {
            throw new IllegalArgumentException("Context policy values must be non-negative");
        }
    }

    public static ContextEnginePolicy defaults() {
        return new ContextEnginePolicy(2_048, 4_096);
    }

    /** Leave room for subsequent tool batches instead of compacting again on every turn. */
    public long preferredInputTokens(long usableInputTokens) {
        return usableInputTokens - usableInputTokens / 4;
    }

    /** A checkpoint must not grow back to occupy the space just reclaimed from the ledger. */
    public long maximumSummaryTokens(long usableInputTokens) {
        return Math.max(32, Math.max(summaryHeadroomTokens, usableInputTokens / 8));
    }
}
