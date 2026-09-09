package org.ruoyi.service.coding.harness.app;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.coding.CodingWorkspaceService;
import org.ruoyi.service.coding.harness.approval.ApprovalDecision;
import org.ruoyi.service.coding.harness.approval.ApprovalState;
import org.ruoyi.service.coding.harness.approval.ResolveApprovalCommand;
import org.ruoyi.service.coding.harness.approval.ToolCallApprovalAggregate;
import org.ruoyi.service.coding.harness.artifact.ArtifactRef;
import org.ruoyi.service.coding.harness.artifact.HarnessArtifactRepository;
import org.ruoyi.service.coding.harness.context.CompactionControl;
import org.ruoyi.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.service.coding.harness.event.HarnessEventOutboxService;
import org.ruoyi.service.coding.harness.loop.HarnessTranscriptReader;
import org.ruoyi.service.coding.harness.loop.protocol.HarnessToolBatchCloser;
import org.ruoyi.service.coding.harness.loop.protocol.SyntheticToolResultReason;
import org.ruoyi.service.coding.harness.loop.protocol.ToolProtocolValidation;
import org.ruoyi.service.coding.harness.loop.protocol.ToolProtocolValidator;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessApproval;
import org.ruoyi.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.service.coding.harness.model.HarnessApprovalStatus;
import org.ruoyi.service.coding.harness.model.HarnessBudget;
import org.ruoyi.service.coding.harness.model.HarnessInputKind;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessModelRoute;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessPermissionMode;
import org.ruoyi.service.coding.harness.model.HarnessQueuedInput;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.model.WorkspaceManifest;
import org.ruoyi.service.coding.harness.model.WorkspaceManifestValidator;
import org.ruoyi.service.coding.harness.plan.PlanAggregate;
import org.ruoyi.service.coding.harness.plan.PlanApprovalCommand;
import org.ruoyi.service.coding.harness.plan.ExecutionMode;
import org.ruoyi.service.coding.harness.modelruntime.HarnessModelPolicy;
import org.ruoyi.service.coding.harness.modelruntime.HarnessModelRouter;
import org.ruoyi.service.coding.harness.recovery.ToolEffectLedgerReconciler;
import org.ruoyi.service.coding.harness.recovery.UncertainToolEffectGuard;
import org.ruoyi.service.coding.harness.recovery.UncertainToolEffectReason;
import org.ruoyi.service.coding.harness.runtime.HarnessRunRequest;
import org.ruoyi.service.coding.harness.runtime.HarnessActiveTurnRegistry;
import org.ruoyi.service.coding.harness.runtime.HarnessScheduler;
import org.ruoyi.service.coding.harness.runtime.HarnessSessionGate;
import org.ruoyi.service.coding.harness.store.HarnessOptimisticLockException;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Authenticated command/query facade. It never derives ownership from client identifiers. */
@Service
@Slf4j
public class CodingHarnessApplicationService {

    static final int MAX_PENDING_INPUTS_PER_RUN = 64;
    static final int MAX_PENDING_INPUT_BYTES_PER_RUN = 1_048_576;
    private static final String INCOMPLETE_PLAN_SUSPENSION =
        "Model stopped before the authoritative plan passed verification";
    private final HarnessStore store;
    private final HarnessEventHub eventHub;
    private final HarnessEventOutboxService eventOutboxService;
    private final HarnessScheduler scheduler;
    private final HarnessSessionGate sessionGate;
    private final CodingWorkspaceService workspaceService;
    private final HarnessActiveTurnRegistry activeTurns;
    private final HarnessTranscriptReader transcriptReader;
    private final ToolProtocolValidator toolProtocolValidator = new ToolProtocolValidator();
    private final HarnessToolBatchCloser toolBatchCloser;
    private final ToolEffectLedgerReconciler toolEffectLedgerReconciler;
    private final HarnessBudgetPolicy budgetPolicy;
    private final HarnessModelRouter modelRouter = new HarnessModelRouter();
    private final WorkspaceManifestValidator workspaceManifestValidator =
        new WorkspaceManifestValidator();

    @Autowired
    private HarnessArtifactRepository artifactRepository;

