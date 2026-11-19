package org.ruoyi.chat.service.coding.harness.approval;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;

/** Idempotent control-plane command; resolving it cannot execute a tool. */
public record ResolveApprovalCommand(
    String decisionId,
    ApprovalDecision decision,
    long expectedRevision,
    String argumentsSha256,
    HarnessOwner owner,
    String sessionId,
    String note
) {

    public ResolveApprovalCommand {
        decisionId = ApprovalValidation.requireId(decisionId, "decisionId");
        if (decision == null || expectedRevision < 0 || owner == null) {
            throw new IllegalArgumentException("Invalid approval resolution command");
        }
        argumentsSha256 = ApprovalValidation.requireSha256(argumentsSha256);
        sessionId = ApprovalValidation.requireId(sessionId, "sessionId");
        note = ApprovalValidation.normalizeOptional(note);
    }
}
