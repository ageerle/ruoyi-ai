package org.ruoyi.service.coding.harness.model;

/** Optional spending limits. Legacy iteration/wall-time fields are retained as zero for wire compatibility. */
public record HarnessBudget(
    int maxIterations,
    int maxToolCalls,
    long maxInputTokens,
    long maxOutputTokens,
    long maxWallTimeMillis
) {

    public static HarnessBudget defaults() {
        return new HarnessBudget(0, 0, 0, 0, 0);
    }

    public HarnessBudget {
        if (maxToolCalls < 0 || maxInputTokens < 0 || maxOutputTokens < 0) {
            throw new IllegalArgumentException("Invalid Harness budget");
        }
        maxIterations = 0;
        maxWallTimeMillis = 0;
    }
}
