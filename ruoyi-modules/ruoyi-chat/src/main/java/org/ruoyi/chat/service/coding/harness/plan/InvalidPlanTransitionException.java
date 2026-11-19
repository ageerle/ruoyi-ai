package org.ruoyi.chat.service.coding.harness.plan;

public class InvalidPlanTransitionException extends IllegalStateException {

    public InvalidPlanTransitionException(ExecutionMode source, String action) {
        super("Cannot " + action + " while plan is in " + source + " mode");
    }
}
