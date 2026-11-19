package org.ruoyi.chat.service.coding.harness.plan;

/** Persisted scheduler state for one authoritative plan step. */
public enum PlanTaskStepStatus {
    PENDING,
    IN_PROGRESS,
    BLOCKED,
    FAILED,
    COMPLETED,
    SKIPPED;

    public boolean satisfiesDependency() {
        return this == COMPLETED || this == SKIPPED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == SKIPPED;
    }
}
