package org.ruoyi.chat.service.coding.harness.approval;

/** Stable machine-readable reasons used to synthesize a fail-closed tool outcome. */
public enum ApprovalOutcomeReason {
    DENIED("approval_denied", "Tool call was denied before execution."),
    EXPIRED("approval_expired", "Tool approval expired before execution."),
    PERMISSION_CHANGED("approval_permission_changed",
        "Tool approval no longer matches the active permission snapshot.");

    private final String code;
    private final String message;

    ApprovalOutcomeReason(String code, String message) {
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
