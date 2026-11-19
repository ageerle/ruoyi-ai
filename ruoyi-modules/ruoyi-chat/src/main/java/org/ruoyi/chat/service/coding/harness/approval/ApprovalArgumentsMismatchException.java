package org.ruoyi.chat.service.coding.harness.approval;

/** The exact invocation arguments no longer match the approved digest. */
public class ApprovalArgumentsMismatchException extends SecurityException {

    public ApprovalArgumentsMismatchException() {
        super("Tool arguments do not match the approval-bound SHA-256 digest");
    }
}
