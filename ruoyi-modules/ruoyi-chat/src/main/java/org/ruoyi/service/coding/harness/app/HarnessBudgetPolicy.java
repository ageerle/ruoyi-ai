package org.ruoyi.service.coding.harness.app;

import org.ruoyi.service.coding.harness.model.HarnessBudget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Server-authoritative ceiling; zero leaves a token dimension unbounded. */
@Component
public final class HarnessBudgetPolicy {

    private static final HarnessBudget LEGACY_DEFAULT =
        new HarnessBudget(80, 240, 0, 100_000, 6 * 60 * 60 * 1000L);

    private final HarnessBudget ceiling;

    @Autowired
    public HarnessBudgetPolicy(
        @Value("${coding.harness.budget.max-iterations:0}") int maxIterations,
        @Value("${coding.harness.budget.max-tool-calls:0}") int maxToolCalls,
        @Value("${coding.harness.budget.max-input-tokens:0}") long maxInputTokens,
        @Value("${coding.harness.budget.max-output-tokens:0}") long maxOutputTokens,
        @Value("${coding.harness.budget.max-wall-time-millis:0}") long maxWallTimeMillis
    ) {
        this(new HarnessBudget(maxIterations, maxToolCalls, maxInputTokens, maxOutputTokens,
            maxWallTimeMillis));
    }

    public HarnessBudgetPolicy(HarnessBudget ceiling) {
        if (ceiling == null) {
            throw new IllegalArgumentException("Harness spending policy is required");
        }
        this.ceiling = ceiling;
    }

    public static HarnessBudgetPolicy secureDefaults() {
        return new HarnessBudgetPolicy(HarnessBudget.defaults());
    }

    public HarnessBudget enforce(HarnessBudget requested) {
        HarnessBudget value = requested == null ? HarnessBudget.defaults() : requested;
        return new HarnessBudget(
            0,
            (int) bounded(value.maxToolCalls(), ceiling.maxToolCalls()),
            bounded(value.maxInputTokens(), ceiling.maxInputTokens()),
            bounded(value.maxOutputTokens(), ceiling.maxOutputTokens()),
            0);
    }

    /** Upgrades only the former server default; explicitly smaller custom budgets stay smaller. */
    public HarnessBudget forFollowUp(HarnessBudget inherited) {
        return enforce(LEGACY_DEFAULT.equals(inherited) ? null : inherited);
    }

    private long bounded(long requested, long maximum) {
        if (maximum == 0) {
            return requested;
        }
        return requested == 0 ? maximum : Math.min(requested, maximum);
    }
}
