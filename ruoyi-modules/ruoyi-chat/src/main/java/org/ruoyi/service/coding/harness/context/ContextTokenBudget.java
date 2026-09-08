package org.ruoyi.service.coding.harness.context;

/** Token-only capacity model. Fixed provider and safety reservations are explicit. */
public record ContextTokenBudget(
    long contextWindowTokens,
    long systemPromptTokens,
    long toolSchemaTokens,
    long maxOutputTokens,
    long toolGrowthTokens,
    long safetyMarginTokens
) {

    public ContextTokenBudget {
        if (contextWindowTokens <= 0 || systemPromptTokens < 0 || toolSchemaTokens < 0
            || maxOutputTokens < 0 || toolGrowthTokens < 0 || safetyMarginTokens < 0) {
            throw new IllegalArgumentException("Token budget values must be non-negative");
        }
        if (reservedTokens(systemPromptTokens, toolSchemaTokens, maxOutputTokens,
            toolGrowthTokens, safetyMarginTokens) >= contextWindowTokens) {
            throw new IllegalArgumentException("Token reservations exhaust the context window");
        }
    }

    public long reservedTokens() {
        return reservedTokens(systemPromptTokens, toolSchemaTokens, maxOutputTokens,
            toolGrowthTokens, safetyMarginTokens);
    }

    public long usableInputTokens() {
        return contextWindowTokens - reservedTokens();
    }

    /** Soft per-call latency target; never increases the provider's hard capacity. Zero disables it. */
    public ContextTokenBudget withActiveInputLimit(long inputLimit) {
        if (inputLimit < 0) {
            throw new IllegalArgumentException("Active input limit must be non-negative");
        }
        if (inputLimit == 0 || inputLimit >= usableInputTokens()) {
            return this;
        }
        return new ContextTokenBudget(reservedTokens() + inputLimit, systemPromptTokens,
            toolSchemaTokens, maxOutputTokens, toolGrowthTokens, safetyMarginTokens);
    }

    private static long reservedTokens(long system, long tools, long output,
                                       long growth, long safety) {
        try {
            return Math.addExact(Math.addExact(Math.addExact(system, tools),
                Math.addExact(output, growth)), safety);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Token reservations overflow", exception);
        }
    }
}
