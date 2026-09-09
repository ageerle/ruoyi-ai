package org.ruoyi.service.coding.harness.loop.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.service.coding.harness.approval.ToolCallApprovalAggregate;
import org.ruoyi.service.coding.harness.loop.HarnessTranscriptReader;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.store.HarnessStore;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Closes a durable assistant tool-call batch before a run becomes terminal. Provider protocols
 * require one TOOL result for every advertised call even when execution was cancelled or denied.
 */
public final class HarnessToolBatchCloser {

    private final HarnessStore store;
    private final HarnessTranscriptReader transcriptReader;
    private final ToolProtocolValidator validator = new ToolProtocolValidator();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public HarnessToolBatchCloser(HarnessStore store, HarnessTranscriptReader transcriptReader) {
        this.store = Objects.requireNonNull(store, "store");
        this.transcriptReader = Objects.requireNonNull(transcriptReader, "transcriptReader");
    }

    /**
     * Crash-idempotent terminal closure. New result slots first receive a durable non-replayable
     * intent and budget reservation; only then is the TOOL ledger appended. COMMITTED calls replay
     * their exact success receipt, while a restart can settle an already-appended terminal result
     * by effect id without charging its call twice.
     */
    public Closure close(HarnessRunState run, SyntheticToolResultReason reason, long timestamp) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(reason, "reason");
        List<HarnessMessage> transcript = transcriptReader.readAfter(run.owner(), run.sessionId(),
            run.contextCheckpoint().toSequence());
        ToolProtocolValidation validation = validator.validate(transcript);
        if (!validation.violations().isEmpty()) {
            throw new ToolProtocolException(validation);
        }
        HarnessRunState projected = reconcilePersistedResults(run, transcript, timestamp);
        ToolBatchProjection open = validation.lastUnclosedBatch().orElse(null);
        if (open == null) {
            return new Closure(projected, 0);
        }
        if (!run.runId().equals(open.assistantMessage().runId())) {
            throw new IllegalStateException(
                "The open tool batch belongs to a different run in this session");
        }
        long firstSequence = transcript.isEmpty() ? 1
            : Math.addExact(transcript.get(transcript.size() - 1).sequence(), 1);
        Set<String> previouslyCounted = new HashSet<>(projected.toolEffects().keySet());
        projected.toolApprovals().values().forEach(approval ->
            previouslyCounted.add(approval.toolCallId()));
        int remainingToolCalls = Math.max(0,
            projected.budget().maxToolCalls() - projected.toolCallCount());
        int newlyCounted = 0;
        boolean createdIntent = false;
        HarnessRunState prepared = projected;
        for (HarnessToolCall call : open.missingCalls()) {
            HarnessToolEffect effect = prepared.toolEffects().get(call.toolCallId());
            if (effect == null) {
                effect = HarnessToolEffect.terminalClosurePending(call.toolCallId(),
                    call.toolName(), argumentsSha256(call), timestamp);
                prepared = prepared.withToolEffect(effect, timestamp);
                createdIntent = true;
            } else {
                requireEffectIdentity(effect, call);
                requireClosableEffect(effect);
            }
            if (!previouslyCounted.contains(call.toolCallId())
                && newlyCounted < remainingToolCalls) {
                newlyCounted++;
            }
        }
        if (newlyCounted > 0) {
            prepared = prepared.withCounters(prepared.iteration(),
                Math.addExact(prepared.toolCallCount(), newlyCounted), timestamp);
        }
        if (createdIntent) {
            projected = store.saveRun(run.owner(), prepared, run.revision());
        } else {
            projected = prepared;
        }

