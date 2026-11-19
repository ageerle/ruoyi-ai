package org.ruoyi.chat.service.coding.harness.plan;

/** Authoritative execution phase for a coding task. */
public enum ExecutionMode {
    PLAN,
    BUILD,
    VERIFY,
    COMPLETED,
    BLOCKED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
