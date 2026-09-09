package org.ruoyi.service.coding.harness.recovery;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.service.coding.harness.event.HarnessModelLifecycleIntegrity;
import org.ruoyi.service.coding.harness.event.HarnessModelLifecycleIntegrityException;
import org.ruoyi.service.coding.harness.event.HarnessEventOutboxService;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessInputKind;
import org.ruoyi.service.coding.harness.model.HarnessModelEffect;
import org.ruoyi.service.coding.harness.model.HarnessModelEffectOutcomeCode;
import org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.model.HarnessUsage;
import org.ruoyi.service.coding.harness.model.ProviderOverflowRecovery;
import org.ruoyi.service.coding.harness.model.ProviderOverflowRecoveryStage;
import org.ruoyi.service.coding.harness.loop.HarnessTranscriptReader;
import org.ruoyi.service.coding.harness.loop.protocol.HarnessToolBatchCloser;
import org.ruoyi.service.coding.harness.loop.protocol.SyntheticToolResultReason;
import org.ruoyi.service.coding.harness.runtime.HarnessRunRequest;
import org.ruoyi.service.coding.harness.runtime.HarnessScheduleResult;
import org.ruoyi.service.coding.harness.runtime.HarnessScheduler;
import org.ruoyi.service.coding.harness.store.HarnessOptimisticLockException;
import org.ruoyi.service.coding.harness.store.HarnessRunScanPage;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Re-enqueues durable work after a single-process restart. Waiting runs remain waiting. A model
 * request whose outcome is not provably present in the immutable message ledger is quarantined,
 * never blindly replayed.
 */
@Slf4j
@Component
public class HarnessStartupRecovery {

    private static final int MESSAGE_PAGE_SIZE = 256;
    private final HarnessStore store;
    private final HarnessScheduler scheduler;
    private final HarnessEventOutboxService eventOutboxService;
    private final HarnessModelLifecycleIntegrity lifecycleIntegrity;
    private final boolean enabled;
    private final int pageSize;
    private final int maxRuns;
    private final int maxScanRecords;
    private final int maxMessagesPerEffect;
    private final HarnessToolBatchCloser toolBatchCloser;
    private final ToolEffectLedgerReconciler toolEffectLedgerReconciler;
    private final AtomicReference<RecoveryLifecycle> lifecycle =
        new AtomicReference<>(RecoveryLifecycle.NEW);
    private volatile HarnessRecoveryReport lastReport;

    @Autowired
    public HarnessStartupRecovery(
        HarnessStore store,
        HarnessScheduler scheduler,
        HarnessEventOutboxService eventOutboxService,
        @Value("${coding.harness.recovery.enabled:true}") boolean enabled,
        @Value("${coding.harness.recovery.page-size:64}") int pageSize,
        @Value("${coding.harness.recovery.max-runs:200}") int maxRuns,
        @Value("${coding.harness.recovery.max-scan-records:100000}") int maxScanRecords,
        @Value("${coding.harness.recovery.max-messages-per-effect:10000}")
        int maxMessagesPerEffect
    ) {
        if (pageSize < 1 || pageSize > 1_000 || maxRuns < 1 || maxScanRecords < maxRuns
            || maxMessagesPerEffect < 1) {
            throw new IllegalArgumentException("Invalid Harness startup recovery limits");
        }
        this.store = store;
        this.scheduler = scheduler;
        this.eventOutboxService = eventOutboxService;
        this.lifecycleIntegrity = new HarnessModelLifecycleIntegrity(store);
        this.enabled = enabled;
        this.pageSize = pageSize;
        this.maxRuns = maxRuns;
        this.maxScanRecords = maxScanRecords;
        this.maxMessagesPerEffect = maxMessagesPerEffect;
        this.toolBatchCloser = new HarnessToolBatchCloser(store,
            new HarnessTranscriptReader(store));
        this.toolEffectLedgerReconciler = new ToolEffectLedgerReconciler(store,
            maxMessagesPerEffect);
    }

    public HarnessStartupRecovery(HarnessStore store, HarnessScheduler scheduler,
                                  boolean enabled, int pageSize, int maxRuns,
                                  int maxMessagesPerEffect) {
        this(store, scheduler, standaloneOutbox(store), enabled, pageSize, maxRuns,
            Math.max(100_000, maxRuns), maxMessagesPerEffect);
    }

    public HarnessStartupRecovery(HarnessStore store, HarnessScheduler scheduler,
                                  boolean enabled, int pageSize, int maxRuns,
                                  int maxScanRecords, int maxMessagesPerEffect) {
        this(store, scheduler, standaloneOutbox(store), enabled, pageSize, maxRuns,
            maxScanRecords, maxMessagesPerEffect);
    }

