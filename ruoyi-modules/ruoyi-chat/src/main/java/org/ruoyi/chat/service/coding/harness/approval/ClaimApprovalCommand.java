package org.ruoyi.chat.service.coding.harness.approval;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

/** Worker-only one-shot claim that binds execution to the current security snapshot. */
public record ClaimApprovalCommand(
    String claimId,
    String workerId,
    long expectedRevision,
    String argumentsSha256,
    HarnessOwner owner,
    String sessionId,
    HarnessPermissionMode permissionMode,
    long permissionRevision
) {

    public ClaimApprovalCommand {
        claimId = ApprovalValidation.requireId(claimId, "claimId");
        workerId = ApprovalValidation.requireId(workerId, "workerId");
        if (expectedRevision < 0 || owner == null || permissionMode == null
            || permissionRevision < 0) {
            throw new IllegalArgumentException("Invalid approval claim command");
        }
        argumentsSha256 = ApprovalValidation.requireSha256(argumentsSha256);
        sessionId = ApprovalValidation.requireId(sessionId, "sessionId");
    }
}
