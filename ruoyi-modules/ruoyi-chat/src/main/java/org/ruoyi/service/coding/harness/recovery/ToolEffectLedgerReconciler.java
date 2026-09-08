package org.ruoyi.service.coding.harness.recovery;

import org.ruoyi.service.coding.harness.approval.ToolCallApprovalAggregate;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.store.HarnessMessageLedgerConflictException;
import org.ruoyi.service.coding.harness.store.HarnessOptimisticLockException;
import org.ruoyi.service.coding.harness.store.HarnessStore;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Reconciles tool write-ahead intents only from an exact immutable-ledger receipt. Identity is
 * established by effect id, run id, call id and tool name, then bound back to the originating
 * ASSISTANT call by the SHA-256 of its arguments. Tool output text is never used as identity.
 */
public final class ToolEffectLedgerReconciler {

    private static final int PAGE_SIZE = 256;
    private static final int DEFAULT_MAX_MESSAGES = 1_000_000;
    private static final int MAX_ATOMIC_ATTEMPTS = 8;

    private final HarnessStore store;
    private final int maxMessages;

    public ToolEffectLedgerReconciler(HarnessStore store) {
        this(store, DEFAULT_MAX_MESSAGES);
    }

    public ToolEffectLedgerReconciler(HarnessStore store, int maxMessages) {
        this.store = Objects.requireNonNull(store, "store");
        if (maxMessages < 1) {
            throw new IllegalArgumentException("Tool-effect ledger scan limit must be positive");
        }
        this.maxMessages = maxMessages;
    }

    /**
     * Reconciles and applies the caller's safety projection with a run-revision/message-tail CAS.
     * A receipt appended after the scan therefore forces a complete retry instead of allowing a
     * stale PENDING decision to overwrite the newly provable result.
     */
    public HarnessRunState updateAtomically(HarnessRunState initial, long now,
                                            UnaryOperator<HarnessRunState> update) {
        Objects.requireNonNull(initial, "initial");
        Objects.requireNonNull(update, "update");
        HarnessRunState current = initial;
        for (int attempt = 0; attempt < MAX_ATOMIC_ATTEMPTS; attempt++) {
            Observation observation = observe(current, now);
            HarnessRunState candidate = Objects.requireNonNull(update.apply(observation.run()),
                "Tool-effect reconciliation update returned null");
            requireSameRun(current, candidate);
            if (candidate == current) {
                return current;
            }
            try {
                return observation.ledgerObserved()
                    ? store.saveRunIfMessageLedgerUnchanged(current.owner(), candidate,
                        current.revision(), observation.highWatermark())
                    : store.saveRun(current.owner(), candidate, current.revision());
            } catch (HarnessMessageLedgerConflictException | HarnessOptimisticLockException
                     conflict) {
                current = store.findRun(current.owner(), current.sessionId(), current.runId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Harness run disappeared during tool-effect reconciliation", conflict));
            }
        }
        throw new ToolEffectLedgerReconciliationException(
            ToolEffectLedgerFailureReason.CONCURRENT_APPEND_RETRY_EXHAUSTED);
    }

    private Observation observe(HarnessRunState run, long now) {
        Objects.requireNonNull(run, "run");
        Map<String, HarnessToolEffect> candidates = new LinkedHashMap<>();
        Map<String, Integer> effectIdCounts = new HashMap<>();
        for (HarnessToolEffect effect : run.toolEffects().values()) {
            if (effect.status() != HarnessToolEffectStatus.PENDING
                && effect.status() != HarnessToolEffectStatus.COMMITTED) {
                continue;
            }
            candidates.put(effect.toolCallId(), effect);
            effectIdCounts.merge(effect.effectId(), 1, Integer::sum);
        }
        if (candidates.isEmpty()) {
            return new Observation(run, 0, false);
        }

        Map<String, AssistantIdentity> assistants = new HashMap<>();
        Map<String, ReceiptIdentity> receipts = new HashMap<>();
        long cursor = 0;
        int inspected = 0;
        while (inspected < maxMessages) {
            int limit = Math.min(PAGE_SIZE, maxMessages - inspected);
            List<HarnessMessage> page = store.readMessages(run.owner(), run.sessionId(), cursor,
                limit);
            if (page.isEmpty()) {
                break;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new ToolEffectLedgerReconciliationException(
                        ToolEffectLedgerFailureReason.NON_MONOTONIC_SEQUENCE);
                }
                cursor = message.sequence();
                inspected++;
                observeAssistant(run, candidates, assistants, message);
                observeReceipt(run, candidates, receipts, message);
            }
            if (page.size() < limit) {
                break;
            }
        }
        if (inspected == maxMessages
            && !store.readMessages(run.owner(), run.sessionId(), cursor, 1).isEmpty()) {
            throw new ToolEffectLedgerReconciliationException(
                ToolEffectLedgerFailureReason.SCAN_LIMIT_EXCEEDED);
        }

        HarnessRunState projected = run;
        for (HarnessToolEffect effect : candidates.values()) {
            if (effectIdCounts.getOrDefault(effect.effectId(), 0) != 1) {
                continue;
            }
            AssistantIdentity assistant = assistants.get(effect.toolCallId());
            ReceiptIdentity receipt = receipts.get(effect.effectId());
            boolean exactEvidence = assistant != null && receipt != null && assistant.exact()
                && receipt.exact() && assistant.count() == 1 && receipt.count() == 1
                && assistant.sequence() < receipt.sequence();
            if (effect.status() == HarnessToolEffectStatus.COMMITTED && receipt != null
                && !exactEvidence) {
                throw new ToolEffectLedgerReconciliationException(
                    ToolEffectLedgerFailureReason.COMMITTED_RECEIPT_MISMATCH);
            }
            if (!exactEvidence) {
                continue;
            }
            projected = projected.settleToolEffectWithEvent(effect.toolCallId(),
                receipt.message(), now);
        }
        return new Observation(projected, cursor, true);
    }

