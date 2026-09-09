package org.ruoyi.service.coding.harness.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Durable identity and progress for exactly one emergency-compaction/provider-retry sequence.
 *
 * <p>The aggregate deliberately retains both the failed and retry effect identities. Recovery may
 * continue only when the current model effect matches the identity expected by the current stage;
 * an unrelated or partially-written effect is never inferred to be safe.</p>
 */
public record ProviderOverflowRecovery(
    int schemaVersion,
    String recoveryId,
    ProviderOverflowRecoveryStage stage,
    String failedEffectId,
    int failedIteration,
    String failedRequestSha256,
    String checkpointBeforeId,
    long checkpointBeforeSequence,
    String retryCheckpointId,
    long retryCheckpointSequence,
    String retryEffectId,
    int retryIteration,
    String retryRequestSha256,
    String error,
    long createdAt,
    long updatedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public ProviderOverflowRecovery {
        checkpointBeforeId = normalize(checkpointBeforeId);
        retryCheckpointId = normalize(retryCheckpointId);
        retryEffectId = normalize(retryEffectId);
        retryRequestSha256 = normalize(retryRequestSha256);
        error = normalize(error);
        if (schemaVersion < 1 || recoveryId == null || recoveryId.isBlank() || stage == null
            || failedEffectId == null || failedEffectId.isBlank() || failedIteration < 1
            || failedRequestSha256 == null || !SHA256.matcher(failedRequestSha256).matches()
            || checkpointBeforeSequence < 0 || retryCheckpointSequence < 0
            || retryIteration < 0 || createdAt <= 0 || updatedAt < createdAt) {
            throw new IllegalArgumentException("Invalid provider overflow recovery");
        }
        boolean hasRetry = retryEffectId != null || retryIteration != 0
            || retryRequestSha256 != null;
        if (stage == ProviderOverflowRecoveryStage.COMPACTION_REQUIRED &&
            (retryCheckpointId != null || retryCheckpointSequence != 0 || hasRetry || error != null)) {
            throw new IllegalArgumentException("Compaction-required recovery has retry state");
        }
        if (stage == ProviderOverflowRecoveryStage.RETRY_READY) {
            if (retryCheckpointId == null
                || retryCheckpointSequence <= checkpointBeforeSequence || hasRetry || error != null) {
                throw new IllegalArgumentException("Retry-ready recovery requires a newer checkpoint");
            }
        }
        if (stage == ProviderOverflowRecoveryStage.RETRY_IN_FLIGHT) {
            if (retryCheckpointId == null
                || retryCheckpointSequence <= checkpointBeforeSequence || retryEffectId == null
                || retryIteration <= failedIteration || retryRequestSha256 == null
                || !SHA256.matcher(retryRequestSha256).matches() || error != null) {
                throw new IllegalArgumentException("Retry-in-flight recovery is incomplete");
            }
        }
        if (stage == ProviderOverflowRecoveryStage.EXHAUSTED && error == null) {
            throw new IllegalArgumentException("Exhausted recovery requires an error");
        }
    }

    public static ProviderOverflowRecovery compactionRequired(
        String recoveryId, HarnessModelEffect failedEffect,
        HarnessContextCheckpoint checkpoint, long now) {
        Objects.requireNonNull(failedEffect, "failedEffect");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (failedEffect.status() != HarnessModelEffectStatus.PENDING) {
            throw new IllegalArgumentException("Provider overflow must close a pending effect");
        }
        return new ProviderOverflowRecovery(CURRENT_SCHEMA_VERSION, recoveryId,
            ProviderOverflowRecoveryStage.COMPACTION_REQUIRED, failedEffect.effectId(),
            failedEffect.iteration(), failedEffect.requestSha256(), checkpoint.checkpointId(),
            checkpoint.toSequence(), null, 0, null, 0, null, null, now, now);
    }

    public ProviderOverflowRecovery retryReady(HarnessContextCheckpoint checkpoint, long now) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        requireStage(ProviderOverflowRecoveryStage.COMPACTION_REQUIRED, "mark retry ready");
        return new ProviderOverflowRecovery(CURRENT_SCHEMA_VERSION, recoveryId,
            ProviderOverflowRecoveryStage.RETRY_READY, failedEffectId, failedIteration,
            failedRequestSha256, checkpointBeforeId, checkpointBeforeSequence,
            checkpoint.checkpointId(), checkpoint.toSequence(), null, 0, null, null,
            createdAt, now);
    }

    public ProviderOverflowRecovery retryInFlight(HarnessModelEffect retryEffect, long now) {
        Objects.requireNonNull(retryEffect, "retryEffect");
        requireStage(ProviderOverflowRecoveryStage.RETRY_READY, "begin provider retry");
        if (retryEffect.status() != HarnessModelEffectStatus.PENDING) {
            throw new IllegalArgumentException("Provider retry effect must be pending");
        }
        return new ProviderOverflowRecovery(CURRENT_SCHEMA_VERSION, recoveryId,
            ProviderOverflowRecoveryStage.RETRY_IN_FLIGHT, failedEffectId, failedIteration,
            failedRequestSha256, checkpointBeforeId, checkpointBeforeSequence,
            retryCheckpointId, retryCheckpointSequence, retryEffect.effectId(),
            retryEffect.iteration(), retryEffect.requestSha256(), null, createdAt, now);
    }

    public ProviderOverflowRecovery exhausted(String reason, long now) {
        if (stage == ProviderOverflowRecoveryStage.EXHAUSTED) {
            return this;
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Provider overflow exhaustion reason is required");
        }
        return new ProviderOverflowRecovery(CURRENT_SCHEMA_VERSION, recoveryId,
            ProviderOverflowRecoveryStage.EXHAUSTED, failedEffectId, failedIteration,
            failedRequestSha256, checkpointBeforeId, checkpointBeforeSequence,
            retryCheckpointId, retryCheckpointSequence, retryEffectId, retryIteration,
            retryRequestSha256, reason, createdAt, now);
    }

    public boolean matchesFailedEffect(HarnessModelEffect effect) {
        return effect != null && failedEffectId.equals(effect.effectId())
            && failedIteration == effect.iteration()
            && failedRequestSha256.equals(effect.requestSha256());
    }

    public boolean matchesRetryEffect(HarnessModelEffect effect) {
        return stage == ProviderOverflowRecoveryStage.RETRY_IN_FLIGHT && effect != null
            && retryEffectId.equals(effect.effectId()) && retryIteration == effect.iteration()
            && retryRequestSha256.equals(effect.requestSha256());
    }

    public boolean checkpointMatches(HarnessContextCheckpoint checkpoint) {
        return checkpoint != null && retryCheckpointSequence == checkpoint.toSequence()
            && Objects.equals(retryCheckpointId, checkpoint.checkpointId());
    }

    public boolean checkpointBeforeMatches(HarnessContextCheckpoint checkpoint) {
        return checkpoint != null && checkpointBeforeSequence == checkpoint.toSequence()
            && Objects.equals(checkpointBeforeId, checkpoint.checkpointId());
    }

    private void requireStage(ProviderOverflowRecoveryStage expected, String action) {
        if (stage != expected) {
            throw new IllegalStateException("Cannot " + action + " in " + stage);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
