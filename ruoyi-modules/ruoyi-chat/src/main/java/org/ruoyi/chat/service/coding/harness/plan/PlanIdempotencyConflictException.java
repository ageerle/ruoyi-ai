package org.ruoyi.chat.service.coding.harness.plan;

public class PlanIdempotencyConflictException extends IllegalStateException {

    public PlanIdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key was already used with a different payload: " + idempotencyKey);
    }
}
