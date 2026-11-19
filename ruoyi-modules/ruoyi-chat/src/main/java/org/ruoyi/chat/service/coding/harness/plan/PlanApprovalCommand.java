package org.ruoyi.chat.service.coding.harness.plan;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Control-plane-only command. This type must not be exposed as an LLM tool. */
public record PlanApprovalCommand(
    UUID taskId,
    long expectedRevision,
    String expectedHash,
    String idempotencyKey
) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public PlanApprovalCommand {
        if (taskId == null) {
            throw new IllegalArgumentException("Approval taskId must not be null");
        }
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("Approval expectedRevision must not be negative");
        }
        if (expectedHash == null || expectedHash.isBlank()) {
            throw new IllegalArgumentException("Approval expectedHash must not be blank");
        }
        expectedHash = expectedHash.strip().toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(expectedHash).matches()) {
            throw new IllegalArgumentException("Approval expectedHash must be a SHA-256 hex digest");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Approval idempotencyKey must not be blank");
        }
        idempotencyKey = idempotencyKey.strip();
    }
}
