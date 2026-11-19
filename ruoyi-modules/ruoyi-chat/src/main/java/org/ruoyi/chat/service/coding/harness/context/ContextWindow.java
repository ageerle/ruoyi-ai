package org.ruoyi.chat.service.coding.harness.context;

/** Token accounting for the exact model working set, excluding fixed reserved capacity. */
public record ContextWindow(
    long usableInputTokens,
    long reservedTokens,
    long pinTokens,
    long summaryTokens,
    long messageTokens,
    long inputTokens,
    boolean overBudget
) {
}
