package org.ruoyi.chat.service.coding.harness.model;

public record HarnessUsage(long inputTokens, long outputTokens, long totalTokens) {
    public HarnessUsage {
        if (inputTokens < 0 || outputTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("Usage cannot be negative");
        }
    }

    public static HarnessUsage empty() {
        return new HarnessUsage(0, 0, 0);
    }
}
