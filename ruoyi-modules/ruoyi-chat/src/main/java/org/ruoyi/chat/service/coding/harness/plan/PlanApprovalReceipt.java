package org.ruoyi.chat.service.coding.harness.plan;

import java.util.UUID;

/** Persisted receipt that makes an approval command safe to retry. */
public record PlanApprovalReceipt(
    String idempotencyKey,
    UUID taskId,
    long expectedRevision,
    String expectedHash,
    long approvedRevision,
    long approvedAt
) {

    public PlanApprovalReceipt {
        if (idempotencyKey == null || idempotencyKey.isBlank() || taskId == null
            || expectedRevision < 0 || expectedHash == null || expectedHash.isBlank()
            || approvedRevision <= expectedRevision || approvedAt <= 0) {
            throw new IllegalArgumentException("Invalid plan approval receipt");
        }
        idempotencyKey = idempotencyKey.strip();
        expectedHash = expectedHash.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean matches(PlanApprovalCommand command) {
        return command != null
            && idempotencyKey.equals(command.idempotencyKey())
            && taskId.equals(command.taskId())
            && expectedRevision == command.expectedRevision()
            && expectedHash.equals(command.expectedHash());
    }
}
