package org.ruoyi.chat.service.coding.harness.approval;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;

import java.util.Objects;

/** Persisted idempotency receipt for an HTTP/control-plane decision. */
public record ApprovalDecisionReceipt(
    String decisionId,
    ApprovalDecision decision,
    long expectedRevision,
    long resolvedRevision,
    String argumentsSha256,
    HarnessOwner owner,
    String sessionId,
    String note,
    long resolvedAt
) {

    public ApprovalDecisionReceipt {
        decisionId = ApprovalValidation.requireId(decisionId, "decisionId");
        if (decision == null || expectedRevision < 0 || resolvedRevision != expectedRevision + 1
            || owner == null || resolvedAt <= 0) {
            throw new IllegalArgumentException("Invalid approval decision receipt");
        }
        argumentsSha256 = ApprovalValidation.requireSha256(argumentsSha256);
        sessionId = ApprovalValidation.requireId(sessionId, "sessionId");
        note = ApprovalValidation.normalizeOptional(note);
    }

    public boolean matches(ResolveApprovalCommand command) {
        return command != null
            && decisionId.equals(command.decisionId())
            && decision == command.decision()
            && expectedRevision == command.expectedRevision()
            && argumentsSha256.equals(command.argumentsSha256())
            && owner.equals(command.owner())
            && sessionId.equals(command.sessionId())
            && Objects.equals(note, command.note());
    }
}
