package org.ruoyi.chat.service.coding.harness.approval;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistable source of truth for one exact tool-call approval.
 *
 * <p>{@link #resolve(ResolveApprovalCommand, long)} is deliberately only a control-plane state
 * transition. It has no executor callback and never consumes or runs the tool. A worker must
 * separately call {@link #claimForExecution(ClaimApprovalCommand, long)}, which atomically moves
 * an approved invocation to the one-shot {@link ApprovalState#CONSUMED} state.</p>
 */
public record ToolCallApprovalAggregate(
    int schemaVersion,
    String approvalId,
    String runId,
    String toolCallId,
    String toolName,
    String argumentsSha256,
    HarnessOwner owner,
    String sessionId,
    HarnessPermissionMode permissionMode,
    long permissionRevision,
    ApprovalState state,
    long revision,
    ApprovalDecisionReceipt decisionReceipt,
    ApprovalClaimReceipt claimReceipt,
    ApprovalOutcomeReason outcomeReason,
    long createdAt,
    long expiresAt,
    long updatedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ToolCallApprovalAggregate {
        if (schemaVersion < 1 || owner == null || permissionMode == null || permissionRevision < 0
            || state == null || revision < 0) {
            throw new IllegalArgumentException("Invalid tool approval identity or state");
        }
        approvalId = ApprovalValidation.requireId(approvalId, "approvalId");
        runId = ApprovalValidation.requireId(runId, "runId");
        toolCallId = ApprovalValidation.requireId(toolCallId, "toolCallId");
        toolName = ApprovalValidation.requireId(toolName, "toolName");
        argumentsSha256 = ApprovalValidation.requireSha256(argumentsSha256);
        sessionId = ApprovalValidation.requireId(sessionId, "sessionId");
        if (createdAt <= 0 || expiresAt <= createdAt || updatedAt < createdAt) {
            throw new IllegalArgumentException("Invalid tool approval timestamps");
        }
        validateReceipts(argumentsSha256, owner, sessionId, permissionMode, permissionRevision,
            revision, createdAt, expiresAt, updatedAt, decisionReceipt, claimReceipt);
        validateState(state, revision, decisionReceipt, claimReceipt, outcomeReason);
    }

    public static ToolCallApprovalAggregate create(
        String approvalId,
        String runId,
        String toolCallId,
        String toolName,
        String argumentsSha256,
        HarnessOwner owner,
        String sessionId,
        HarnessPermissionMode permissionMode,
        long permissionRevision,
        long createdAt,
        long expiresAt
    ) {
        return new ToolCallApprovalAggregate(CURRENT_SCHEMA_VERSION, approvalId, runId,
            toolCallId, toolName, argumentsSha256, owner, sessionId, permissionMode,
            permissionRevision, ApprovalState.PENDING, 0, null, null, null,
            createdAt, expiresAt, createdAt);
    }

    public static ToolCallApprovalAggregate create(
        String runId,
        String toolCallId,
        String toolName,
        String argumentsSha256,
        HarnessOwner owner,
        String sessionId,
        HarnessPermissionMode permissionMode,
        long permissionRevision,
        long createdAt,
        long expiresAt
    ) {
        return create(UUID.randomUUID().toString(), runId, toolCallId, toolName,
            argumentsSha256, owner, sessionId, permissionMode, permissionRevision,
            createdAt, expiresAt);
    }

    /**
     * Resolves the human decision only. An APPROVE result remains non-executable until claimed by
     * a worker. Replaying the exact decisionId and payload returns this aggregate unchanged.
     */
    public ToolCallApprovalAggregate resolve(ResolveApprovalCommand command, long now) {
        Objects.requireNonNull(command, "command");
        if (decisionReceipt != null) {
            if (decisionReceipt.decisionId().equals(command.decisionId())) {
                if (decisionReceipt.matches(command)) {
                    return this;
                }
                throw new ApprovalConflictException(
                    "decisionId was already used with a different payload");
            }
            throw new ApprovalConflictException("Tool approval was already resolved");
        }
        requireState(ApprovalState.PENDING, "resolve");
        requireActive(now);
        requireOwnerAndSession(command.owner(), command.sessionId());
        requireRevision(command.expectedRevision());
        requireArguments(command.argumentsSha256());

        long nextRevision = revision + 1;
        ApprovalDecisionReceipt receipt = new ApprovalDecisionReceipt(command.decisionId(),
            command.decision(), command.expectedRevision(), nextRevision,
            command.argumentsSha256(), command.owner(), command.sessionId(), command.note(), now);
        ApprovalState nextState = command.decision() == ApprovalDecision.APPROVE
            ? ApprovalState.APPROVED : ApprovalState.DENIED;
        ApprovalOutcomeReason reason = command.decision() == ApprovalDecision.DENY
            ? ApprovalOutcomeReason.DENIED : null;
        return next(nextState, nextRevision, receipt, null, reason, now);
    }

    /**
     * Worker-only one-shot transition. Successful return authorizes only the exact bound arguments
     * under the unchanged owner/session and permission snapshot recorded by the receipt.
     */
    public ToolCallApprovalAggregate claimForExecution(ClaimApprovalCommand command, long now) {
        Objects.requireNonNull(command, "command");
        if (claimReceipt != null) {
            if (claimReceipt.claimId().equals(command.claimId())) {
                if (claimReceipt.matches(command)) {
                    return this;
                }
                throw new ApprovalConflictException(
                    "claimId was already used with a different payload");
            }
            throw new ApprovalConflictException("Tool approval was already consumed");
        }
        requireState(ApprovalState.APPROVED, "claim for execution");
        requireActive(now);
        requireOwnerAndSession(command.owner(), command.sessionId());
        requireRevision(command.expectedRevision());
        requireArguments(command.argumentsSha256());
        if (permissionMode != command.permissionMode()
            || permissionRevision != command.permissionRevision()) {
            throw new ApprovalPermissionChangedException();
        }

        long nextRevision = revision + 1;
        ApprovalClaimReceipt receipt = new ApprovalClaimReceipt(command.claimId(),
            command.workerId(), command.expectedRevision(), nextRevision,
            command.argumentsSha256(), command.owner(), command.sessionId(),
            command.permissionMode(), command.permissionRevision(), now);
        return next(ApprovalState.CONSUMED, nextRevision, decisionReceipt, receipt, null, now);
    }

    /** Durably marks an unconsumed approval expired once its deadline is reached. */
    public ToolCallApprovalAggregate expire(long now) {
        if (state == ApprovalState.EXPIRED) {
            return this;
        }
        if (state != ApprovalState.PENDING && state != ApprovalState.APPROVED) {
            throw new InvalidApprovalTransitionException(state, "expire");
        }
        if (now < expiresAt) {
            throw new IllegalStateException("Tool approval has not reached its expiry time");
        }
        return next(ApprovalState.EXPIRED, revision + 1, decisionReceipt, null,
            ApprovalOutcomeReason.EXPIRED, now);
    }

    /**
     * Optional durable invalidation hook for a permission update observed before worker claim.
     * A mismatch always fails closed, even when the new mode is more permissive.
     */
    public ToolCallApprovalAggregate invalidateForPermissionChange(
        HarnessPermissionMode currentMode, long currentPermissionRevision, long now) {
        if (currentMode == null || currentPermissionRevision < 0) {
            throw new IllegalArgumentException("Invalid current permission snapshot");
        }
        if (permissionMode == currentMode && permissionRevision == currentPermissionRevision) {
            return this;
        }
        if (state == ApprovalState.EXPIRED
            && outcomeReason == ApprovalOutcomeReason.PERMISSION_CHANGED) {
            return this;
        }
        if (state != ApprovalState.PENDING && state != ApprovalState.APPROVED) {
            throw new InvalidApprovalTransitionException(state,
                "invalidate after a permission change");
        }
        return next(ApprovalState.EXPIRED, revision + 1, decisionReceipt, null,
            ApprovalOutcomeReason.PERMISSION_CHANGED, now);
    }

    public ApprovalSyntheticOutcome syntheticOutcome() {
        if (state != ApprovalState.DENIED && state != ApprovalState.EXPIRED) {
            throw new InvalidApprovalTransitionException(state, "create a synthetic outcome");
        }
        return new ApprovalSyntheticOutcome(toolCallId, toolName, state,
            outcomeReason.code(), outcomeReason.message());
    }

    /** Utility for hashing the exact canonical argument bytes before creating a request. */
    public static String sha256(byte[] canonicalArguments) {
        Objects.requireNonNull(canonicalArguments, "canonicalArguments");
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(canonicalArguments));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private ToolCallApprovalAggregate next(ApprovalState nextState, long nextRevision,
                                            ApprovalDecisionReceipt nextDecisionReceipt,
                                            ApprovalClaimReceipt nextClaimReceipt,
                                            ApprovalOutcomeReason nextOutcomeReason,
                                            long now) {
        if (now < updatedAt) {
            throw new IllegalArgumentException("Tool approval update time must be monotonic");
        }
        return new ToolCallApprovalAggregate(schemaVersion, approvalId, runId, toolCallId,
            toolName, argumentsSha256, owner, sessionId, permissionMode, permissionRevision,
            nextState, nextRevision, nextDecisionReceipt, nextClaimReceipt, nextOutcomeReason,
            createdAt, expiresAt, now);
    }

    private void requireState(ApprovalState expected, String action) {
        if (state != expected) {
            if (state == ApprovalState.EXPIRED) {
                throw new ApprovalExpiredException();
            }
            throw new InvalidApprovalTransitionException(state, action);
        }
    }

    private void requireActive(long now) {
        if (now < updatedAt) {
            throw new IllegalArgumentException("Tool approval update time must be monotonic");
        }
        if (now >= expiresAt) {
            throw new ApprovalExpiredException();
        }
    }

    private void requireRevision(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StaleApprovalException(expectedRevision, revision);
        }
    }

    private void requireOwnerAndSession(HarnessOwner commandOwner, String commandSessionId) {
        if (!owner.equals(commandOwner) || !sessionId.equals(commandSessionId)) {
            throw new ApprovalOwnershipException();
        }
    }

    private void requireArguments(String commandHash) {
        if (!MessageDigest.isEqual(argumentsSha256.getBytes(StandardCharsets.US_ASCII),
            commandHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new ApprovalArgumentsMismatchException();
        }
    }

    private static void validateReceipts(
        String argumentsSha256,
        HarnessOwner owner,
        String sessionId,
        HarnessPermissionMode permissionMode,
        long permissionRevision,
        long revision,
        long createdAt,
        long expiresAt,
        long updatedAt,
        ApprovalDecisionReceipt decisionReceipt,
        ApprovalClaimReceipt claimReceipt
    ) {
        if (decisionReceipt != null) {
            if (!decisionReceipt.argumentsSha256().equals(argumentsSha256)
                || !decisionReceipt.owner().equals(owner)
                || !decisionReceipt.sessionId().equals(sessionId)
                || decisionReceipt.resolvedRevision() > revision
                || decisionReceipt.resolvedAt() < createdAt
                || decisionReceipt.resolvedAt() >= expiresAt
                || decisionReceipt.resolvedAt() > updatedAt) {
                throw new IllegalArgumentException("Decision receipt does not belong to this approval");
            }
        }
        if (claimReceipt != null) {
            if (!claimReceipt.argumentsSha256().equals(argumentsSha256)
                || !claimReceipt.owner().equals(owner)
                || !claimReceipt.sessionId().equals(sessionId)
                || claimReceipt.permissionMode() != permissionMode
                || claimReceipt.permissionRevision() != permissionRevision
                || claimReceipt.consumedRevision() > revision
                || claimReceipt.consumedAt() < createdAt
                || claimReceipt.consumedAt() >= expiresAt
                || claimReceipt.consumedAt() > updatedAt) {
                throw new IllegalArgumentException("Claim receipt does not belong to this approval");
            }
        }
    }

    private static void validateState(ApprovalState state,
                                      long revision,
                                      ApprovalDecisionReceipt decisionReceipt,
                                      ApprovalClaimReceipt claimReceipt,
                                      ApprovalOutcomeReason outcomeReason) {
        switch (state) {
            case PENDING -> requireInvariant(decisionReceipt == null && claimReceipt == null
                && outcomeReason == null && revision == 0,
                "PENDING approval cannot have receipts, outcome, or advanced revision");
            case APPROVED -> requireInvariant(decisionReceipt != null
                && decisionReceipt.decision() == ApprovalDecision.APPROVE
                && decisionReceipt.resolvedRevision() == revision
                && claimReceipt == null && outcomeReason == null,
                "APPROVED approval requires only an approve receipt");
            case DENIED -> requireInvariant(decisionReceipt != null
                && decisionReceipt.decision() == ApprovalDecision.DENY
                && decisionReceipt.resolvedRevision() == revision
                && claimReceipt == null && outcomeReason == ApprovalOutcomeReason.DENIED,
                "DENIED approval requires a deny receipt and reason");
            case EXPIRED -> requireInvariant(claimReceipt == null
                && (decisionReceipt == null
                    || decisionReceipt.decision() == ApprovalDecision.APPROVE)
                && (decisionReceipt == null ? revision == 1
                    : revision == decisionReceipt.resolvedRevision() + 1)
                && (outcomeReason == ApprovalOutcomeReason.EXPIRED
                    || outcomeReason == ApprovalOutcomeReason.PERMISSION_CHANGED),
                "EXPIRED approval has invalid receipts or reason");
            case CONSUMED -> requireInvariant(decisionReceipt != null
                && decisionReceipt.decision() == ApprovalDecision.APPROVE
                && claimReceipt != null
                && claimReceipt.expectedRevision() == decisionReceipt.resolvedRevision()
                && claimReceipt.consumedRevision() == revision
                && outcomeReason == null,
                "CONSUMED approval requires approve and claim receipts");
        }
    }

    private static void requireInvariant(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