    private void requireSameRun(HarnessRunState source, HarnessRunState candidate) {
        if (!source.owner().equals(candidate.owner())
            || !source.sessionId().equals(candidate.sessionId())
            || !source.runId().equals(candidate.runId())
            || source.revision() != candidate.revision()) {
            throw new IllegalArgumentException(
                "Tool-effect reconciliation cannot replace run identity or revision");
        }
    }

    private void observeAssistant(HarnessRunState run,
                                  Map<String, HarnessToolEffect> candidates,
                                  Map<String, AssistantIdentity> assistants,
                                  HarnessMessage message) {
        if (message.role() != HarnessMessageRole.ASSISTANT
            || !run.runId().equals(message.runId())) {
            return;
        }
        for (HarnessToolCall call : message.toolCalls()) {
            HarnessToolEffect effect = candidates.get(call.toolCallId());
            if (effect == null) {
                continue;
            }
            boolean exact = effect.toolName().equals(call.toolName())
                && effect.argumentsSha256().equals(ToolCallApprovalAggregate.sha256(
                    Objects.toString(call.arguments(), "").getBytes(StandardCharsets.UTF_8)));
            AssistantIdentity prior = assistants.get(call.toolCallId());
            assistants.put(call.toolCallId(), prior == null
                ? new AssistantIdentity(exact, 1, message.sequence())
                : new AssistantIdentity(prior.exact() && exact, prior.count() + 1,
                    Math.min(prior.sequence(), message.sequence())));
        }
    }

    private void observeReceipt(HarnessRunState run,
                                Map<String, HarnessToolEffect> candidates,
                                Map<String, ReceiptIdentity> receipts,
                                HarnessMessage message) {
        Object rawEffectId = message.metadata().get("effectId");
        if (!(rawEffectId instanceof String effectId)) {
            return;
        }
        HarnessToolEffect effect = candidates.values().stream()
            .filter(candidate -> candidate.effectId().equals(effectId))
            .findFirst().orElse(null);
        if (effect == null) {
            return;
        }
        boolean exact = message.role() == HarnessMessageRole.TOOL
            && run.runId().equals(message.runId())
            && effect.toolCallId().equals(message.toolCallId())
            && effect.toolName().equals(message.toolName())
            && (effect.status() != HarnessToolEffectStatus.COMMITTED
                || effect.matchesCommittedReceipt(message.toolError(), message.content()));
        ReceiptIdentity prior = receipts.get(effectId);
        receipts.put(effectId, prior == null
            ? new ReceiptIdentity(exact, 1, message, message.sequence())
            : new ReceiptIdentity(prior.exact() && exact, prior.count() + 1,
                prior.message(), Math.min(prior.sequence(), message.sequence())));
    }

    private record AssistantIdentity(boolean exact, int count, long sequence) {
    }

    private record ReceiptIdentity(boolean exact, int count, HarnessMessage message,
                                   long sequence) {
    }

    private record Observation(HarnessRunState run, long highWatermark,
                               boolean ledgerObserved) {
    }
}
