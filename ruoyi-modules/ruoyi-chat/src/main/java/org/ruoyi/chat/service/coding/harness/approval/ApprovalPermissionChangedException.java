package org.ruoyi.chat.service.coding.harness.approval;

/** A worker's current permission snapshot differs from the snapshot awaiting approval. */
public class ApprovalPermissionChangedException extends SecurityException {

    public ApprovalPermissionChangedException() {
        super(ApprovalOutcomeReason.PERMISSION_CHANGED.code());
    }

    public ApprovalOutcomeReason reason() {
        return ApprovalOutcomeReason.PERMISSION_CHANGED;
    }
}
