package org.ruoyi.service.coding.harness.event;

import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessModelEffect;
import org.ruoyi.service.coding.harness.model.HarnessModelEffectOutcomeCode;
import org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.store.HarnessStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Fail-closed validation for the durable provider-turn lifecycle. */
public final class HarnessModelLifecycleIntegrity {

    private static final int PAGE_SIZE = 256;
    private static final Pattern LEGACY_RANDOM_EVENT_ID = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern CANONICAL_UNSIGNED_DECIMAL = Pattern.compile(
        "(?:0|[1-9][0-9]{0,18})");

    private final HarnessStore store;

    public HarnessModelLifecycleIntegrity(HarnessStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /** A terminal run is publishable only after both started and its terminal marker are durable. */
    public boolean isComplete(HarnessRunState run, int maxEvents, int maxMessages) {
        HarnessModelEffect effect = run.modelEffect();
        if (effect == null) {
            return true;
        }
        if (effect.status() == HarnessModelEffectStatus.PENDING) {
            return false;
        }
        HarnessMessage response = effect.status() == HarnessModelEffectStatus.SETTLED
            ? requireExactSettledResponse(run, maxMessages) : null;
        Optional<HarnessEvent> started = findEventById(run, effect.startedEventId(), maxEvents);
        Optional<HarnessEvent> terminal = findEventById(run, terminalEventId(effect), maxEvents);
        started.ifPresent(event -> validateLifecycleEvent(run, event, response));
        terminal.ifPresent(event -> validateLifecycleEvent(run, event, response));
        if (terminal.isPresent() && started.isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle terminal marker exists before its started marker");
        }
        if (started.isPresent() && terminal.isPresent()
            && started.get().sequence() >= terminal.get().sequence()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle terminal marker does not follow its started marker");
        }
        return started.isPresent() && terminal.isPresent();
    }

    /**
     * Resolves the model-lifecycle boundary of an already durable historical terminal run.
     *
     * <p>Current stable markers always win and remain strict. A fallback is allowed only when
     * both stable ids are absent and the ledger contains the exact UUID-v4 event shape emitted by
     * the pre-outbox implementation. This is read compatibility: new terminal publication still
     * calls {@link #isComplete(HarnessRunState, int, int)} and therefore requires stable markers.
     */
    public Optional<HarnessEvent> requireHistoricalTerminalMarker(HarnessRunState run,
                                                                   int maxEvents,
                                                                   int maxMessages) {
        Objects.requireNonNull(run, "run");
        HarnessModelEffect effect = run.modelEffect();
        if (effect == null) {
            return Optional.empty();
        }
        if (effect.status() == HarnessModelEffectStatus.PENDING) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal run retains a pending model effect");
        }
        HarnessMessage response = effect.status() == HarnessModelEffectStatus.SETTLED
            ? requireExactSettledResponse(run, maxMessages) : null;
        Optional<HarnessEvent> stableStarted = findEventById(run, effect.startedEventId(),
            maxEvents);
        Optional<HarnessEvent> stableTerminal = findEventById(run, terminalEventId(effect),
            maxEvents);
        stableStarted.ifPresent(event -> validateLifecycleEvent(run, event, response));
        stableTerminal.ifPresent(event -> validateLifecycleEvent(run, event, response));
        if (stableStarted.isPresent() != stableTerminal.isPresent()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Historical run has a partial stable model lifecycle");
        }
        if (stableStarted.isPresent()) {
            if (stableStarted.orElseThrow().sequence()
                >= stableTerminal.orElseThrow().sequence()) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Model lifecycle terminal marker does not follow its started marker");
            }
            return stableTerminal;
        }
        return Optional.of(requireLegacyTerminalMarker(run, effect, response, maxEvents));
    }

    /** Validates an outbox/ledger lifecycle event against the current effect and exact receipt. */
    public void validateLifecycleEvent(HarnessRunState run, HarnessEvent event,
                                       HarnessMessage settledResponse) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(event, "event");
        HarnessModelEffect effect = run.modelEffect();
        if (effect == null) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle event has no durable effect");
        }
        requireEnvelope(run, event, effect);
        if (effect.startedEventId().equals(event.eventId())) {
            validateStarted(event, effect);
            return;
        }
        if (effect.status() == HarnessModelEffectStatus.SETTLED
            && effect.completedEventId().equals(event.eventId())) {
            HarnessMessage response = Objects.requireNonNull(settledResponse,
                "settledResponse");
            if (!effect.responseMessageId().equals(response.messageId())) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Completed lifecycle event is not bound to the settled response");
            }
            Map<String, Object> expected = Map.of("effectId", effect.effectId(),
                "messageId", response.messageId(), "toolCallCount", response.toolCalls().size());
            if (!"assistant.completed".equals(event.type())
                || event.timestamp() != effect.settledAt() || !expected.equals(event.data())) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Completed lifecycle event id is bound to different logical content");
            }
            return;
        }
        if (effect.status() == HarnessModelEffectStatus.ABANDONED
            && effect.abandonedEventId().equals(event.eventId())) {
            Object code = event.data().get("code");
            boolean validCode = code instanceof String value;
            if (validCode) {
                try {
                    HarnessModelEffectOutcomeCode.valueOf((String) code);
                } catch (IllegalArgumentException invalid) {
                    validCode = false;
                }
            }
            if (!"model.turn.abandoned".equals(event.type())
                || event.timestamp() != effect.settledAt() || !validCode
                || !Map.of("effectId", effect.effectId(), "outcome", "ABANDONED",
                    "code", code).equals(event.data())) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Abandoned lifecycle event id is bound to different logical content");
            }
            return;
        }
        throw new HarnessModelLifecycleIntegrityException(
            "Event id is not valid for the durable model effect");
    }

    public boolean isLifecycleEvent(HarnessRunState run, HarnessEvent event) {
        HarnessModelEffect effect = run.modelEffect();
        return effect != null && (effect.startedEventId().equals(event.eventId())
            || effect.completedEventId().equals(event.eventId())
            || effect.abandonedEventId().equals(event.eventId()));
    }

    /** Scans from sequence zero and rejects missing, duplicate, or conflicting response identity. */
    public HarnessMessage requireExactSettledResponse(HarnessRunState run, int maxMessages) {
        HarnessModelEffect effect = run.modelEffect();
        if (effect == null || effect.status() != HarnessModelEffectStatus.SETTLED) {
            throw new IllegalArgumentException("A settled model effect is required");
        }
        requirePositiveBound(maxMessages);
        long cursor = 0;
        int inspected = 0;
        HarnessMessage exact = null;
        while (inspected < maxMessages) {
            int limit = Math.min(PAGE_SIZE, maxMessages - inspected);
            List<HarnessMessage> page = store.readMessages(run.owner(), run.sessionId(), cursor,
                limit);
            if (page.isEmpty()) {
                break;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Harness message scan did not advance during lifecycle validation");
                }
                cursor = message.sequence();
                inspected++;
                boolean expectedMessage = effect.responseMessageId().equals(message.messageId());
                boolean expectedEffect = effect.effectId().equals(
                    message.metadata().get("effectId"));
                if (!expectedMessage && !expectedEffect) {
                    continue;
                }
                boolean candidate = expectedMessage && expectedEffect
                    && message.role() == HarnessMessageRole.ASSISTANT
                    && run.runId().equals(message.runId());
                if (!candidate || exact != null) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Settled model effect has conflicting assistant response identity");
                }
                exact = message;
            }
            if (page.size() < limit) {
                break;
            }
        }
        if (inspected == maxMessages
            && !store.readMessages(run.owner(), run.sessionId(), cursor, 1).isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model response scan exceeded its bounded limit");
        }
        if (exact == null) {
            throw new HarnessModelLifecycleIntegrityException(
                "Settled model effect has no exact durable assistant response");
        }
        return exact;
    }

    public Optional<HarnessEvent> findEventById(HarnessRunState run, String eventId,
                                                int maxEvents) {
        requirePositiveBound(maxEvents);
        long cursor = 0;
        int inspected = 0;
        HarnessEvent found = null;
        while (inspected < maxEvents) {
            int limit = Math.min(PAGE_SIZE, maxEvents - inspected);
            List<HarnessEvent> page = store.readEvents(run.owner(), run.sessionId(), run.runId(),
                cursor, limit);
            if (page.isEmpty()) {
                break;
            }
            for (HarnessEvent event : page) {
                if (event.sequence() <= cursor) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Harness event scan did not advance during lifecycle validation");
                }
                cursor = event.sequence();
                inspected++;
                if (!eventId.equals(event.eventId())) {
                    continue;
                }
                if (found != null) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Duplicate model lifecycle event id: "
                        + eventId);
                }
                found = event;
            }
            if (page.size() < limit) {
                break;
            }
        }
        if (inspected == maxEvents
            && !store.readEvents(run.owner(), run.sessionId(), run.runId(), cursor, 1).isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle event scan exceeded its bounded limit");
        }
        return Optional.ofNullable(found);
    }

    private String terminalEventId(HarnessModelEffect effect) {
        return effect.status() == HarnessModelEffectStatus.SETTLED
            ? effect.completedEventId() : effect.abandonedEventId();
    }

    private HarnessEvent requireLegacyTerminalMarker(HarnessRunState run,
                                                      HarnessModelEffect effect,
                                                      HarnessMessage response,
                                                      int maxEvents) {
        requirePositiveBound(maxEvents);
        HarnessEvent started = null;
        HarnessEvent terminal = null;
        long cursor = 0;
        int inspected = 0;
        while (inspected < maxEvents) {
            int limit = Math.min(PAGE_SIZE, maxEvents - inspected);
            List<HarnessEvent> page = store.readEvents(run.owner(), run.sessionId(), run.runId(),
                cursor, limit);
            if (page.isEmpty()) {
                break;
            }
            for (HarnessEvent event : page) {
                if (event.sequence() <= cursor) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Harness event scan did not advance during legacy lifecycle validation");
                }
                cursor = event.sequence();
                inspected++;
                if (!effect.effectId().equals(event.data().get("effectId"))) {
                    continue;
                }
                if ("model.turn.started".equals(event.type())) {
                    if (started != null) {
                        throw new HarnessModelLifecycleIntegrityException(
                            "Legacy model lifecycle has duplicate started events");
                    }
                    started = event;
                } else if ("assistant.completed".equals(event.type())
                    || "model.turn.abandoned".equals(event.type())) {
                    if (terminal != null) {
                        throw new HarnessModelLifecycleIntegrityException(
                            "Legacy model lifecycle has duplicate terminal events");
                    }
                    terminal = event;
                }
            }
            if (page.size() < limit) {
                break;
            }
        }
        if (inspected == maxEvents
            && !store.readEvents(run.owner(), run.sessionId(), run.runId(), cursor, 1).isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Legacy model lifecycle event scan exceeded its bounded limit");
        }
        validateLegacyStarted(run, effect, started);
        if (effect.status() == HarnessModelEffectStatus.SETTLED) {
            validateLegacyCompleted(run, effect, response, terminal);
            if (started.sequence() >= terminal.sequence()) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Legacy model lifecycle terminal event does not follow its started event");
            }
            return terminal;
        }
        if (run.status()
            != org.ruoyi.service.coding.harness.model.HarnessRunStatus.CANCELLED
            || terminal != null) {
            throw new HarnessModelLifecycleIntegrityException(
                "Abandoned model effect has no valid historical lifecycle boundary");
        }
        // Schema-v5 cancellation settled a pending effect in state but emitted only the started
        // event before the run.cancelled boundary. The exact started event is the last available
        // provider boundary for that immutable legacy format.
        return started;
    }

    private void validateLegacyStarted(HarnessRunState run, HarnessModelEffect effect,
                                       HarnessEvent event) {
        if (!isLegacyEnvelope(run, event) || event == null
            || !"model.turn.started".equals(event.type())
            || event.timestamp() < effect.startedAt()
            || !event.data().keySet().equals(
                Set.of("effectId", "iteration", "revision", "status"))
            || !effect.effectId().equals(event.data().get("effectId"))
            || !integralEquals(event.data().get("iteration"), effect.iteration())
            || !isIntegralBetween(event.data().get("revision"), 0, run.revision())
            || !"RUNNING".equals(event.data().get("status"))) {
            throw new HarnessModelLifecycleIntegrityException(
                "Legacy model lifecycle started event is invalid");
        }
    }

    private void validateLegacyCompleted(HarnessRunState run, HarnessModelEffect effect,
                                         HarnessMessage response, HarnessEvent event) {
        if (!isLegacyEnvelope(run, event) || event == null
            || !"assistant.completed".equals(event.type())
            || event.timestamp() < effect.settledAt()
            || !event.data().keySet().equals(
                Set.of("effectId", "messageId", "toolCallCount"))
            || !effect.effectId().equals(event.data().get("effectId"))
            || !effect.responseMessageId().equals(event.data().get("messageId"))
            || response == null
            || !response.messageId().equals(event.data().get("messageId"))
            || !integralEquals(event.data().get("toolCallCount"),
                response.toolCalls().size())) {
            throw new HarnessModelLifecycleIntegrityException(
                "Legacy model lifecycle completed event is invalid");
        }
    }

    private boolean isLegacyEnvelope(HarnessRunState run, HarnessEvent event) {
        return event != null
            && event.schemaVersion() == HarnessEvent.CURRENT_SCHEMA_VERSION
            && LEGACY_RANDOM_EVENT_ID.matcher(event.eventId()).matches()
            && run.sessionId().equals(event.sessionId())
            && run.runId().equals(event.runId())
            && event.sequence() > 0
            && event.stepId() == null
            && event.toolCallId() == null
            && event.approvalId() == null;
    }

    private boolean integralEquals(Object value, long expected) {
        Long actual = canonicalUnsignedLong(value);
        return actual != null && actual == expected;
    }

    private boolean isIntegralBetween(Object value, long minimum, long maximum) {
        Long actual = canonicalUnsignedLong(value);
        return actual != null && actual >= minimum && actual <= maximum;
    }

    private Long canonicalUnsignedLong(Object value) {
        if (value instanceof Byte || value instanceof Short
            || value instanceof Integer || value instanceof Long) {
            long parsed = ((Number) value).longValue();
            return parsed >= 0 ? parsed : null;
        }
        if (!(value instanceof String text)
            || !CANONICAL_UNSIGNED_DECIMAL.matcher(text).matches()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException overflow) {
            return null;
        }
    }

    private void requireEnvelope(HarnessRunState run, HarnessEvent event,
                                 HarnessModelEffect effect) {
        if (event.schemaVersion() != HarnessEvent.CURRENT_SCHEMA_VERSION
            || !run.sessionId().equals(event.sessionId()) || !run.runId().equals(event.runId())
            || event.stepId() != null || event.toolCallId() != null
            || event.approvalId() != null
            || !(effect.startedEventId().equals(event.eventId())
            || effect.completedEventId().equals(event.eventId())
            || effect.abandonedEventId().equals(event.eventId()))) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle event id is bound to a different envelope");
        }
    }

    private void validateStarted(HarnessEvent event, HarnessModelEffect effect) {
        Map<String, Object> data = event.data();
        boolean basic = "model.turn.started".equals(event.type())
            && event.timestamp() == effect.startedAt()
            && effect.effectId().equals(data.get("effectId"))
            && Objects.equals(effect.iteration(), data.get("iteration"));
        boolean basePayload = data.size() == 2;
        boolean overflowPayload = data.size() == 4
            && Boolean.TRUE.equals(data.get("overflowRetry"))
            && data.get("overflowRecoveryId") instanceof String recoveryId
            && !recoveryId.isBlank();
        if (!basic || (!basePayload && !overflowPayload)) {
            throw new HarnessModelLifecycleIntegrityException(
                "Started lifecycle event id is bound to different logical content");
        }
    }

    private void requirePositiveBound(int bound) {
        if (bound < 1) {
            throw new IllegalArgumentException("Lifecycle scan bound must be positive");
        }
    }
}
