package org.ruoyi.chat.service.coding.harness.approval;

/** A command id or one-shot approval was already used by a different payload. */
public class ApprovalConflictException extends IllegalStateException {

    public ApprovalConflictException(String message) {
        super(message);
    }
}
