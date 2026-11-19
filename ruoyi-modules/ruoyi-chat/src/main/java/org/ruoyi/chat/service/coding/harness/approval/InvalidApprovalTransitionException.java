package org.ruoyi.chat.service.coding.harness.approval;

/** Requested action is incompatible with the authoritative approval state. */
public class InvalidApprovalTransitionException extends IllegalStateException {

    public InvalidApprovalTransitionException(ApprovalState state, String action) {
        super("Cannot " + action + " while tool approval is " + state);
    }
}
