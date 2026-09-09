package org.ruoyi.service.coding.harness.model;

import java.util.UUID;
import java.util.regex.Pattern;

/** Write-ahead marker preventing blind replay of an uncertain mutating or executable tool call. */
public record HarnessToolEffect(
    String effectId,
    String toolCallId,
    String toolName,
    String argumentsSha256,
    boolean replaySafe,
    HarnessToolEffectStatus status,
    long startedAt,
    long settledAt,
    String resultMessageId,
    String error,
    String committedResult,
    HarnessEvent controlEvent,
    boolean controlEventPublished
) {

    private static final String TERMINAL_CLOSURE_PREFIX = "terminal-closure-effect-";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public HarnessToolEffect {
        if (effectId == null || effectId.isBlank() || toolCallId == null || toolCallId.isBlank()
            || toolName == null || toolName.isBlank() || argumentsSha256 == null
            || !SHA256.matcher(argumentsSha256).matches() || status == null || startedAt <= 0
            || settledAt < 0) {
            throw new IllegalArgumentException("Invalid tool effect");
        }
        if (status == HarnessToolEffectStatus.PENDING
            && (settledAt != 0 || resultMessageId != null || error != null
            || committedResult != null || controlEvent != null || controlEventPublished)) {
            throw new IllegalArgumentException("Pending tool effect cannot have a settlement");
        }
        if (status == HarnessToolEffectStatus.COMMITTED
            && (settledAt < startedAt || resultMessageId != null || error != null
            || committedResult == null || committedResult.isBlank())) {
            throw new IllegalArgumentException(
                "Committed tool effect requires a durable result receipt");
        }
        if (status == HarnessToolEffectStatus.SETTLED
            && (settledAt < startedAt || resultMessageId == null || resultMessageId.isBlank()
            || error != null || (committedResult != null && committedResult.isBlank()))) {
            throw new IllegalArgumentException("Settled tool effect requires a result message");
        }
        if (status == HarnessToolEffectStatus.ABANDONED
            && (settledAt < startedAt || error == null || error.isBlank()
            || committedResult != null || controlEvent != null || controlEventPublished)) {
            throw new IllegalArgumentException("Abandoned tool effect requires an error");
        }
        if (controlEventPublished && controlEvent == null) {
            throw new IllegalArgumentException(
                "Published control event marker requires its durable event draft");
        }
        if (controlEvent != null && (committedResult == null || committedResult.isBlank()
            || (status != HarnessToolEffectStatus.COMMITTED
            && status != HarnessToolEffectStatus.SETTLED))) {
            throw new IllegalArgumentException(
                "Control event outbox requires a committed control result");
        }
        if (controlEvent != null && (controlEvent.sequence() != 0
            || !toolCallId.equals(controlEvent.toolCallId()))) {
            throw new IllegalArgumentException(
                "Control event outbox must retain the exact call-bound event draft");
        }
    }

    public static HarnessToolEffect pending(String toolCallId, String toolName,
                                            String argumentsSha256, boolean replaySafe,
                                            long now) {
        return new HarnessToolEffect(UUID.randomUUID().toString(), toolCallId, toolName,
            argumentsSha256, replaySafe, HarnessToolEffectStatus.PENDING, now, 0, null, null,
            null, null, false);
    }

    /** Durable intent used when terminalization, rather than a tool executor, owns the result. */
    public static HarnessToolEffect terminalClosurePending(String toolCallId, String toolName,
                                                           String argumentsSha256, long now) {
        return new HarnessToolEffect(TERMINAL_CLOSURE_PREFIX + UUID.randomUUID(), toolCallId,
            toolName, argumentsSha256, false, HarnessToolEffectStatus.PENDING, now, 0, null,
            null, null, null, false);
    }

    public boolean terminalClosureIntent() {
        return effectId.startsWith(TERMINAL_CLOSURE_PREFIX);
    }

    /** Stable identity for the one public completion signal owned by this durable effect. */
    public String completedEventId() {
        return "tool-effect:" + effectId + ":completed";
    }

    /**
     * The external side effect may already have happened even though no durable receipt exists.
     * Ordinary cancellation/failure cleanup must never erase this evidence.
     */
    public boolean requiresOperatorAdjudication() {
        return status == HarnessToolEffectStatus.PENDING
            && !replaySafe && !terminalClosureIntent();
    }

    public HarnessToolEffect commit(String serializedResult, long now) {
        return commit(serializedResult, null, now);
    }

    public HarnessToolEffect commit(String serializedResult, HarnessEvent event, long now) {
        if (status == HarnessToolEffectStatus.COMMITTED
            && committedResult.equals(serializedResult)
            && java.util.Objects.equals(controlEvent, event)) {
            return this;
        }
        requirePending("commit");
        if (serializedResult == null || serializedResult.isBlank()) {
            throw new IllegalArgumentException("Committed tool result must not be blank");
        }
        return new HarnessToolEffect(effectId, toolCallId, toolName, argumentsSha256,
            replaySafe, HarnessToolEffectStatus.COMMITTED, startedAt, now, null, null,
            serializedResult, event, false);
    }

    /** Exact authoritative receipt semantics shared by live terminal closure and recovery. */
    public boolean matchesCommittedReceipt(boolean toolError, String resultContent) {
        return status == HarnessToolEffectStatus.COMMITTED && !toolError
            && committedResult.equals(resultContent);
    }

    public HarnessToolEffect settle(String messageId, long now) {
        if (status == HarnessToolEffectStatus.SETTLED && resultMessageId.equals(messageId)) {
            return this;
        }
        if (status != HarnessToolEffectStatus.PENDING
            && status != HarnessToolEffectStatus.COMMITTED) {
            throw new IllegalStateException("Cannot settle tool effect in " + status);
        }
        String durableResult = status == HarnessToolEffectStatus.COMMITTED
            ? committedResult : null;
        HarnessEvent durableEvent = status == HarnessToolEffectStatus.COMMITTED
            ? controlEvent : null;
        boolean eventPublished = status == HarnessToolEffectStatus.COMMITTED
            && controlEventPublished;
        return new HarnessToolEffect(effectId, toolCallId, toolName, argumentsSha256,
            replaySafe, HarnessToolEffectStatus.SETTLED, startedAt, now, messageId, null,
            durableResult, durableEvent, eventPublished);
    }

    public boolean hasPendingControlEvent() {
        return controlEvent != null && !controlEventPublished;
    }

    public HarnessToolEffect markControlEventPublished() {
        if (controlEvent == null) {
            throw new IllegalStateException("Tool effect has no durable control event");
        }
        if (controlEventPublished) {
            return this;
        }
        if (status != HarnessToolEffectStatus.COMMITTED
            && status != HarnessToolEffectStatus.SETTLED) {
            throw new IllegalStateException("Cannot publish control event in " + status);
        }
        return new HarnessToolEffect(effectId, toolCallId, toolName, argumentsSha256,
            replaySafe, status, startedAt, settledAt, resultMessageId, error, committedResult,
            controlEvent, true);
    }

    public HarnessToolEffect abandon(String reason, long now) {
        if (status == HarnessToolEffectStatus.ABANDONED && error.equals(reason)) {
            return this;
        }
        requirePending("abandon");
        if (requiresOperatorAdjudication()) {
            throw new IllegalStateException(
                "Cannot abandon an uncertain non-replayable tool effect");
        }
        return new HarnessToolEffect(effectId, toolCallId, toolName, argumentsSha256,
            replaySafe, HarnessToolEffectStatus.ABANDONED, startedAt, now, null, reason, null,
            null, false);
    }

    private void requirePending(String action) {
        if (status != HarnessToolEffectStatus.PENDING) {
            throw new IllegalStateException("Cannot " + action + " tool effect in " + status);
        }
    }
}
