package org.ruoyi.chat.service.coding.harness.plan;

public class StalePlanApprovalException extends IllegalStateException {

    public StalePlanApprovalException(String field, Object expected, Object actual) {
        super("Stale plan approval: " + field + " expected=" + expected + ", actual=" + actual);
    }
}