        int closed = 0;
        for (int index = 0; index < open.missingCalls().size(); index++) {
            HarnessToolCall call = open.missingCalls().get(index);
            long sequence = Math.addExact(firstSequence, index);
            HarnessToolEffect effect = projected.toolEffects().get(call.toolCallId());
            HarnessMessage candidate;
            if (effect.status() == HarnessToolEffectStatus.COMMITTED) {
                candidate = open.synthesizeCommitted(call.toolCallId(), effect.committedResult(),
                    effect.effectId(), sequence, timestamp);
            } else {
                requireClosableEffect(effect);
                String malformed = malformedArguments(call.arguments());
                candidate = malformed == null
                    ? open.synthesizeMissing(call.toolCallId(), reason, sequence, timestamp)
                    : open.synthesizeInvalid(call.toolCallId(), malformed, sequence, timestamp);
            }
            candidate = bindTerminalEffect(candidate, effect);
            HarnessMessage stored = store.appendMessage(run.owner(), candidate);
            if (effect.status() == HarnessToolEffectStatus.PENDING
                || effect.status() == HarnessToolEffectStatus.COMMITTED) {
                projected = projected.withToolEffect(effect.settle(stored.messageId(), timestamp),
                    timestamp);
            }
            closed++;
        }
        return new Closure(projected, closed);
    }

    private HarnessRunState reconcilePersistedResults(HarnessRunState run,
                                                      List<HarnessMessage> transcript,
                                                      long timestamp) {
        HarnessRunState projected = run;
        for (HarnessToolEffect effect : run.toolEffects().values()) {
            if (effect.status() != HarnessToolEffectStatus.PENDING
                && effect.status() != HarnessToolEffectStatus.COMMITTED) {
                continue;
            }
            HarnessMessage result = transcript.stream()
                .filter(message -> message.role() == HarnessMessageRole.TOOL)
                .filter(message -> run.runId().equals(message.runId()))
                .filter(message -> effect.toolCallId().equals(message.toolCallId()))
                .filter(message -> effect.toolName().equals(message.toolName()))
                .filter(message -> effect.effectId().equals(message.metadata().get("effectId")))
                .findFirst().orElse(null);
            if (result == null) {
                continue;
            }
            if (effect.status() == HarnessToolEffectStatus.COMMITTED
                && !effect.matchesCommittedReceipt(result.toolError(), result.content())) {
                throw new IllegalStateException(
                    "Durable tool result does not match its committed control receipt");
            }
            projected = projected.withToolEffect(effect.settle(result.messageId(), timestamp),
                timestamp);
        }
        return projected;
    }

    private void requireEffectIdentity(HarnessToolEffect effect, HarnessToolCall call) {
        String argumentsSha256 = argumentsSha256(call);
        if (!effect.toolName().equals(call.toolName())
            || !effect.argumentsSha256().equals(argumentsSha256)) {
            throw new IllegalStateException(
                "Tool effect does not match the open assistant call");
        }
    }

    private String argumentsSha256(HarnessToolCall call) {
        return ToolCallApprovalAggregate.sha256(
            call.arguments().getBytes(StandardCharsets.UTF_8));
    }

    private void requireClosableEffect(HarnessToolEffect effect) {
        if (effect.status() == HarnessToolEffectStatus.COMMITTED) {
            return;
        }
        if (effect.status() != HarnessToolEffectStatus.PENDING
            || (!effect.replaySafe() && !effect.terminalClosureIntent())) {
            throw new IllegalStateException("Cannot claim terminal non-execution for uncertain "
                + "tool effect " + effect.effectId());
        }
    }

    private HarnessMessage bindTerminalEffect(HarnessMessage result, HarnessToolEffect effect) {
        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        metadata.put("effectId", effect.effectId());
        metadata.put("terminalClosure", true);
        return new HarnessMessage(result.schemaVersion(), result.messageId(), result.sessionId(),
            result.runId(), result.sequence(), result.role(), result.content(), result.thinking(),
            result.toolCalls(), result.toolCallId(), result.toolName(), result.toolError(),
            result.usage(), metadata, result.timestamp());
    }

    private String malformedArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "Tool arguments are not valid JSON";
        }
        try {
            JsonNode value = objectMapper.readTree(arguments);
            return value != null && value.isObject() ? null
                : "Tool arguments must be exactly one JSON object";
        } catch (JsonProcessingException invalid) {
            return "Tool arguments are not valid JSON";
        }
    }

    public record Closure(HarnessRunState run, int closedCount) {
        public Closure {
            Objects.requireNonNull(run, "run");
            if (closedCount < 0) {
                throw new IllegalArgumentException("closedCount cannot be negative");
            }
        }
    }
}
