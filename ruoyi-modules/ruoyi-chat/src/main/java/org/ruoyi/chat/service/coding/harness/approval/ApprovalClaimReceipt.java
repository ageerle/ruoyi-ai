package org.ruoyi.chat.service.coding.harness.approval;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

/** Persisted receipt proving which worker consumed the one-shot approval. */
public record ApprovalClaimReceipt(
    String claimId,
    String workerId,
    long expectedRevision,
    long consumedRevision,
    String argumentsSha256,
    HarnessOwner owner,
    String sessionId,
    HarnessPermissionMode permissionMode,
    long permissionRevision,
    long consumedAt
) {

    public ApprovalClaimReceipt {
        claimId = ApprovalValidation.requireId(claimId, "claimId");
        workerId = ApprovalValidation.requireId(workerId, "workerId");
        if (expectedRevision < 0 || consumedRevision != expectedRevision + 1 || owner == null
            || permissionMode == null || permissionRevision < 0 || consumedAt <= 0) {
            throw new IllegalArgumentException("Invalid approval claim receipt");
        }
        argumentsSha256 = ApprovalValidation.requireSha256(argumentsSha256);
        sessionId = ApprovalValidation.requireId(sessionId, "sessionId");
    }

    public boolean matches(ClaimApprovalCommand command) {
        return command != null
            && claimId.equals(command.claimId())
            && workerId.equals(command.workerId())
            && expectedRevision == command.expectedRevision()
            && argumentsSha256.equals(command.argumentsSha256())
            && owner.equals(command.owner())
            && sessionId.equals(command.sessionId())
            && permissionMode == command.permissionMode()
            && permissionRevision == command.permissionRevision();
    }
}
