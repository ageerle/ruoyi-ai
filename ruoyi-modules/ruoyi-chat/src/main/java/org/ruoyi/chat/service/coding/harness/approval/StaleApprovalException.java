package org.ruoyi.chat.service.coding.harness.approval;

/** Optimistic revision supplied by a decision or worker no longer matches. */
public class StaleApprovalException extends IllegalStateException {

    public StaleApprovalException(long expected, long actual) {
        super("Stale tool approval revision: expected=" + expected + ", actual=" + actual);
    }
}
