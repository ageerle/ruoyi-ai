package org.ruoyi.chat.service.coding.harness.approval;

/** Owner/session context does not match the approval authority boundary. */
public class ApprovalOwnershipException extends SecurityException {

    public ApprovalOwnershipException() {
        super("Tool approval owner or session does not match");
    }
}