    private static HarnessEventOutboxService standaloneOutbox(HarnessStore store) {
        return new HarnessEventOutboxService(store,
            new HarnessEventHub(store, Runnable::run, 16));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onApplicationReady() {
        HarnessRecoveryReport report = recover();
        if (!report.idempotentNoop()) {
            log.info("Harness startup recovery scanned={}, scheduled={}, duplicate={}, waiting={}, "
                    + "terminal={}, quarantined={}, truncated={}",
                report.scanned(), report.scheduled(), report.alreadyScheduled(),
                report.waitingSkipped(), report.terminalSkipped(), report.quarantined(),
                report.truncated());
        }
    }

    /** Safe to call more than once; only the first successful invocation performs a scan. */
    public HarnessRecoveryReport recover() {
        if (!enabled) {
            return HarnessRecoveryReport.disabledOrAlreadyRun();
        }
        if (!lifecycle.compareAndSet(RecoveryLifecycle.NEW, RecoveryLifecycle.RUNNING)) {
            HarnessRecoveryReport completed = lastReport;
            return completed == null ? HarnessRecoveryReport.disabledOrAlreadyRun()
                : new HarnessRecoveryReport(completed.scanned(), completed.scheduled(),
                    completed.alreadyScheduled(), completed.waitingSkipped(),
                    completed.terminalSkipped(), completed.quarantined(), completed.truncated(), true);
        }
        try {
            HarnessRecoveryReport report = doRecover();
            lastReport = report;
            lifecycle.set(RecoveryLifecycle.COMPLETED);
            return report;
        } catch (RuntimeException failure) {
            // A retry is safe: the keyed scheduler deduplicates requests already accepted before
            // the failure, while the scan cursor starts again from immutable durable snapshots.
            lifecycle.set(RecoveryLifecycle.NEW);
            throw failure;
        }
    }

    private HarnessRecoveryReport doRecover() {
        MutableReport report = new MutableReport();
        String cursor = null;
        while (report.actionable < maxRuns && report.scanned < maxScanRecords) {
            int remaining = maxScanRecords - report.scanned;
            HarnessRunScanPage page = store.scanRunsForRecovery(cursor,
                Math.min(pageSize, remaining));
            if (page.runs().isEmpty()) {
                report.truncated = page.hasMore();
                break;
            }
            for (HarnessRunState discovered : page.runs()) {
                report.scanned++;
                if (discovered.status() == HarnessRunStatus.QUEUED
                    || discovered.status() == HarnessRunStatus.RUNNING) {
                    report.actionable++;
                }
                recoverRun(discovered, report);
                if (report.actionable >= maxRuns) {
                    break;
                }
            }
            if (!page.hasMore()) {
                break;
            }
            cursor = page.nextCursor();
            if (report.actionable >= maxRuns || report.scanned >= maxScanRecords) {
                report.truncated = true;
            }
        }
        return report.freeze();
    }

    private void recoverRun(HarnessRunState discovered, MutableReport report) {
        try {
            // Validate/repair lifecycle before the first drain. An empty legacy terminal outbox
            // must never let automatic run.* repair overtake its missing model marker.
            discovered = repairModelEffectLifecycleEvent(discovered);
        } catch (HarnessModelLifecycleIntegrityException invalidLifecycle) {
            isolateLifecycleIntegrityFailure(discovered, invalidLifecycle);
            report.quarantined++;
            return;
        }
        discovered = eventOutboxService.drainBestEffort(discovered.owner(), discovered);
        if (!discovered.eventOutbox().isEmpty()) {
            if (discovered.status().isTerminal()) {
                accountTerminalRecovery(discovered, report);
            } else {
                report.alreadyScheduled++;
            }
            return;
        }
        discovered = reconcileControlEventOutbox(discovered);
        discovered = eventOutboxService.drainBestEffort(discovered.owner(), discovered);
        HarnessRunStatus status = discovered.status();
        if (status.isTerminal()) {
            accountTerminalRecovery(discovered, report);
            return;
        }
        // Recovery must not start providers/tools (or synthesize later state) across a durable
        // lifecycle marker that could not yet reach the ledger. Maintenance will retry this FIFO
        // and schedule RUNNING/QUEUED work only after it becomes empty.
        if (!discovered.eventOutbox().isEmpty()) {
            report.alreadyScheduled++;
            return;
        }
        if (status != HarnessRunStatus.QUEUED && status != HarnessRunStatus.RUNNING) {
            report.waitingSkipped++;
            return;
        }

        Optional<HarnessRunState> creationRepaired;
        try {
            creationRepaired = repairCreationState(discovered);
        } catch (RuntimeException repairFailure) {
            log.warn("Unable to repair creation stages for Harness run {}",
                discovered.runId(), repairFailure);
            quarantine(discovered, "Creation recovery failed: "
                + safeMessage(repairFailure), 0);
            report.quarantined++;
            return;
        }
        if (creationRepaired.isEmpty()) {
            report.quarantined++;
            return;
        }
        Optional<HarnessRunState> toolSafe = makeToolEffectsSafe(creationRepaired.get());
        if (toolSafe.isEmpty()) {
            report.quarantined++;
            return;
        }
        HarnessRunState reconciled = toolSafe.get();
        // Cancellation is authoritative only after exact receipts have closed every external
        // effect. An unknown non-replayable effect must remain PENDING for later adjudication;
        // cancellation cannot manufacture a TOOL result or pretend the effect did not happen.
        if (reconciled.cancellationRequested()) {
            honorCancellation(reconciled);
            report.terminalSkipped++;
            return;
        }
        Optional<HarnessRunState> safe = makeModelEffectSafe(reconciled);
        if (safe.isEmpty()) {
            report.quarantined++;
            return;
        }
        HarnessRunState run = safe.get();
        // Model-effect reconciliation can itself stage assistant.completed. If its first ledger
        // append is deferred, do not even enqueue a worker: the maintenance cursor owns draining
        // this FIFO and will schedule the RUNNING/QUEUED snapshot only after it is empty.
        if (!run.eventOutbox().isEmpty()) {
            report.alreadyScheduled++;
            return;
        }
        // A concurrent request may have changed state while an effect ledger was inspected.
        if (run.status() != HarnessRunStatus.QUEUED
            && run.status() != HarnessRunStatus.RUNNING) {
            report.waitingSkipped++;
            return;
        }
        HarnessScheduleResult result = scheduler.schedule(new HarnessRunRequest(
            run.owner(), run.sessionId(), run.runId()));
        if (result == HarnessScheduleResult.SCHEDULED) {
            report.scheduled++;
        } else {
            report.alreadyScheduled++;
        }
    }

    /**
     * Corrupt lifecycle evidence cannot be repaired by guessing. Isolate runnable snapshots
     * without emitting a later state event; terminal/waiting snapshots are already non-runnable.
     * Any invalid lifecycle draft remains a durable barrier and is rejected again by the hub.
     */
    private void isolateLifecycleIntegrityFailure(HarnessRunState initial,
                                                  RuntimeException failure) {
        log.warn("Isolated Harness run {} after model lifecycle validation failed: {}",
            initial.runId(), failure.getClass().getSimpleName());
        HarnessRunState run = reload(initial);
        for (int attempt = 0; attempt < 4; attempt++) {
            HarnessRunState isolated = run;
            long timestamp = now();
            if (run.status() == HarnessRunStatus.QUEUED) {
                isolated = run.transition(HarnessRunStatus.RUNNING, null, timestamp)
                    .transition(HarnessRunStatus.SUSPENDED,
                        "model_lifecycle_integrity_failure", timestamp);
            } else if (run.status() == HarnessRunStatus.RUNNING) {
                isolated = run.transition(HarnessRunStatus.SUSPENDED,
                    "model_lifecycle_integrity_failure", timestamp);
            } else {
                return;
            }
            try {
                store.saveRun(run.owner(), isolated, run.revision());
                return;
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
            }
        }
        log.warn("Unable to persist lifecycle isolation for Harness run {}", initial.runId());
    }

    /**
     * Reconciles exact TOOL receipts and atomically isolates any remaining non-replayable PENDING
     * effect. Its id, arguments hash and status are deliberately retained byte-for-byte.
     */
    private Optional<HarnessRunState> makeToolEffectsSafe(HarnessRunState initial) {
        long timestamp = now();
        try {
            HarnessRunState saved = toolEffectLedgerReconciler.updateAtomically(initial,
                timestamp, reconciled -> UncertainToolEffectGuard.firstFinding(reconciled)
                    .map(finding -> UncertainToolEffectGuard.suspend(reconciled, finding,
                        timestamp))
                    .orElse(reconciled));
            // Exact TOOL evidence may atomically settle an effect and stage tool.completed.
            // Drain that new FIFO head before any provider/tool redispatch. A transient ledger
            // failure deliberately leaves the draft attached; the common barrier below then
            // reports the run as already scheduled instead of crossing the missing signal.
            saved = eventOutboxService.drainBestEffort(saved.owner(), saved);
            Optional<UncertainToolEffectGuard.Finding> finding =
                UncertainToolEffectGuard.firstFinding(saved);
            if (finding.isPresent()) {
                log.warn("Quarantined Harness run {} for reason {} and effect {}",
                    saved.runId(), finding.get().reason().code(),
                    finding.get().effect().effectId());
                return Optional.empty();
            }
            return Optional.of(saved);
        } catch (RuntimeException reconciliationFailure) {
            return isolateToolLedgerFailure(initial, reconciliationFailure);
        }
    }

    private Optional<HarnessRunState> isolateToolLedgerFailure(
        HarnessRunState initial, RuntimeException failure
    ) {
        String reason = UncertainToolEffectReason.LEDGER_RECONCILIATION_UNAVAILABLE.code();
        HarnessRunState run = reload(initial);
        for (int attempt = 0; attempt < 4; attempt++) {
            if (run.status().isTerminal()) {
                return Optional.of(run);
            }
            HarnessRunState isolated = UncertainToolEffectGuard.suspend(run, reason, now());
            try {
                HarnessRunState saved = isolated == run ? run
                    : store.saveRun(run.owner(), isolated, run.revision());
                String effectId = run.toolEffects().values().stream()
                    .filter(effect -> effect.status() == HarnessToolEffectStatus.PENDING
                        || effect.status() == HarnessToolEffectStatus.COMMITTED)
                    .map(HarnessToolEffect::effectId).sorted().findFirst().orElse("none");
                String detail = failure instanceof ToolEffectLedgerReconciliationException exact
                    ? exact.reason().code() : failure.getClass().getSimpleName();
                log.warn("Quarantined Harness run {} for reason {}, detail {}, effect {}",
                    run.runId(), reason, detail, effectId);
                return Optional.empty();
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
            }
        }
        log.warn("Unable to persist Harness run {} isolation for reason {}",
            initial.runId(), reason);
        return Optional.empty();
    }

    /**
     * Drains committed plan-event drafts for every status, including waiting and terminal runs
     * which will not be scheduled again. A crash after append but before the snapshot marker is
     * repaired by matching the immutable event id, never by appending a second event.
     */
    private HarnessRunState reconcileControlEventOutbox(HarnessRunState initial) {
        HarnessRunState run = initial;
        for (int attempt = 0; attempt < 4; attempt++) {
            HarnessRunState next = run;
            for (HarnessToolEffect observed : run.toolEffects().values()) {
                HarnessToolEffect effect = next.toolEffects().get(observed.toolCallId());
                if (effect == null || !effect.hasPendingControlEvent()) {
                    continue;
                }
                HarnessEvent event = effect.controlEvent();
                if (!controlEventExists(run, event)) {
                    store.appendEvent(run.owner(), event);
                }
                next = next.withToolEffect(effect.markControlEventPublished(), now());
            }
            if (next == run) {
                return run;
            }
            try {
                return store.saveRun(run.owner(), next, run.revision());
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
            }
        }
        throw new IllegalStateException("Unable to acknowledge control-event outbox for run "
            + initial.runId());
    }

    private boolean controlEventExists(HarnessRunState run, HarnessEvent expected) {
        long cursor = 0;
        int inspected = 0;
        while (inspected < maxScanRecords) {
            int limit = Math.min(MESSAGE_PAGE_SIZE, maxScanRecords - inspected);
            List<HarnessEvent> page = store.readEvents(run.owner(), run.sessionId(), run.runId(),
                cursor, limit);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessEvent event : page) {
                if (event.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness event scan did not advance during outbox recovery");
                }
                cursor = event.sequence();
                inspected++;
                if (expected.eventId().equals(event.eventId())) {
                    if (!expected.withSequence(event.sequence()).equals(event)) {
                        throw new IllegalStateException(
                            "Control event id was reused for different event content");
                    }
                    return true;
                }
            }
            if (page.size() < limit) {
                return false;
            }
        }
        if (store.readEvents(run.owner(), run.sessionId(), run.runId(), cursor, 1).isEmpty()) {
            return false;
        }
        throw new IllegalStateException("Control event recovery scan exceeded "
            + maxScanRecords + " records");
    }

