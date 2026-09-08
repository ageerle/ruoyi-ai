package org.ruoyi.service.coding.harness.model;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Write-ahead marker for an LLM request. A recovered PENDING effect is uncertain and must not be
 * replayed blindly; recovery either links an already-persisted response or suspends for review.
 */
public record HarnessModelEffect(
    String effectId,
    int iteration,
    String requestSha256,
    HarnessModelEffectStatus status,
    long startedAt,
    long settledAt,
    String responseMessageId,
    String error
) {

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String EVENT_ID_PREFIX = "model-effect:";

    public HarnessModelEffect {
        if (effectId == null || effectId.isBlank() || iteration < 0
            || requestSha256 == null || !SHA256.matcher(requestSha256).matches()
            || status == null || startedAt <= 0 || settledAt < 0) {
            throw new IllegalArgumentException("Invalid model effect");
        }
        if (status == HarnessModelEffectStatus.PENDING
            && (settledAt != 0 || responseMessageId != null || error != null)) {
            throw new IllegalArgumentException("Pending model effect cannot have a settlement");
        }
        if (status == HarnessModelEffectStatus.SETTLED
            && (settledAt < startedAt || responseMessageId == null || responseMessageId.isBlank()
            || error != null)) {
            throw new IllegalArgumentException("Settled model effect requires a response message");
        }
        if (status == HarnessModelEffectStatus.ABANDONED
            && (settledAt < startedAt || error == null || error.isBlank())) {
            throw new IllegalArgumentException("Abandoned model effect requires an error");
        }
    }

    public static HarnessModelEffect pending(int iteration, String requestSha256, long now) {
        return new HarnessModelEffect(UUID.randomUUID().toString(), iteration, requestSha256,
            HarnessModelEffectStatus.PENDING, now, 0, null, null);
    }

    public HarnessModelEffect settle(String messageId, long now) {
        if (status == HarnessModelEffectStatus.SETTLED
            && responseMessageId.equals(messageId)) {
            return this;
        }
        requirePending("settle");
        return new HarnessModelEffect(effectId, iteration, requestSha256,
            HarnessModelEffectStatus.SETTLED, startedAt, now, messageId, null);
    }

    public HarnessModelEffect abandon(String reason, long now) {
        if (status == HarnessModelEffectStatus.ABANDONED && error.equals(reason)) {
            return this;
        }
        requirePending("abandon");
        return new HarnessModelEffect(effectId, iteration, requestSha256,
            HarnessModelEffectStatus.ABANDONED, startedAt, now, null, reason);
    }

    public String startedEventId() {
        return EVENT_ID_PREFIX + effectId + ":started";
    }

    public String completedEventId() {
        return EVENT_ID_PREFIX + effectId + ":completed";
    }

    public String abandonedEventId() {
        return EVENT_ID_PREFIX + effectId + ":abandoned";
    }

    private void requirePending(String action) {
        if (status != HarnessModelEffectStatus.PENDING) {
            throw new IllegalStateException("Cannot " + action + " model effect in " + status);
        }
    }
}
