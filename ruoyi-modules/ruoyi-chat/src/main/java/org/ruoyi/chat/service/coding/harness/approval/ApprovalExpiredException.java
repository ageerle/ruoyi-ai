package org.ruoyi.chat.service.coding.harness.approval;

/** The tool approval deadline has passed and execution is denied. */
public class ApprovalExpiredException extends IllegalStateException {

    public ApprovalExpiredException() {
        super(ApprovalOutcomeReason.EXPIRED.code());
    }

    public ApprovalOutcomeReason reason() {
        return ApprovalOutcomeReason.EXPIRED;
    }
}