    private static final int MAX_IMAGES_PER_RUN = 5;
    private static final long MAX_IMAGE_BYTES_PER_RUN = 8L * 1024 * 1024;
    private static final Map<String, byte[]> IMAGE_SIGNATURES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff},
        "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
        "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    @Autowired
    public CodingHarnessApplicationService(HarnessStore store, HarnessEventHub eventHub,
                                           HarnessEventOutboxService eventOutboxService,
                                           HarnessScheduler scheduler, HarnessSessionGate sessionGate,
                                           CodingWorkspaceService workspaceService,
                                           HarnessActiveTurnRegistry activeTurns,
                                           HarnessBudgetPolicy budgetPolicy) {
        this.store = store;
        this.eventHub = eventHub;
        this.eventOutboxService = Objects.requireNonNull(eventOutboxService,
            "eventOutboxService");
        this.scheduler = scheduler;
        this.sessionGate = sessionGate;
        this.workspaceService = workspaceService;
        this.activeTurns = activeTurns;
        this.transcriptReader = new HarnessTranscriptReader(store);
        this.toolBatchCloser = new HarnessToolBatchCloser(store, transcriptReader);
        this.toolEffectLedgerReconciler = new ToolEffectLedgerReconciler(store);
        this.budgetPolicy = Objects.requireNonNull(budgetPolicy, "budgetPolicy");
    }

    public CodingHarnessApplicationService(HarnessStore store, HarnessEventHub eventHub,
                                           HarnessScheduler scheduler, HarnessSessionGate sessionGate,
                                           CodingWorkspaceService workspaceService,
                                           HarnessActiveTurnRegistry activeTurns,
                                           HarnessBudgetPolicy budgetPolicy) {
        this(store, eventHub, new HarnessEventOutboxService(store, eventHub), scheduler,
            sessionGate, workspaceService, activeTurns, budgetPolicy);
    }

    public CodingHarnessApplicationService(HarnessStore store, HarnessEventHub eventHub,
                                           HarnessScheduler scheduler,
                                           HarnessSessionGate sessionGate,
                                           CodingWorkspaceService workspaceService,
                                           HarnessActiveTurnRegistry activeTurns) {
        this(store, eventHub, scheduler, sessionGate, workspaceService, activeTurns,
            HarnessBudgetPolicy.secureDefaults());
    }

    public HarnessSessionState createSession(HarnessOwner owner, CreateHarnessSessionCommand command) {
        if (command == null || command.model() == null || command.model().isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        Path workspace = canonicalWorkspace(owner, command.workspacePath());
        WorkspaceManifest workspaceManifest = workspaceManifestValidator.normalize(
            workspace, command.workspaceManifest());
        String model = command.model().strip();
        HarnessPermissionMode permissionMode = command.permissionMode() == null
            ? HarnessPermissionMode.READ_ONLY : command.permissionMode();
        HarnessApprovalPolicy approvalPolicy = command.approvalPolicy() == null
            ? HarnessApprovalPolicy.ON_REQUEST : command.approvalPolicy();
        String sessionId = stableId("session", owner, null, command.idempotencyKey());
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessSessionState existing = store.findSession(owner, sessionId).orElse(null);
            if (existing != null) {
                if (!existing.workspace().equals(workspace.toString())
                    || !existing.workspaceManifest().equals(workspaceManifest)
                    || !existing.model().equals(model)
                    || existing.permissionMode() != permissionMode
                    || existing.approvalPolicy() != approvalPolicy
                    || !Objects.equals(existing.title(), command.title())) {
                    throw new HarnessConflictException(
                        "idempotencyKey was already used for a different session request");
                }
                return existing;
            }
            long now = System.currentTimeMillis();
            return store.createSession(HarnessSessionState.createWithId(sessionId, owner,
                workspace.toString(), model, permissionMode, approvalPolicy, command.title(), now,
                workspaceManifest, command.thinkingLevel(), command.verificationMode()));
        });
    }

    public List<HarnessSessionState> listSessions(HarnessOwner owner) {
        return store.listSessions(owner);
    }

    public HarnessSessionState getSession(HarnessOwner owner, String sessionId) {
        return store.findSession(owner, sessionId)
            .filter(session -> session.deletedAt() == 0)
            .orElseThrow(() -> new HarnessNotFoundException("session", sessionId));
    }

    public HarnessSessionState setSessionPinned(HarnessOwner owner, String sessionId, boolean pinned) {
        return sessionGate.withSession(owner, sessionId, () -> {
            getSession(owner, sessionId);
            return store.setSessionPinned(owner, sessionId, pinned);
        });
    }

    public void deleteSession(HarnessOwner owner, String sessionId) {
        sessionGate.withSession(owner, sessionId, () -> store.setSessionDeleted(owner, sessionId, true));
    }

    public HarnessSessionState restoreSession(HarnessOwner owner, String sessionId) {
        return sessionGate.withSession(owner, sessionId, () -> store.setSessionDeleted(owner, sessionId, false));
    }

    public List<HarnessRunState> listRuns(HarnessOwner owner, String sessionId) {
        getSession(owner, sessionId);
        return store.listRuns(owner, sessionId);
    }

    public HarnessRunState getRun(HarnessOwner owner, String sessionId, String runId) {
        getSession(owner, sessionId);
        return store.findRun(owner, sessionId, runId)
            .orElseThrow(() -> new HarnessNotFoundException("run", runId));
    }

    public HarnessRunState createRun(HarnessOwner owner, String sessionId,
                                     CreateHarnessRunCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("run command is required");
        }
        HarnessBudget enforcedBudget = budgetPolicy.enforce(command.budget());
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessSessionState session = getSession(owner, sessionId);
            String runId = stableId("run", owner, sessionId, command.idempotencyKey());
            HarnessRunState existing = store.findRun(owner, sessionId, runId).orElse(null);
            if (existing != null) {
                if (!existing.originalRequirement().equals(command.requirement())
                    || !existing.budget().equals(enforcedBudget)) {
                    throw new HarnessConflictException(
                        "idempotencyKey was already used for a different run request");
                }
                validateImageModel(session, command.images());
                validateExistingInitialImages(owner, existing, command.images());
                return repairIdempotentRunCreation(owner, session, existing, command.images());
            }
            requireNoActiveRun(owner, session);
            validateImageModel(session, command.images());
            HarnessModelRoute modelRoute = modelRouter.route(session,
                command.requirement(), command.hasImages());
            long now = System.currentTimeMillis();
            HarnessRunState candidate = HarnessRunState.createWithId(runId, session,
                command.requirement(), enforcedBudget, modelRoute, now);
            HarnessRunState predecessor = session.activeRunId() == null ? null
                : store.findRun(owner, sessionId, session.activeRunId()).orElse(null);
            if (predecessor != null && predecessor.status().isTerminal()
                && !predecessor.contextCheckpoint().isEmpty()) {
                // A session sequence is global across follow-up runs. Reusing the predecessor's
                // durable boundary prevents every new run from rematerializing archived history;
                // failure counters are run-local and deliberately reset.
                candidate = candidate.withContextState(predecessor.contextCheckpoint(),
                    CompactionControl.initial(), now);
            }
            // Creation is observable only through this stable draft. Keeping it in the very first
            // run snapshot prevents a worker from publishing run.started before run.created and
            // lets an idempotent retry recover an interrupted ledger append.
            candidate = candidate.enqueueEvent(runCreatedEvent(owner, candidate), now);
            HarnessRunRequest request = new HarnessRunRequest(owner, sessionId, runId);
            // Admission precedes every durable mutation for this new run. A capacity exception is
            // therefore an honest HTTP 429: no run, session pointer, message or event was written.
            try (HarnessScheduler.Admission admission = scheduler.reserve(request)) {
                HarnessRunState run = store.createRun(owner, candidate);
                try {
                    session = store.saveSession(owner, session.withActiveRun(run.runId(), now),
                        session.revision());
                } catch (HarnessOptimisticLockException conflict) {
                    HarnessRunState cancelled = run.transition(HarnessRunStatus.CANCELLED,
                        "A concurrent run won the session lease", System.currentTimeMillis());
                    store.saveRun(owner, cancelled, run.revision());
                    throw new HarnessConflictException("Another run was created concurrently", conflict);
                }

                appendInitialMessageIfMissing(owner, run, command.images(), now);
                run = eventOutboxService.drainBestEffort(owner, run);
                if (run.eventOutbox().isEmpty()) {
                    commitAdmissionBestEffort(admission, run);
                }
                return run;
            }
        });
    }

    public HarnessRunState queueInput(HarnessOwner owner, String sessionId, String runId,
                                      QueueHarnessInputCommand command) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = getRun(owner, sessionId, runId);
            if (run.status().isTerminal()) {
                if (command.kind() == HarnessInputKind.FOLLOW_UP) {
                    return createRun(owner, sessionId,
                        new CreateHarnessRunCommand(command.content(),
                            budgetPolicy.forFollowUp(run.budget()),
                            "follow-up:" + command.idempotencyKey()));
                }
                throw new HarnessConflictException("Cannot steer a terminal run");
            }
            long now = System.currentTimeMillis();
            String inputId = stableId("input", owner, sessionId,
                runId + "\u0000" + command.idempotencyKey());
            HarnessQueuedInput existingInput = run.pendingInputs().stream()
                .filter(candidate -> candidate.inputId().equals(inputId)).findFirst().orElse(null);
            HarnessMessage existingMessage = findMessageByInputId(owner, sessionId, inputId);
            if ((existingInput != null && (existingInput.kind() != command.kind()
                || !existingInput.content().equals(command.content())))
                || (existingMessage != null && (!command.kind().name().equals(
                    existingMessage.metadata().get("kind"))
                    || !command.content().equals(existingMessage.content())))) {
                throw new HarnessConflictException(
                    "idempotencyKey was already used for a different queued input");
            }
            HarnessRunState next = run;
            boolean queuedInputPresent = existingInput != null;
            HarnessQueuedInput eventInput = existingInput;
            if (existingInput == null && existingMessage == null) {
                requirePendingInputCapacity(run, command.content());
                HarnessQueuedInput input = HarnessQueuedInput.createWithId(inputId,
                    command.kind(), command.content(), now);
                next = next.enqueue(input, now);
                queuedInputPresent = true;
                eventInput = input;
            }
            boolean wake = queuedInputPresent
                && next.status() == HarnessRunStatus.WAITING_FOR_INPUT;
            if (wake) {
                next = next.transition(HarnessRunStatus.QUEUED, null, now);
            }
            if (existingMessage == null) {
                if (eventInput == null) {
                    throw new IllegalStateException(
                        "Queued input event has no durable input identity");
                }
                String eventId = stableId("event", owner, sessionId,
                    runId + "\u0000input.queued\u0000" + inputId);
                next = next.enqueueEvent(HarnessEvent.draftWithId(eventId, sessionId, runId,
                    "input.queued", null, null, null, Map.of("inputId", inputId,
                        "kind", command.kind().name()), eventInput.createdAt()), now);
            }
            if (wake) {
                next = enqueueRunStateEvent(run, next, "run.queued",
                    Map.of("reason", "input_queued"), now);
            }
            HarnessRunState saved = next == run ? run
                : store.saveRun(owner, next, run.revision());
            if (existingMessage == null) {
                store.appendMessage(owner, HarnessMessage.draft(sessionId, runId,
                    HarnessMessageRole.CONTROL, command.content(), null, List.of(), null, null,
                    false, null, Map.of("kind", command.kind().name(), "queued", true,
                        "inputId", inputId), now));
            }
            return drainAndScheduleIfRunnable(owner, saved);
        });
    }

    public HarnessRunState cancel(HarnessOwner owner, String sessionId, String runId) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = eventOutboxService.drainBestEffort(owner,
                getRun(owner, sessionId, runId));
            if (run.status().isTerminal()) {
                return run;
            }
            long now = System.currentTimeMillis();
            HarnessRunRequest request = new HarnessRunRequest(owner, sessionId, runId);
            boolean acceptedBefore = run.cancellationRequested();
            boolean requestMarkerKnown;
            if (!acceptedBefore) {
                // A cancellation intent without its stable event cannot be reconstructed exactly.
                // Apply honest backpressure before touching the scheduler or active turn when a
                // legacy/full FIFO has no slot for the write-ahead marker.
                if (run.eventOutbox().size() >= HarnessRunState.MAX_EVENT_OUTBOX_ENTRIES) {
                    throw new HarnessConflictException(
                        "Cancellation is temporarily backpressured by the event outbox");
                }
                HarnessRunState requested = run.requestCancellation(now);
                requested = enqueueCancellationRequestEvent(run, requested, now);
                run = store.saveRun(owner, requested, run.revision());
                requestMarkerKnown = true;
            } else {
                requestMarkerKnown = cancellationRequestEventExists(owner, run);
            }
            if (acceptedBefore && !requestMarkerKnown) {
                // Compatibility repair for a snapshot accepted by an older writer. Never invent
                // the marker while the FIFO is full; the durable flag keeps execution fenced and
                // the next command/maintenance pass can retry after the head drains.
                if (run.eventOutbox().size() < HarnessRunState.MAX_EVENT_OUTBOX_ENTRIES) {
                    HarnessRunState repaired = enqueueCancellationRequestEvent(run, run, now);
                    run = store.saveRun(owner, repaired, run.revision());
                    requestMarkerKnown = true;
                }
            }

            // Scheduler removal and in-process interruption are notifications after the durable
            // intent. Either may race shutdown/rejection and must never turn an accepted command
            // into an HTTP error.
            boolean removedFromQueue = cancelQueuedBestEffort(request);
            cancelActiveTurnBestEffort(request);
            run = eventOutboxService.drainBestEffort(owner, run);
            if (!run.eventOutbox().isEmpty() || !requestMarkerKnown) {
                return run;
            }

            HarnessRunState reconciled = reconcileToolEffects(run,
                System.currentTimeMillis(), true);
            UncertainToolEffectGuard.Finding uncertain =
                UncertainToolEffectGuard.firstFinding(reconciled).orElse(null);
            if (uncertain != null) {
                return eventOutboxService.drainBestEffort(owner, reconciled);
            }
            if (isToolLedgerReconciliationIsolation(reconciled)) {
                return eventOutboxService.drainBestEffort(owner, reconciled);
            }
            run = reconciled;
            if (!removedFromQueue && run.status() == HarnessRunStatus.RUNNING) {
                return eventOutboxService.drainBestEffort(owner, run);
            }

            // Control receipts can exceed the bounded outbox. Stage at most the currently free
            // capacity minus one terminal slot, persist, drain, and continue. A failed drain
            // returns the non-terminal cancellationRequested snapshot; recovery/next API resumes
            // from the remaining per-effect drafts without losing or overtaking one.
            while (hasPendingControlEvents(run)) {
                long batchTimestamp = System.currentTimeMillis();
                HarnessRunState staged = stagePendingControlEvents(run, batchTimestamp);
                if (staged == run) {
                    return run;
                }
                run = store.saveRun(owner, staged, run.revision());
                run = eventOutboxService.drainBestEffort(owner, run);
                if (!run.eventOutbox().isEmpty()) {
                    return run;
                }
            }

            long terminalTimestamp = System.currentTimeMillis();
            HarnessToolBatchCloser.Closure closure = toolBatchCloser.close(run,
                SyntheticToolResultReason.CANCEL, terminalTimestamp);
            HarnessRunState terminalSource = closure.run();
            HarnessRunState next = abandonPendingToolEffects(terminalSource,
                    "Cancellation requested", terminalTimestamp)
                .transition(HarnessRunStatus.CANCELLED, null, terminalTimestamp);
            next = enqueueRunStateEvent(terminalSource, next, "run.cancelled",
                Map.of("code", "USER_CANCELLED"), terminalTimestamp);
            HarnessRunState saved = store.saveRun(owner, next, terminalSource.revision());
            return eventOutboxService.drainBestEffort(owner, saved);
        });
    }

    /**
     * Best-effort delivery of control events before a waiting/queued run becomes terminal. The
     * exact draft remains attached to the effect if delivery fails, so cancellation still returns
     * the authoritative committed tool result and startup recovery can retry by stable event id.
     */
    private HarnessRunState stagePendingControlEvents(HarnessRunState run, long now) {
        HarnessRunState next = run;
        int remaining = HarnessRunState.MAX_EVENT_OUTBOX_ENTRIES
            - run.eventOutbox().size() - 1;
        if (remaining <= 0) {
            return run;
        }
        for (HarnessToolEffect observed : run.toolEffects().values()) {
            if (remaining == 0) {
                break;
            }
            HarnessToolEffect effect = next.toolEffects().get(observed.toolCallId());
            if (effect == null || !effect.hasPendingControlEvent()) {
                continue;
            }
            HarnessEvent event = effect.controlEvent();
            next = next.enqueueEvent(event, now)
                .withToolEffect(effect.markControlEventPublished(), now);
            remaining--;
        }
        return next;
    }

    private boolean hasPendingControlEvents(HarnessRunState run) {
        return run.toolEffects().values().stream()
            .anyMatch(HarnessToolEffect::hasPendingControlEvent);
    }

    public HarnessRunState resume(HarnessOwner owner, String sessionId, String runId) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState persisted = eventOutboxService.drainBestEffort(owner,
                getRun(owner, sessionId, runId));
            long now = System.currentTimeMillis();
            HarnessRunState run = reconcileToolEffects(persisted, now, false);
            UncertainToolEffectGuard.Finding uncertain =
                UncertainToolEffectGuard.firstFinding(run).orElse(null);
            if (uncertain != null) {
                throw new HarnessConflictException(uncertain.reason().code()
                    + ": operator adjudication is required before resume");
            }
            if (isToolLedgerReconciliationIsolation(run)) {
                throw new HarnessConflictException(
                    UncertainToolEffectReason.LEDGER_RECONCILIATION_UNAVAILABLE.code()
                        + ": operator adjudication is required before resume");
            }
            if (run.status() == HarnessRunStatus.RUNNING) {
                return eventOutboxService.drainBestEffort(owner, run);
            }
            if (run.status() != HarnessRunStatus.SUSPENDED
                && run.status() != HarnessRunStatus.QUEUED) {
                throw new HarnessConflictException("Only a suspended run can be resumed");
            }
            HarnessRunState resumable = enqueueIncompletePlanResumeInput(run, now);
            if (run.status() == HarnessRunStatus.SUSPENDED) {
                // Only a fresh user decision re-arms the durable circuit. Startup recovery and
                // queued redispatch must preserve failures, otherwise an impossible compaction
                // can loop forever across process restarts.
                resumable = resumable.resetCompactionCircuit(now);
            }
            HarnessRunState queued = run;
            if (run.status() == HarnessRunStatus.SUSPENDED) {
                HarnessRunState transitioned = resumable.transition(HarnessRunStatus.QUEUED,
                    null, now);
                transitioned = enqueueRunStateEvent(run, transitioned, "run.queued",
                    Map.of("reason", "resume"), now);
                queued = store.saveRun(owner, transitioned, run.revision());
            }
            queued = eventOutboxService.drainBestEffort(owner, queued);
            if (queued.eventOutbox().isEmpty()) {
                scheduleBestEffort(queued);
            }
            return queued;
        });
    }

    /**
     * Re-queuing a transcript that already ends at a natural stop cannot create another provider
     * turn by itself. Persist one synthetic steering input for an explicit user resume so the
     * worker crosses a new model boundary. A queued retry keeps the same pending input, preserving
     * idempotency when dispatch fails after the state transition.
     */
    private HarnessRunState enqueueIncompletePlanResumeInput(HarnessRunState run, long now) {
        PlanAggregate plan = run.executionPlan();
        if (run.status() != HarnessRunStatus.SUSPENDED
            || !INCOMPLETE_PLAN_SUSPENSION.equals(run.error())
            || plan == null
            || (plan.mode() != ExecutionMode.BUILD && plan.mode() != ExecutionMode.VERIFY)
            || run.pendingInputs().stream().anyMatch(input ->
                input.kind() == HarnessInputKind.STEER
                    || input.kind() == HarnessInputKind.FOLLOW_UP)) {
            return run;
        }
        String instruction = "The user explicitly resumed this run after the model stopped before "
            + "the authoritative plan completed. Continue from the current " + plan.mode().name()
            + " state now: use the available tools, persist plan evidence and progress, and do not "
            + "stop after narration alone.";
        requirePendingInputCapacity(run, instruction);
        String inputId = stableId("input", run.owner(), run.sessionId(),
            run.runId() + "\u0000plan-resume\u0000" + run.revision());
        return run.enqueue(HarnessQueuedInput.createWithId(inputId, HarnessInputKind.STEER,
            instruction, now), now);
    }

    /** Resolves control-plane approval state only; the queued worker owns any later execution. */
    public HarnessRunState resolveToolApproval(HarnessOwner owner, String sessionId, String runId,
                                               String approvalId, String decisionId,
                                               ApprovalDecision decision, long expectedRevision,
                                               String argumentsSha256, String note) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = getRun(owner, sessionId, runId);
            ToolCallApprovalAggregate approval = run.toolApprovals().get(approvalId);
            if (approval == null) {
                throw new HarnessNotFoundException("tool approval", approvalId);
            }
            long now = System.currentTimeMillis();
            ToolCallApprovalAggregate resolved;
            try {
                resolved = approval.resolve(new ResolveApprovalCommand(
                    decisionId, decision, expectedRevision, argumentsSha256, owner, sessionId,
                    note), now);
            } catch (IllegalStateException conflict) {
                throw new HarnessConflictException(conflict.getMessage(), conflict);
            }
            boolean newlyResolved = resolved != approval;
            HarnessRunState next = newlyResolved ? run.withToolApproval(resolved, now) : run;

            HarnessApproval preview = run.approvals().get(approvalId);
            if (newlyResolved && preview != null) {
                HarnessApprovalStatus status = decision == ApprovalDecision.APPROVE
                    ? HarnessApprovalStatus.APPROVED_ONCE : HarnessApprovalStatus.DENIED;
                next = next.withApproval(new HarnessApproval(preview.approvalId(),
                    preview.toolCallId(), preview.toolName(), preview.capability(),
                    preview.summary(), preview.argumentsPreview(), status, preview.createdAt(),
                    now, owner.userId(), note), now);
            }

            boolean pending = next.toolApprovals().values().stream()
                .anyMatch(item -> item.state() == ApprovalState.PENDING);
            boolean shouldSchedule = next.status() == HarnessRunStatus.WAITING_FOR_APPROVAL
                && !pending;
            if (shouldSchedule) {
                next = next.transition(HarnessRunStatus.QUEUED, null, now);
            }
            if (newlyResolved) {
                String eventId = stableId("event", owner, sessionId,
                    runId + "\u0000approval.resolved\u0000" + approvalId
                        + "\u0000" + decisionId);
                long resolvedAt = resolved.decisionReceipt().resolvedAt();
                next = next.enqueueEvent(HarnessEvent.draftWithId(eventId, sessionId, runId,
                    "approval.resolved", null, resolved.toolCallId(), approvalId,
                    Map.of("decision", decision.name(), "state", resolved.state().name()),
                    resolvedAt), now);
            }
            if (shouldSchedule) {
                next = enqueueRunStateEvent(run, next, "run.queued",
                    Map.of("reason", "approval_resolved"), now);
            }
            HarnessRunState saved = next == run ? run
                : store.saveRun(owner, next, run.revision());
            return drainAndScheduleIfRunnable(owner, saved);
        });
    }

    /** Approves exactly one immutable taskId/revision/hash tuple; model tools cannot invoke this. */
    public HarnessRunState approvePlan(HarnessOwner owner, String sessionId, String runId,
                                       UUID taskId, long expectedRevision, String expectedHash,
                                       String idempotencyKey) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = getRun(owner, sessionId, runId);
            PlanAggregate plan = run.executionPlan();
            if (plan == null) {
                throw new HarnessConflictException("Run has no authoritative execution plan");
            }
            long now = System.currentTimeMillis();
            String normalizedIdempotencyKey = idempotencyKey == null
                ? null : idempotencyKey.strip();
            PlanAggregate approved;
            try {
                approved = plan.approveFromControlPlane(new PlanApprovalCommand(
                    taskId, expectedRevision, expectedHash, normalizedIdempotencyKey), now);
            } catch (IllegalStateException conflict) {
                throw new HarnessConflictException(conflict.getMessage(), conflict);
            }
            if (approved != plan) {
                requireStablePlanControlBoundary(owner, run);
            }
            HarnessRunState next = approved == plan ? run : run.withExecutionPlan(approved, now);
            boolean shouldSchedule = next.status() == HarnessRunStatus.WAITING_FOR_INPUT;
            if (shouldSchedule) {
                next = next.transition(HarnessRunStatus.QUEUED, null, now);
            }
            if (approved != plan) {
                String eventId = stableId("event", owner, sessionId,
                    runId + "\u0000plan.approved\u0000" + normalizedIdempotencyKey);
                long approvedAt = approved.approvalReceipts().get(normalizedIdempotencyKey)
                    .approvedAt();
                next = next.enqueueEvent(HarnessEvent.draftWithId(eventId, sessionId, runId,
                    "plan.approved", null, null, null,
                    Map.of("taskId", approved.taskId().toString(),
                        "revision", approved.revision(), "hash", approved.canonicalHash(),
                        "idempotencyKey", normalizedIdempotencyKey), approvedAt), now);
            }
            if (shouldSchedule) {
                next = enqueueRunStateEvent(run, next, "run.queued",
                    Map.of("reason", "plan_approved"), now);
            }
            HarnessRunState saved = next == run ? run : store.saveRun(owner, next, run.revision());
            return drainAndScheduleIfRunnable(owner, saved);
        });
    }

    /** Records authenticated plan feedback and schedules a new model turn to revise the draft. */
    public HarnessRunState requestPlanRevision(HarnessOwner owner, String sessionId, String runId,
                                               UUID taskId, long expectedRevision,
                                               String expectedHash, String feedbackId,
                                               String content) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = getRun(owner, sessionId, runId);
            PlanAggregate plan = run.executionPlan();
            if (plan == null) {
                throw new HarnessConflictException("Run has no authoritative execution plan");
            }
            String normalizedFeedbackId = feedbackId == null ? null : feedbackId.strip();
            var priorFeedback = plan.feedbackHistory().stream()
                .filter(item -> item.feedbackId().equals(normalizedFeedbackId))
                .findFirst().orElse(null);
            if (priorFeedback != null && !priorFeedback.content().equals(content.strip())) {
                throw new HarnessConflictException(
                    "feedbackId was already used with different plan feedback");
            }
            boolean idempotentReplay = priorFeedback != null && plan.taskId().equals(taskId);
            if (!idempotentReplay && (!plan.taskId().equals(taskId)
                || plan.revision() != expectedRevision
                || !constantTimeEquals(plan.canonicalHash(), expectedHash))) {
                throw new HarnessConflictException(
                    "Plan feedback targets a stale taskId, revision, or hash");
            }
            if (!idempotentReplay) {
                requireStablePlanControlBoundary(owner, run);
            }
            long now = System.currentTimeMillis();
            PlanAggregate revised;
            try {
                revised = plan.requestRevision(normalizedFeedbackId, content, now);
            } catch (IllegalStateException conflict) {
                throw new HarnessConflictException(conflict.getMessage(), conflict);
            }
            HarnessRunState next = revised == plan ? run : run.withExecutionPlan(revised, now);
            if (next.status() == HarnessRunStatus.WAITING_FOR_INPUT) {
                next = next.transition(HarnessRunStatus.QUEUED, null, now);
            }
            var durableFeedback = revised.feedbackHistory().stream()
                .filter(item -> item.feedbackId().equals(normalizedFeedbackId))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                    "Plan revision did not retain its durable feedback"));
            if (revised != plan) {
                String eventId = stableId("event", owner, sessionId,
                    runId + "\u0000plan.revision.requested\u0000" + normalizedFeedbackId);
                next = next.enqueueEvent(HarnessEvent.draftWithId(eventId, sessionId, runId,
                    "plan.revision.requested", null, null, null,
                    Map.of("taskId", taskId.toString(),
                        "feedbackId", normalizedFeedbackId,
                        "revision", revised.revision(), "hash", revised.canonicalHash()),
                    durableFeedback.createdAt()), now);
            }
            if (run.status() == HarnessRunStatus.WAITING_FOR_INPUT
                && next.status() == HarnessRunStatus.QUEUED) {
                next = enqueueRunStateEvent(run, next, "run.queued",
                    Map.of("reason", "plan_revision_requested"), now);
            }
            HarnessRunState saved = next == run ? run : store.saveRun(owner, next, run.revision());
            return drainAndScheduleIfRunnable(owner, saved);
        });
    }

    private void requireStablePlanControlBoundary(HarnessOwner owner, HarnessRunState run) {
        if (run.status() != HarnessRunStatus.WAITING_FOR_INPUT) {
            throw new HarnessConflictException(
                "Plan control commands require a stable WAITING_FOR_INPUT boundary");
        }
        if (run.modelEffect() != null
            && run.modelEffect().status() == org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus.PENDING) {
            throw new HarnessConflictException(
                "Plan control commands cannot race an active provider request");
        }
        // Plan approval is a control-plane decision over the durable run ledger, not a model
        // context projection. A checkpoint may compact through an ASSISTANT tool-call message
        // while its adjacent TOOL result remains after the checkpoint. Validating only the
        // post-checkpoint suffix would then manufacture an ORPHAN_RESULT and permanently block
        // an otherwise closed approval boundary. Rebuild the bounded, run-scoped ledger from
        // sequence zero so adjacency is evaluated against the authoritative transcript.
        List<HarnessMessage> runMessages = transcriptReader.readAfter(owner, run.sessionId(), 0)
            .stream()
            .filter(message -> run.runId().equals(message.runId()))
            .toList();
        ToolProtocolValidation validation = toolProtocolValidator.validate(runMessages);
        if (!validation.violations().isEmpty() || validation.lastUnclosedBatch().isPresent()) {
            throw new HarnessConflictException(
                "Plan control commands require a closed, valid tool protocol boundary");
        }
    }

    public List<HarnessMessage> readMessages(HarnessOwner owner, String sessionId,
                                             long afterSequence, int limit) {
        getSession(owner, sessionId);
        return store.readMessages(owner, sessionId, afterSequence, limit);
    }

    public List<HarnessEvent> readEvents(HarnessOwner owner, String sessionId, String runId,
                                         long afterSequence, int limit) {
        HarnessRunState run = getRun(owner, sessionId, runId);
        if (run.status().isTerminal()) {
            eventHub.ensureTerminalEvent(owner, sessionId, runId);
        }
        return store.readEvents(owner, sessionId, runId, afterSequence, limit);
    }

    private void requireNoActiveRun(HarnessOwner owner, HarnessSessionState session) {
        if (session.activeRunId() == null) {
            return;
        }
        HarnessRunState active = store.findRun(owner, session.sessionId(), session.activeRunId())
            .orElse(null);
        if (active != null && !active.status().isTerminal()) {
            throw new HarnessConflictException("Session already has an active run: " + active.runId());
        }
    }

    private HarnessRunState abandonPendingToolEffects(HarnessRunState run, String reason,
                                                       long now) {
        HarnessRunState next = run;
        for (HarnessToolEffect effect : run.toolEffects().values()) {
            if (effect.status() == HarnessToolEffectStatus.PENDING) {
                next = next.withToolEffect(effect.abandon(reason, now), now);
            }
        }
        return next;
    }

    private void requirePendingInputCapacity(HarnessRunState run, String content) {
        if (run.pendingInputs().size() >= MAX_PENDING_INPUTS_PER_RUN) {
            throw new HarnessConflictException("Pending input queue limit exceeded");
        }
        long bytes = content.getBytes(StandardCharsets.UTF_8).length;
        for (HarnessQueuedInput pending : run.pendingInputs()) {
            bytes += pending.content().getBytes(StandardCharsets.UTF_8).length;
            if (bytes > MAX_PENDING_INPUT_BYTES_PER_RUN) {
                break;
            }
        }
        if (bytes > MAX_PENDING_INPUT_BYTES_PER_RUN) {
            throw new HarnessConflictException("Pending input byte budget exceeded");
        }
    }

    /** Repairs every durable stage that can be left incomplete by a process crash or event outage. */
    private HarnessRunState repairIdempotentRunCreation(HarnessOwner owner,
                                                        HarnessSessionState session,
                                                        HarnessRunState run,
                                                        List<HarnessImageInput> images) {
        long now = System.currentTimeMillis();
        if (!run.status().isTerminal() && !run.runId().equals(session.activeRunId())) {
            if (session.activeRunId() != null) {
                HarnessRunState active = store.findRun(owner, session.sessionId(),
                    session.activeRunId()).orElse(null);
                if (active != null && !active.status().isTerminal()) {
                    throw new HarnessConflictException(
                        "Session already has an active run: " + active.runId());
                }
            }
            session = store.saveSession(owner, session.withActiveRun(run.runId(), now),
                session.revision());
        }

        appendInitialMessageIfMissing(owner, run, images, now);
        if (!outboxContainsType(run, "run.created")
            && !eventTypeExists(owner, run.sessionId(), run.runId(), "run.created")) {
            HarnessRunState staged = run.enqueueEvent(runCreatedEvent(owner, run), now);
            if (staged != run) {
                run = store.saveRun(owner, staged, run.revision());
            }
        }
        return drainAndScheduleIfRunnable(owner, run);
    }

    private void appendInitialMessageIfMissing(HarnessOwner owner, HarnessRunState run,
                                               List<HarnessImageInput> images, long now) {
        String inputId = "run-create:" + run.runId();
        if (messageWithInputIdExists(owner, run.sessionId(), inputId)) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", HarnessInputKind.INITIAL.name());
        metadata.put("inputId", inputId);
        List<Map<String, Object>> attachments = persistImages(owner, run, images);
        if (!attachments.isEmpty()) {
            metadata.put("images", attachments);
        }
        store.appendMessage(owner, HarnessMessage.draft(run.sessionId(), run.runId(),
            HarnessMessageRole.USER, run.originalRequirement(), null, List.of(), null, null,
            false, null, metadata, now));
    }

    private void validateImageModel(HarnessSessionState session, List<HarnessImageInput> images) {
        if (images != null && !images.isEmpty() && !HarnessModelPolicy.supportsImages(session.model())) {
            throw new IllegalArgumentException(
                "当前固定模型不支持图片输入；请选择支持多模态的模型（如 Doubao 或视觉模型）");
        }
    }

    private Map<String, Object> runCreatedPayload(HarnessRunState run) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", run.status().name());
        if (run.modelRoute() != null) {
            payload.put("modelRoute", run.modelRoute().eventMetadata());
        }
        return Map.copyOf(payload);
    }

    private HarnessEvent runCreatedEvent(HarnessOwner owner, HarnessRunState run) {
        String eventId = stableId("event", owner, run.sessionId(),
            run.runId() + "\u0000run.created");
        return HarnessEvent.draftWithId(eventId, run.sessionId(), run.runId(), "run.created",
            null, null, null, runCreatedPayload(run), run.createdAt());
    }

    private void validateExistingInitialImages(HarnessOwner owner, HarnessRunState run,
                                               List<HarnessImageInput> images) {
        HarnessMessage existing = findMessageByInputId(owner, run.sessionId(),
            "run-create:" + run.runId());
        if (existing == null) {
            return;
        }
        List<String> expected = decodeImages(images).stream().map(DecodedImage::sha256).toList();
        Object raw = existing.metadata().get("images");
        List<String> actual = raw instanceof List<?> list ? list.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(item -> Objects.toString(item.get("artifactId"), ""))
            .toList() : List.of();
        if (!actual.equals(expected)) {
            throw new HarnessConflictException(
                "idempotencyKey was already used with different images");
        }
    }

    private List<Map<String, Object>> persistImages(HarnessOwner owner, HarnessRunState run,
                                                    List<HarnessImageInput> images) {
        List<DecodedImage> decoded = decodeImages(images);
        if (decoded.isEmpty()) {
            return List.of();
        }
        if (artifactRepository == null) {
            throw new IllegalStateException("Harness artifact repository is unavailable");
        }
        List<Map<String, Object>> result = new ArrayList<>(decoded.size());
        for (DecodedImage image : decoded) {
            ArtifactRef ref = artifactRepository.putImage(owner, run.sessionId(), run.runId(),
                image.mediaType(), image.bytes());
            result.add(Map.of(
                "artifactId", ref.hash(),
                "mediaType", image.mediaType(),
                "byteSize", image.bytes().length,
                "detail", image.detail()
            ));
        }
        return List.copyOf(result);
    }

    private List<DecodedImage> decodeImages(List<HarnessImageInput> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > MAX_IMAGES_PER_RUN) {
            throw new IllegalArgumentException("A run accepts at most 5 images");
        }
        long total = 0;
        List<DecodedImage> result = new ArrayList<>(images.size());
        for (HarnessImageInput input : images) {
            String value = input.dataUrl();
            int separator = value.indexOf(",");
            if (separator < 0 || !value.startsWith("data:image/")
                || !value.substring(0, separator).endsWith(";base64")) {
                throw new IllegalArgumentException("Image must be a base64 data URL");
            }
            String mediaType = value.substring(5, separator - ";base64".length())
                .toLowerCase(java.util.Locale.ROOT);
            byte[] signature = IMAGE_SIGNATURES.get(mediaType);
            if (signature == null) {
                throw new IllegalArgumentException("Unsupported image media type: " + mediaType);
            }
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(value.substring(separator + 1));
            } catch (IllegalArgumentException invalidBase64) {
                throw new IllegalArgumentException("Image data URL contains invalid base64", invalidBase64);
            }
            if (!startsWith(bytes, signature)
                || ("image/webp".equals(mediaType)
                    && (bytes.length < 12 || bytes[8] != 0x57 || bytes[9] != 0x45
                        || bytes[10] != 0x42 || bytes[11] != 0x50))) {
                throw new IllegalArgumentException("Image content does not match " + mediaType);
            }
            total += bytes.length;
            if (bytes.length == 0 || total > MAX_IMAGE_BYTES_PER_RUN) {
                throw new IllegalArgumentException("Image payload exceeds the 8 MiB run limit");
            }
            result.add(new DecodedImage(mediaType, input.detail(), bytes, sha256(bytes)));
        }
        return List.copyOf(result);
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record DecodedImage(String mediaType, String detail, byte[] bytes, String sha256) { }

    private boolean eventTypeExists(HarnessOwner owner, String sessionId, String runId,
                                    String eventType) {
        long cursor = 0;
        int inspected = 0;
        while (inspected < 100_000) {
            List<HarnessEvent> page = store.readEvents(owner, sessionId, runId, cursor, 1_000);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessEvent event : page) {
                if (eventType.equals(event.type())) {
                    return true;
                }
                cursor = event.sequence();
                inspected++;
            }
            if (page.size() < 1_000) {
                return false;
            }
        }
        throw new HarnessConflictException("Run event ledger exceeds idempotency scan limit");
    }

    private boolean messageWithInputIdExists(HarnessOwner owner, String sessionId,
                                             String inputId) {
        return findMessageByInputId(owner, sessionId, inputId) != null;
    }

    private HarnessMessage findMessageByInputId(HarnessOwner owner, String sessionId,
                                                String inputId) {
        long cursor = 0;
        int inspected = 0;
        while (inspected < 100_000) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor, 1_000);
            if (page.isEmpty()) {
                return null;
            }
            for (HarnessMessage message : page) {
                if (inputId.equals(message.metadata().get("inputId"))) {
                    return message;
                }
                cursor = message.sequence();
                inspected++;
            }
            if (page.size() < 1_000) {
                return null;
            }
        }
        throw new HarnessConflictException("Session message ledger exceeds feedback scan limit");
    }

    private boolean constantTimeEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
            expected.strip().toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private HarnessRunState reconcileToolEffects(HarnessRunState initial, long now,
                                                  boolean requestCancellation) {
        try {
            return toolEffectLedgerReconciler.updateAtomically(initial, now, reconciled -> {
                UncertainToolEffectGuard.Finding finding =
                    UncertainToolEffectGuard.firstFinding(reconciled).orElse(null);
                if (finding == null) {
                    return reconciled;
                }
                HarnessRunState projected = requestCancellation
                    ? reconciled.requestCancellation(now) : reconciled;
                if ((!requestCancellation
                    && projected.status() == HarnessRunStatus.SUSPENDED)
                    || (projected.status() == HarnessRunStatus.SUSPENDED
                        && finding.reason().code().equals(projected.error()))) {
                    if (requestCancellation && !reconciled.cancellationRequested()) {
                        return enqueueRunStateEvent(reconciled, projected,
                            "run.cancel.requested",
                            Map.of("code", "USER_CANCEL_REQUESTED"), now);
                    }
                    return projected;
                }
                HarnessRunState suspended = UncertainToolEffectGuard.suspend(projected,
                    finding, now);
                return enqueueRunStateEvent(reconciled, suspended, "run.suspended",
                    Map.of("reason", finding.reason().code(),
                        "effectId", finding.effect().effectId()), now);
            });
        } catch (RuntimeException reconciliationFailure) {
            String reason =
                UncertainToolEffectReason.LEDGER_RECONCILIATION_UNAVAILABLE.code();
            HarnessRunState current = initial;
            for (int attempt = 0; attempt < 4; attempt++) {
                current = getRun(initial.owner(), initial.sessionId(), initial.runId());
                if (current.status().isTerminal()) {
                    return current;
                }
                HarnessRunState projected = requestCancellation
                    ? current.requestCancellation(now) : current;
                HarnessRunState isolated;
                if (projected.status() == HarnessRunStatus.SUSPENDED
                    && reason.equals(projected.error())) {
                    isolated = requestCancellation && !current.cancellationRequested()
                        ? enqueueRunStateEvent(current, projected, "run.cancel.requested",
                            Map.of("code", "USER_CANCEL_REQUESTED"), now)
                        : projected;
                } else {
                    isolated = UncertainToolEffectGuard.suspend(projected, reason, now);
                    isolated = enqueueRunStateEvent(current, isolated, "run.suspended",
                        Map.of("reason", reason,
                            "effectId", firstRecoverableEffectId(current)), now);
                }
                if (isolated == current) {
                    return current;
                }
                try {
                    return store.saveRun(current.owner(), isolated, current.revision());
                } catch (HarnessOptimisticLockException conflict) {
                    // Retry from the authoritative snapshot while the application session gate
                    // prevents a second local command from making a contradictory decision.
                }
            }
            throw new HarnessConflictException(reason);
        }
    }

    /** Stages a stable control/state event in the same revision as its authoritative mutation. */
    private HarnessRunState enqueueRunStateEvent(HarnessRunState source,
                                                  HarnessRunState mutated,
                                                  String type,
                                                  Map<String, Object> extra,
                                                  long timestamp) {
        if (source == null || mutated == null || source.revision() != mutated.revision()
            || !source.runId().equals(mutated.runId())) {
            throw new IllegalArgumentException("Run state event must share its source revision");
        }
        Map<String, Object> data = new LinkedHashMap<>(extra);
        data.put("status", mutated.status().name());
        data.put("revision", source.revision() + 1);
        HarnessEvent event = HarnessEvent.draftWithId(
            "run-state:" + source.runId() + ":" + type + ":" + (source.revision() + 1),
            source.sessionId(), source.runId(), type, null, null, null, data, timestamp);
        return mutated.enqueueEvent(event, timestamp);
    }

    /** Stable write-ahead event for the one cancellation decision a run can accept. */
    private HarnessRunState enqueueCancellationRequestEvent(HarnessRunState source,
                                                             HarnessRunState mutated,
                                                             long timestamp) {
        if (source == null || mutated == null || source.revision() != mutated.revision()
            || !source.runId().equals(mutated.runId()) || !mutated.cancellationRequested()) {
            throw new IllegalArgumentException(
                "Cancellation request event must share its accepted source revision");
        }
        String eventId = stableId("event", source.owner(), source.sessionId(),
            source.runId() + "\u0000run.cancel.requested");
        HarnessEvent event = HarnessEvent.draftWithId(eventId, source.sessionId(),
            source.runId(), "run.cancel.requested", null, null, null,
            Map.of("status", mutated.status().name(),
                "revision", source.revision() + 1,
                "code", "USER_CANCEL_REQUESTED"), timestamp);
        return mutated.enqueueEvent(event, timestamp);
    }

    private boolean cancellationRequestEventExists(HarnessOwner owner, HarnessRunState run) {
        return outboxContainsType(run, "run.cancel.requested")
            || eventTypeExists(owner, run.sessionId(), run.runId(), "run.cancel.requested");
    }

    private boolean outboxContainsType(HarnessRunState run, String type) {
        return run.eventOutbox().stream()
            .anyMatch(entry -> type.equals(entry.event().type()));
    }

    private HarnessRunState drainAndScheduleIfRunnable(HarnessOwner owner,
                                                        HarnessRunState saved) {
        HarnessRunState drained = eventOutboxService.drainBestEffort(owner, saved);
        if (drained.status() == HarnessRunStatus.QUEUED && drained.eventOutbox().isEmpty()) {
            scheduleBestEffort(drained);
        }
        return drained;
    }

    private void scheduleBestEffort(HarnessRunState run) {
        try {
            scheduler.schedule(new HarnessRunRequest(run.owner(), run.sessionId(), run.runId()));
        } catch (RuntimeException rejected) {
            // The durable QUEUED snapshot remains authoritative. Maintenance redispatches it.
            log.warn("Harness dispatch will be retried for run {}", run.runId(), rejected);
        }
    }

    private void commitAdmissionBestEffort(HarnessScheduler.Admission admission,
                                           HarnessRunState run) {
        try {
            admission.commit();
        } catch (RuntimeException rejected) {
            // Creation and run.created are already durable. Closing an uncommitted reservation
            // releases it; maintenance can admit the same durable run on a later pass.
            log.warn("Harness creation dispatch will be retried for run {}", run.runId(),
                rejected);
        }
    }

    private boolean cancelQueuedBestEffort(HarnessRunRequest request) {
        try {
            return scheduler.cancelQueued(request);
        } catch (RuntimeException rejected) {
            log.warn("Harness queued cancellation notification failed for run {}",
                request.runId(), rejected);
            return false;
        }
    }

    private void cancelActiveTurnBestEffort(HarnessRunRequest request) {
        try {
            activeTurns.cancel(request);
        } catch (RuntimeException interruptFailure) {
            // cancellationRequested and its event were committed first. The worker re-reads the
            // flag at every provider/tool boundary, so an in-process interrupt is only an
            // acceleration and never part of API success semantics.
            log.warn("Harness active-turn interrupt will be recovered for run {}",
                request.runId(), interruptFailure);
        }
    }

    private boolean isToolLedgerReconciliationIsolation(HarnessRunState run) {
        return run.status() == HarnessRunStatus.SUSPENDED
            && UncertainToolEffectReason.LEDGER_RECONCILIATION_UNAVAILABLE.code()
                .equals(run.error());
    }

    private String firstRecoverableEffectId(HarnessRunState run) {
        return run.toolEffects().values().stream()
            .filter(effect -> effect.status() == HarnessToolEffectStatus.PENDING
                || effect.status() == HarnessToolEffectStatus.COMMITTED)
            .map(HarnessToolEffect::effectId).sorted().findFirst().orElse("none");
    }

    private String stableId(String domain, HarnessOwner owner, String sessionId,
                            String idempotencyKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, domain);
            updateDigest(digest, owner.tenantId());
            updateDigest(digest, owner.userId().toString());
            updateDigest(digest, sessionId == null ? "" : sessionId);
            updateDigest(digest, idempotencyKey);
            byte[] value = digest.digest();
            StringBuilder encoded = new StringBuilder(50);
            encoded.append(switch (domain) {
                case "session" -> "s_";
                case "run" -> "r_";
                case "input" -> "i_";
                case "event" -> "e_";
                default -> throw new IllegalArgumentException("Unknown stable id domain");
            });
            for (int index = 0; index < 24; index++) {
                encoded.append(Character.forDigit((value[index] >>> 4) & 0xf, 16));
                encoded.append(Character.forDigit(value[index] & 0xf, 16));
            }
            return encoded.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private Path canonicalWorkspace(HarnessOwner owner, String requested) {
        Path root = workspaceService.resolveHarnessRoot(owner, requested);
        try {
            Files.createDirectories(root);
            return root.toRealPath();
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot open workspace: " + root, error);
        }
    }
}
