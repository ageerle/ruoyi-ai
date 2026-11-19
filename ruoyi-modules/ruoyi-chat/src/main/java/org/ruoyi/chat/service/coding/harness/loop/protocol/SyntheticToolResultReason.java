package org.ruoyi.chat.service.coding.harness.loop.protocol;

/** Fail-closed reasons for settling a call without claiming that its effect succeeded. */
public enum SyntheticToolResultReason {
    DENY("tool_denied", "Tool call was denied by runtime policy."),
    CANCEL("tool_cancelled", "Tool call was cancelled before a result was committed."),
    LIMIT("tool_not_executed", "Tool call was not executed because the run terminated."),
    UNKNOWN("tool_outcome_unknown",
        "Tool outcome is unknown after recovery; the call was not replayed."),
    ;

    private final String code;
    private final String message;

    SyntheticToolResultReason(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
