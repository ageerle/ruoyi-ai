package org.ruoyi.chat.service.coding.harness.model;

import java.util.EnumSet;
import java.util.Set;

public enum HarnessRunStatus {
    QUEUED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    WAITING_FOR_INPUT,
    SUSPENDED,
    COMPLETED,
    FAILED,
    CANCELLED;

    private static final Set<HarnessRunStatus> TERMINAL =
        EnumSet.of(COMPLETED, FAILED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(HarnessRunStatus target) {
        if (target == null || this == target) {
            return target == this;
        }
        if (isTerminal()) {
            return false;
        }
        return switch (this) {
            case QUEUED -> target == RUNNING || target == CANCELLED || target == FAILED;
            case RUNNING -> target == WAITING_FOR_APPROVAL || target == WAITING_FOR_INPUT
                || target == SUSPENDED || target.isTerminal();
            case WAITING_FOR_APPROVAL, WAITING_FOR_INPUT, SUSPENDED ->
                target == QUEUED || target == CANCELLED || target == FAILED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }
}