    /** Repairs snapshots written before model-effect terminal events joined the durable outbox. */
    private HarnessRunState repairModelEffectLifecycleEvent(HarnessRunState initial) {
        HarnessRunState run = initial;
        for (int attempt = 0; attempt < 4; attempt++) {
            HarnessModelEffect effect = run.modelEffect();
            if (effect == null) {
                return run;
            }
            if (run.status().isTerminal()
                && effect.status() == HarnessModelEffectStatus.PENDING) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Terminal run retains a pending model effect");
            }

            HarnessMessage response = effect.status() == HarnessModelEffectStatus.SETTLED
                ? lifecycleIntegrity.requireExactSettledResponse(run, maxMessagesPerEffect) : null;
            Optional<HarnessEvent> startedOutbox = outboxEvent(run, effect.startedEventId());
            Optional<HarnessEvent> startedDurable = lifecycleIntegrity.findEventById(run,
                effect.startedEventId(), maxScanRecords);
            if (startedOutbox.isPresent()) {
                lifecycleIntegrity.validateLifecycleEvent(run, startedOutbox.get(), response);
            }
            if (startedDurable.isPresent()) {
                lifecycleIntegrity.validateLifecycleEvent(run, startedDurable.get(), response);
            }
            requireSameLifecycleLogicalEvent(startedOutbox, startedDurable);

            String terminalEventId = effect.status() == HarnessModelEffectStatus.SETTLED
                ? effect.completedEventId() : effect.abandonedEventId();
            Optional<HarnessEvent> terminalOutbox = effect.status()
                == HarnessModelEffectStatus.PENDING ? Optional.empty()
                : outboxEvent(run, terminalEventId);
            Optional<HarnessEvent> terminalDurable = effect.status()
                == HarnessModelEffectStatus.PENDING ? Optional.empty()
                : lifecycleIntegrity.findEventById(run, terminalEventId, maxScanRecords);
            if (terminalOutbox.isPresent()) {
                lifecycleIntegrity.validateLifecycleEvent(run, terminalOutbox.get(), response);
            }
            if (terminalDurable.isPresent()) {
                lifecycleIntegrity.validateLifecycleEvent(run, terminalDurable.get(), response);
            }
            requireSameLifecycleLogicalEvent(terminalOutbox, terminalDurable);
            if (effect.status() != HarnessModelEffectStatus.PENDING) {
                requireLifecycleOrder(run, startedOutbox, startedDurable,
                    terminalOutbox, terminalDurable);
            }

            boolean missingStarted = startedOutbox.isEmpty() && startedDurable.isEmpty();
            boolean missingTerminal = effect.status() != HarnessModelEffectStatus.PENDING
                && terminalOutbox.isEmpty() && terminalDurable.isEmpty();
            if ((missingStarted || missingTerminal)
                && hasUnrepairableLifecycleSuccessor(run, effect, missingStarted,
                    startedDurable)) {
                throw new HarnessModelLifecycleIntegrityException(
                    "A later durable event already crossed the missing model lifecycle marker");
            }
            if (!missingStarted && !missingTerminal) {
                return run;
            }

            long timestamp = Math.max(now(), Math.max(effect.startedAt(), effect.settledAt()));
            HarnessRunState next = run;
            if (missingStarted) {
                next = next.enqueueEvent(HarnessEvent.draftWithId(effect.startedEventId(),
                    run.sessionId(), run.runId(), "model.turn.started", null, null, null,
                    Map.of("effectId", effect.effectId(), "iteration", effect.iteration()),
                    effect.startedAt()), timestamp);
            }
            if (missingTerminal && effect.status() == HarnessModelEffectStatus.SETTLED) {
                next = next.settleModelEffectWithEvent(effect.effectId(),
                    response.messageId(), response.usage(), false, response.toolCalls().size(),
                    timestamp);
            } else if (missingTerminal) {
                next = next.abandonModelEffectWithEvent(effect.effectId(), effect.error(),
                    recoveredAbandonmentCode(run), timestamp);
            }
            try {
                HarnessRunState saved = store.saveRun(run.owner(), next, run.revision());
                return eventOutboxService.drainBestEffort(saved.owner(), saved);
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
            }
        }
        throw new HarnessModelLifecycleIntegrityException(
            "Model lifecycle repair exhausted its optimistic-lock retry budget");
    }

    private Optional<HarnessEvent> outboxEvent(HarnessRunState run, String eventId) {
        return run.eventOutbox().stream()
            .map(entry -> entry.event())
            .filter(event -> eventId.equals(event.eventId()))
            .findFirst();
    }

    private void requireSameLifecycleLogicalEvent(Optional<HarnessEvent> outbox,
                                                  Optional<HarnessEvent> durable) {
        if (outbox.isEmpty() || durable.isEmpty()) {
            return;
        }
        HarnessEvent expected = outbox.get().withSequence(durable.get().sequence());
        if (!expected.equals(durable.get())) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle event id is bound to conflicting durable content");
        }
    }

    private void requireLifecycleOrder(HarnessRunState run,
                                       Optional<HarnessEvent> startedOutbox,
                                       Optional<HarnessEvent> startedDurable,
                                       Optional<HarnessEvent> terminalOutbox,
                                       Optional<HarnessEvent> terminalDurable) {
        if (terminalDurable.isPresent()) {
            if (startedDurable.isEmpty()
                || startedDurable.get().sequence() >= terminalDurable.get().sequence()) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Model lifecycle terminal marker precedes its started marker");
            }
            return;
        }
        if (terminalOutbox.isPresent() && startedDurable.isEmpty()
            && startedOutbox.isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle terminal outbox marker has no started marker");
        }
        if (startedOutbox.isEmpty() || terminalOutbox.isEmpty()) {
            return;
        }
        int startedIndex = outboxIndex(run, startedOutbox.get().eventId());
        int terminalIndex = outboxIndex(run, terminalOutbox.get().eventId());
        if (startedDurable.isEmpty() && startedIndex >= terminalIndex) {
            throw new HarnessModelLifecycleIntegrityException(
                "Model lifecycle outbox terminal marker precedes its started marker");
        }
    }

    private int outboxIndex(HarnessRunState run, String eventId) {
        for (int index = 0; index < run.eventOutbox().size(); index++) {
            if (eventId.equals(run.eventOutbox().get(index).event().eventId())) {
                return index;
            }
        }
        return -1;
    }

    private boolean hasUnrepairableLifecycleSuccessor(HarnessRunState run,
                                                       HarnessModelEffect effect,
                                                       boolean missingStarted,
                                                       Optional<HarnessEvent> startedDurable) {
        int startedOutboxIndex = outboxIndex(run, effect.startedEventId());
        for (int index = 0; index < run.eventOutbox().size(); index++) {
            var entry = run.eventOutbox().get(index);
            HarnessEvent event = entry.event();
            boolean afterStarted = missingStarted
                ? event.timestamp() >= effect.startedAt()
                : startedDurable.isPresent()
                    || (startedOutboxIndex >= 0 && index > startedOutboxIndex);
            if (isUnrepairableSuccessor(event, effect, missingStarted, afterStarted)) {
                return true;
            }
        }
        long cursor = 0;
        int inspected = 0;
        while (inspected < maxScanRecords) {
            int limit = Math.min(MESSAGE_PAGE_SIZE, maxScanRecords - inspected);
            List<HarnessEvent> page = store.readEvents(run.owner(), run.sessionId(), run.runId(),
                cursor, limit);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessEvent event : page) {
                if (event.sequence() <= cursor) {
                    throw new HarnessModelLifecycleIntegrityException(
                        "Harness event scan did not advance during lifecycle ordering repair");
                }
                cursor = event.sequence();
                inspected++;
                boolean afterStarted = missingStarted
                    ? event.timestamp() >= effect.startedAt()
                    : startedDurable.map(started ->
                        event.sequence() > started.sequence()).orElseGet(() ->
                            event.timestamp() >= effect.startedAt());
                if (isUnrepairableSuccessor(event, effect, missingStarted, afterStarted)) {
                    return true;
                }
            }
            if (page.size() < limit) {
                return false;
            }
        }
        if (!store.readEvents(run.owner(), run.sessionId(), run.runId(), cursor, 1).isEmpty()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Lifecycle ordering scan exceeded its bounded limit");
        }
        return false;
    }

    private boolean isUnrepairableSuccessor(HarnessEvent event, HarnessModelEffect effect,
                                            boolean missingStarted, boolean afterStarted) {
        if (effect.startedEventId().equals(event.eventId())
            || effect.completedEventId().equals(event.eventId())
            || effect.abandonedEventId().equals(event.eventId())) {
            return false;
        }
        if (!afterStarted) {
            return false;
        }
        boolean currentProgress = !missingStarted
            && effect.effectId().equals(event.data().get("effectId"))
            && ("assistant.text.delta".equals(event.type())
                || "assistant.thinking.delta".equals(event.type())
                || "assistant.tool.complete".equals(event.type()));
        return !currentProgress;
    }

    private HarnessModelEffectOutcomeCode recoveredAbandonmentCode(HarnessRunState run) {
        if (run.status() == HarnessRunStatus.CANCELLED || run.cancellationRequested()) {
            return HarnessModelEffectOutcomeCode.RUN_CANCELLED;
        }
        if (run.providerOverflowRecovery() != null) {
            return HarnessModelEffectOutcomeCode.PROVIDER_CONTEXT_OVERFLOW;
        }
        if (run.status() == HarnessRunStatus.FAILED) {
            return HarnessModelEffectOutcomeCode.RUN_FAILED;
        }
        if (run.status() == HarnessRunStatus.SUSPENDED) {
            return HarnessModelEffectOutcomeCode.RUN_SUSPENDED;
        }
        return HarnessModelEffectOutcomeCode.RECOVERY_UNCERTAIN;
    }

    /**
     * Run creation spans session pointer, run snapshot and initial USER ledger writes. A crash can
     * leave the QUEUED snapshot as the only durable stage. Repair the other two stages before the
     * scheduler is allowed to expose the run to the model.
     */
    private Optional<HarnessRunState> repairCreationState(HarnessRunState run) {
        HarnessSessionState session = store.findSession(run.owner(), run.sessionId()).orElse(null);
        if (session == null) {
            return quarantine(run, "Run has no durable owning session", 0);
        }
        for (int attempt = 0; attempt < 4 && !run.runId().equals(session.activeRunId()); attempt++) {
            if (session.activeRunId() != null) {
                HarnessRunState active = store.findRun(run.owner(), run.sessionId(),
                    session.activeRunId()).orElse(null);
                if (active != null && !active.status().isTerminal()) {
                    return quarantine(run,
                        "Session points to a different non-terminal run during recovery", 0);
                }
            }
            try {
                session = store.saveSession(run.owner(),
                    session.withActiveRun(run.runId(), now()), session.revision());
            } catch (HarnessOptimisticLockException conflict) {
                session = store.findSession(run.owner(), run.sessionId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Harness session disappeared during startup recovery"));
            }
        }
        if (!run.runId().equals(session.activeRunId())) {
            return quarantine(run, "Unable to repair the active run pointer", 0);
        }

        String inputId = "run-create:" + run.runId();
        if (!messageWithInputIdExists(run, inputId)) {
            store.appendMessage(run.owner(), HarnessMessage.draft(run.sessionId(), run.runId(),
                HarnessMessageRole.USER, run.originalRequirement(), null, List.of(), null, null,
                false, null, Map.of("kind", HarnessInputKind.INITIAL.name(),
                    "inputId", inputId), now()));
        }
        return Optional.of(run);
    }

    private void repairTerminalEvent(HarnessRunState run) {
        eventOutboxService.ensureTerminalEventAfterBarrier(run.owner(), run);
    }

    private void accountTerminalRecovery(HarnessRunState run, MutableReport report) {
        try {
            repairTerminalEvent(run);
            report.terminalSkipped++;
        } catch (HarnessModelLifecycleIntegrityException invalidTerminal) {
            isolateLifecycleIntegrityFailure(run, invalidTerminal);
            report.quarantined++;
        }
    }

    private boolean messageWithInputIdExists(HarnessRunState run, String inputId) {
        long cursor = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(run.owner(), run.sessionId(), cursor,
                MESSAGE_PAGE_SIZE);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness message scan did not advance its cursor");
                }
                cursor = message.sequence();
                if (inputId.equals(message.metadata().get("inputId"))) {
                    return true;
                }
            }
            if (page.size() < MESSAGE_PAGE_SIZE) {
                return false;
            }
        }
    }

    private String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
            ? failure.getClass().getSimpleName() : message;
    }

    private Optional<HarnessRunState> makeModelEffectSafe(HarnessRunState initial) {
        HarnessRunState run = initial;
        for (int attempt = 0; attempt < 4; attempt++) {
            HarnessModelEffect effect = run.modelEffect();
            ProviderOverflowRecovery recovery = run.providerOverflowRecovery();
            if (recovery != null) {
                return makeProviderOverflowRecoverySafe(run, effect, recovery, attempt);
            }
            if (effect == null) {
                return Optional.of(run);
            }
            if (effect.iteration() != run.iteration()) {
                return quarantine(run, "Model effect iteration does not match the durable run", attempt);
            }

            if (effect.status() == HarnessModelEffectStatus.SETTLED) {
                // The aggregate constructor guarantees a settled effect has a response message id.
                // Its raw message may be older than the current compaction checkpoint, so do not
                // mistake an intentionally compacted response for an unknown provider outcome.
                return Optional.of(run);
            }
            if (effect.status() == HarnessModelEffectStatus.ABANDONED) {
                return quarantine(run, "Abandoned model effect cannot be resumed automatically", attempt);
            }
            Optional<HarnessMessage> response = findPersistedResponse(run, effect);
            if (response.isEmpty()) {
                return quarantine(run,
                    "Process restarted with an unsettled provider request", attempt);
            }

            long settledAt = now();
            // Native snapshots account usage in the same durable revision that settles the
            // provider effect. Legacy snapshots deliberately leave this to the processor's
            // one-time ledger fold, otherwise the recovered response would be counted twice.
            HarnessRunState next = run.settleModelEffectWithEvent(effect.effectId(),
                response.get().messageId(), response.get().usage(), run.usageInitialized(),
                response.get().toolCalls().size(), settledAt);
            try {
                HarnessRunState saved = store.saveRun(run.owner(), next, run.revision());
                return Optional.of(eventOutboxService.drainBestEffort(saved.owner(), saved));
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
                if (!isRecoverable(run)) {
                    return Optional.of(run);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Resumes only exact, durable overflow boundaries. A COMPACTION_REQUIRED or RETRY_READY state
     * is safe to schedule because no provider effect is pending. A RETRY_IN_FLIGHT state is never
     * replayed: it is settled from its exact ledger receipt or exhausted and isolated.
     */
    private Optional<HarnessRunState> makeProviderOverflowRecoverySafe(
        HarnessRunState run, HarnessModelEffect effect, ProviderOverflowRecovery recovery,
        int attempt) {
        if (recovery.stage() == ProviderOverflowRecoveryStage.COMPACTION_REQUIRED) {
            if (effect != null && effect.status() == HarnessModelEffectStatus.ABANDONED
                && recovery.matchesFailedEffect(effect)
                && run.iteration() == recovery.failedIteration()
                && recovery.checkpointBeforeMatches(run.contextCheckpoint())
                && !run.compactionControl().emergencyAttempted(recovery.recoveryId())) {
                return Optional.of(run);
            }
            return quarantine(run,
                "Compaction-required overflow recovery does not match its durable boundary",
                attempt);
        }
        if (recovery.stage() == ProviderOverflowRecoveryStage.RETRY_READY) {
            if (effect != null && effect.status() == HarnessModelEffectStatus.ABANDONED
                && recovery.matchesFailedEffect(effect)
                && run.iteration() == recovery.failedIteration()
                && recovery.checkpointMatches(run.contextCheckpoint())
                && run.compactionControl().emergencyAttempted(recovery.recoveryId())) {
                return Optional.of(run);
            }
            return quarantine(run,
                "Retry-ready overflow recovery does not match its durable checkpoint", attempt);
        }
        if (recovery.stage() == ProviderOverflowRecoveryStage.EXHAUSTED) {
            return quarantine(run, recovery.error(), attempt);
        }
        if (recovery.stage() != ProviderOverflowRecoveryStage.RETRY_IN_FLIGHT
            || effect == null || effect.status() != HarnessModelEffectStatus.PENDING
            || !recovery.matchesRetryEffect(effect)
            || run.iteration() != recovery.retryIteration()
            || !recovery.checkpointMatches(run.contextCheckpoint())
            || !run.compactionControl().emergencyAttempted(recovery.recoveryId())) {
            return quarantine(run,
                "In-flight overflow retry does not match its durable effect", attempt);
        }
        Optional<HarnessMessage> response = findPersistedResponse(run, effect);
        if (response.isEmpty()) {
            return quarantine(run,
                "Process restarted with the single overflow retry in flight and no durable response",
                attempt);
        }
        long settledAt = now();
        HarnessRunState next = run.settleModelEffectWithEvent(effect.effectId(),
                response.get().messageId(), response.get().usage(), run.usageInitialized(),
                response.get().toolCalls().size(), settledAt)
            .withProviderOverflowRecovery(null, settledAt);
        try {
            HarnessRunState saved = store.saveRun(run.owner(), next, run.revision());
            return Optional.of(eventOutboxService.drainBestEffort(saved.owner(), saved));
        } catch (HarnessOptimisticLockException conflict) {
            HarnessRunState current = reload(run);
            if (!isRecoverable(current)) {
                return Optional.of(current);
            }
            return makeModelEffectSafe(current);
        }
    }

    private HarnessRunState honorCancellation(HarnessRunState initial) {
        HarnessRunState run = initial;
        for (int attempt = 0; attempt < 4; attempt++) {
            if (run.status().isTerminal()) {
                return run;
            }
            long timestamp = now();
            Optional<UncertainToolEffectGuard.Finding> uncertain =
                UncertainToolEffectGuard.firstFinding(run);
            if (uncertain.isPresent()) {
                HarnessRunState suspended = UncertainToolEffectGuard.suspend(
                    run.requestCancellation(timestamp), uncertain.get(), timestamp);
                suspended = enqueueRecoveryStateEvent(suspended, "run.suspended",
                    Map.of("reason", uncertain.get().reason().code(),
                        "effectId", uncertain.get().effect().effectId()), timestamp);
                try {
                    HarnessRunState saved = store.saveRun(run.owner(), suspended, run.revision());
                    return eventOutboxService.drainBestEffort(saved.owner(), saved);
                } catch (HarnessOptimisticLockException conflict) {
                    run = reload(run);
                    continue;
                }
            }
            HarnessToolBatchCloser.Closure closure = toolBatchCloser.close(run,
                SyntheticToolResultReason.CANCEL, timestamp);
            HarnessRunState next = closure.run();
            for (HarnessToolEffect effect : next.toolEffects().values()) {
                if (effect.status() == HarnessToolEffectStatus.PENDING) {
                    next = next.withToolEffect(
                        effect.abandon("Cancellation recovered after restart", timestamp),
                        timestamp);
                }
            }
            HarnessModelEffect modelEffect = next.modelEffect();
            if (modelEffect != null && modelEffect.status() == HarnessModelEffectStatus.PENDING) {
                next = next.abandonModelEffectWithEvent(modelEffect.effectId(),
                    "Cancellation recovered after restart",
                    HarnessModelEffectOutcomeCode.RUN_CANCELLED, timestamp);
            }
            ProviderOverflowRecovery recovery = next.providerOverflowRecovery();
            if (recovery != null
                && recovery.stage() != ProviderOverflowRecoveryStage.EXHAUSTED) {
                next = next.withProviderOverflowRecovery(recovery.exhausted(
                    "Cancellation recovered after restart", timestamp), timestamp);
            }
            next = next.transition(HarnessRunStatus.CANCELLED, null, timestamp);
            next = enqueueRecoveryStateEvent(next, "run.cancelled",
                Map.of("reason", "startup_recovery_cancellation"), timestamp);
            try {
                HarnessRunState saved = store.saveRun(run.owner(), next, next.revision());
                return eventOutboxService.drainBestEffort(saved.owner(), saved);
            } catch (HarnessOptimisticLockException conflict) {
                run = reload(run);
            }
        }
        throw new IllegalStateException(
            "Unable to persist durable cancellation for run " + initial.runId());
    }

    private Optional<HarnessRunState> quarantine(HarnessRunState run, String reason, int attempt) {
        Optional<UncertainToolEffectGuard.Finding> uncertain =
            UncertainToolEffectGuard.firstFinding(run);
        if (uncertain.isPresent()) {
            long timestamp = now();
            HarnessRunState suspended = UncertainToolEffectGuard.suspend(run,
                uncertain.get(), timestamp);
            suspended = enqueueRecoveryStateEvent(suspended, "run.suspended",
                Map.of("reason", uncertain.get().reason().code(),
                    "effectId", uncertain.get().effect().effectId()), timestamp);
            try {
                HarnessRunState saved = store.saveRun(run.owner(), suspended, run.revision());
                eventOutboxService.drainBestEffort(saved.owner(), saved);
                log.warn("Quarantined Harness run {} for reason {} and effect {}",
                    run.runId(), uncertain.get().reason().code(),
                    uncertain.get().effect().effectId());
                return Optional.empty();
            } catch (HarnessOptimisticLockException conflict) {
                if (attempt >= 3) {
                    return Optional.empty();
                }
                HarnessRunState current = reload(run);
                if (!isRecoverable(current)) {
                    return Optional.of(current);
                }
                return quarantine(current, reason, attempt + 1);
            }
        }
        long timestamp = now();
        HarnessRunState next = run;
        HarnessModelEffect effect = run.modelEffect();
        if (effect != null && effect.status() == HarnessModelEffectStatus.PENDING) {
            next = next.abandonModelEffectWithEvent(effect.effectId(), reason,
                HarnessModelEffectOutcomeCode.RECOVERY_UNCERTAIN, timestamp);
        }
        ProviderOverflowRecovery recovery = next.providerOverflowRecovery();
        if (recovery != null && recovery.stage() != ProviderOverflowRecoveryStage.EXHAUSTED) {
            next = next.withProviderOverflowRecovery(recovery.exhausted(reason, timestamp),
                timestamp);
        }
        HarnessRunStatus target = run.status() == HarnessRunStatus.RUNNING
            ? HarnessRunStatus.SUSPENDED : HarnessRunStatus.FAILED;
        next = next.transition(target, reason, timestamp);
        next = enqueueRecoveryStateEvent(next,
            target == HarnessRunStatus.SUSPENDED ? "run.suspended" : "run.failed",
            Map.of("reason", "startup_recovery_quarantine"), timestamp);
        try {
            HarnessRunState saved = store.saveRun(run.owner(), next, run.revision());
            eventOutboxService.drainBestEffort(saved.owner(), saved);
            log.warn("Quarantined Harness run {} during startup recovery: {}", run.runId(), reason);
            return Optional.empty();
        } catch (HarnessOptimisticLockException conflict) {
            if (attempt >= 3) {
                return Optional.empty();
            }
            HarnessRunState current = reload(run);
            if (!isRecoverable(current)) {
                return Optional.of(current);
            }
            return makeModelEffectSafe(current);
        }
    }

    private Optional<HarnessMessage> findPersistedResponse(HarnessRunState run,
                                                           HarnessModelEffect effect) {
        long cursor = run.contextCheckpoint().compactedThroughMessageSequence();
        int inspected = 0;
        while (inspected < maxMessagesPerEffect) {
            int limit = Math.min(MESSAGE_PAGE_SIZE, maxMessagesPerEffect - inspected);
            List<HarnessMessage> page = store.readMessages(run.owner(), run.sessionId(), cursor, limit);
            if (page.isEmpty()) {
                return Optional.empty();
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException("Harness message scan did not advance its cursor");
                }
                cursor = message.sequence();
                inspected++;
                if (message.role() == HarnessMessageRole.ASSISTANT
                    && run.runId().equals(message.runId())
                    && effect.effectId().equals(String.valueOf(message.metadata().get("effectId")))) {
                    return Optional.of(message);
                }
            }
            if (page.size() < limit) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private HarnessRunState reload(HarnessRunState run) {
        return store.findRun(run.owner(), run.sessionId(), run.runId())
            .orElseThrow(() -> new IllegalStateException(
                "Harness run disappeared during startup recovery: " + run.runId()));
    }

    /** Queues a stable state signal behind any lifecycle marker in the same recovery snapshot. */
    private HarnessRunState enqueueRecoveryStateEvent(HarnessRunState run, String type,
                                                       Map<String, Object> extra,
                                                       long timestamp) {
        Map<String, Object> data = new LinkedHashMap<>(extra);
        data.put("status", run.status().name());
        data.put("revision", run.revision() + 1);
        HarnessEvent event = HarnessEvent.draftWithId(
            "run-state:" + run.runId() + ":" + type + ":" + (run.revision() + 1),
            run.sessionId(), run.runId(), type, null, null, null, data, timestamp);
        return run.enqueueEvent(event, timestamp);
    }

    private boolean isRecoverable(HarnessRunState run) {
        return run.status() == HarnessRunStatus.QUEUED
            || run.status() == HarnessRunStatus.RUNNING;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private enum RecoveryLifecycle {
        NEW,
        RUNNING,
        COMPLETED
    }

    private static final class MutableReport {
        private int scanned;
        private int actionable;
        private int scheduled;
        private int alreadyScheduled;
        private int waitingSkipped;
        private int terminalSkipped;
        private int quarantined;
        private boolean truncated;

        private HarnessRecoveryReport freeze() {
            return new HarnessRecoveryReport(scanned, scheduled, alreadyScheduled,
                waitingSkipped, terminalSkipped, quarantined, truncated, false);
        }
    }
}
