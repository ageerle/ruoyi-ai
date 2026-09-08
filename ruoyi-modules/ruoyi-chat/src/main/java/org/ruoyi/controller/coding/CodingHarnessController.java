package org.ruoyi.controller.coding;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.coding.harness.app.CodingHarnessApplicationService;
import org.ruoyi.service.coding.harness.app.CreateHarnessRunCommand;
import org.ruoyi.service.coding.harness.app.CreateHarnessSessionCommand;
import org.ruoyi.service.coding.harness.app.HarnessImageInput;
import org.ruoyi.service.coding.harness.app.QueueHarnessInputCommand;
import org.ruoyi.service.coding.harness.approval.ApprovalDecision;
import org.ruoyi.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.service.coding.harness.event.HarnessEventSubscription;
import org.ruoyi.service.coding.harness.model.HarnessBudget;
import org.ruoyi.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessInputKind;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessPermissionMode;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessThinkingLevel;
import org.ruoyi.service.coding.harness.model.HarnessVerificationMode;
import org.ruoyi.service.coding.harness.model.WorkspaceManifest;
import org.ruoyi.service.coding.harness.modelruntime.HarnessModelRegistry;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Durable coding-agent Harness API. Authentication is mandatory for every endpoint. */
@Validated
@RestController
@RequestMapping("/coding/harness")
@SaCheckPermission("coding:harness:use")
public class CodingHarnessController {

    private static final long STREAM_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000L;

    private final CodingHarnessApplicationService applicationService;
    private final HarnessEventHub eventHub;
    private final HarnessModelRegistry modelRegistry;

    public CodingHarnessController(CodingHarnessApplicationService applicationService,
                                   HarnessEventHub eventHub,
                                   HarnessModelRegistry modelRegistry) {
        this.applicationService = applicationService;
        this.eventHub = eventHub;
        this.modelRegistry = modelRegistry;
    }

    @PostMapping("/sessions")
    public R<HarnessSessionState> createSession(@Valid @RequestBody CreateSessionRequest request) {
        if (request.permissionMode() != HarnessPermissionMode.READ_ONLY) {
            // Permission mode is a requested ceiling, never a client-granted authority.
            StpUtil.checkPermission("coding:harness:write");
        }
        modelRegistry.requireSessionModelReady(request.model());
        HarnessThinkingLevel thinkingLevel = HarnessThinkingLevel.fromWire(request.thinkingLevel());
        HarnessVerificationMode verificationMode = request.verificationMode() == null
            ? HarnessVerificationMode.DEFAULT : request.verificationMode();
        return R.ok(applicationService.createSession(owner(), new CreateHarnessSessionCommand(
            request.workspacePath(), request.model(), request.permissionMode(),
            request.approvalPolicy(), request.title(), request.idempotencyKey(),
            request.workspaceManifest(), thinkingLevel, verificationMode)));
    }

    @GetMapping("/sessions")
    public R<List<HarnessSessionState>> listSessions() {
        return R.ok(applicationService.listSessions(owner()));
    }

    @GetMapping("/sessions/{sessionId}")
    public R<HarnessSessionState> getSession(@PathVariable String sessionId) {
        return R.ok(applicationService.getSession(owner(), sessionId));
    }

    @PutMapping("/sessions/{sessionId}/pin")
    public R<HarnessSessionState> setSessionPinned(@PathVariable String sessionId,
                                                  @Valid @RequestBody PinSessionRequest request) {
        return R.ok(applicationService.setSessionPinned(owner(), sessionId, request.pinned()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public R<Void> deleteSession(@PathVariable String sessionId) {
        applicationService.deleteSession(owner(), sessionId);
        return R.ok();
    }

    @PostMapping("/sessions/{sessionId}/restore")
    public R<HarnessSessionState> restoreSession(@PathVariable String sessionId) {
        return R.ok(applicationService.restoreSession(owner(), sessionId));
    }

    public record PinSessionRequest(@NotNull Boolean pinned) { }

    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<HarnessMessage>> messages(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "0") @Min(0) long afterSequence,
        @RequestParam(defaultValue = "500") @Min(1) @Max(10_000) int limit) {
        return R.ok(applicationService.readMessages(owner(), sessionId, afterSequence, limit));
    }

    @PostMapping("/sessions/{sessionId}/runs")
    public R<HarnessRunState> createRun(@PathVariable String sessionId,
                                        @Valid @RequestBody CreateRunRequest request) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.createRun(owner(), sessionId,
            new CreateHarnessRunCommand(request.requirement(), request.budget(),
                request.idempotencyKey(), request.images())));
    }

    @GetMapping("/sessions/{sessionId}/runs")
    public R<List<HarnessRunState>> listRuns(@PathVariable String sessionId) {
        return R.ok(applicationService.listRuns(owner(), sessionId));
    }

    @GetMapping("/sessions/{sessionId}/runs/{runId}")
    public R<HarnessRunState> getRun(@PathVariable String sessionId, @PathVariable String runId) {
        return R.ok(applicationService.getRun(owner(), sessionId, runId));
    }

    @GetMapping("/sessions/{sessionId}/runs/{runId}/events")
    public R<List<HarnessEvent>> events(
        @PathVariable String sessionId,
        @PathVariable String runId,
        @RequestParam(defaultValue = "0") @Min(0) long afterSequence,
        @RequestParam(defaultValue = "1000") @Min(1) @Max(10_000) int limit) {
        return R.ok(applicationService.readEvents(owner(), sessionId, runId, afterSequence, limit));
    }

    @GetMapping(value = "/sessions/{sessionId}/runs/{runId}/events/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
        @PathVariable String sessionId,
        @PathVariable String runId,
        @RequestParam(required = false) Long afterSequence,
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        HarnessOwner owner = owner();
        applicationService.getRun(owner, sessionId, runId);
        long cursor = resolveCursor(afterSequence, lastEventId);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicReference<HarnessEventSubscription> subscription = new AtomicReference<>();
        AtomicBoolean transportClosed = new AtomicBoolean(false);

        Runnable closeSubscription = () -> {
            transportClosed.set(true);
            HarnessEventSubscription current = subscription.getAndSet(null);
            if (current != null) {
                current.close();
            }
        };
        emitter.onCompletion(closeSubscription);
        emitter.onTimeout(closeSubscription);
        emitter.onError(ignored -> closeSubscription.run());

        HarnessEventSubscription created = eventHub.subscribe(owner, sessionId, runId, cursor,
            event -> sendEvent(emitter, event, closeSubscription), error -> {
                closeSubscription.run();
                try {
                    emitter.completeWithError(error);
                } catch (IllegalStateException ignored) {
                    // The client has already disconnected.
                }
            });
        subscription.set(created);
        if (transportClosed.get() && subscription.compareAndSet(created, null)) {
            created.close();
        }
        return emitter;
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/inputs")
    public R<HarnessRunState> queueInput(@PathVariable String sessionId, @PathVariable String runId,
                                         @Valid @RequestBody QueueInputRequest request) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.queueInput(owner(), sessionId, runId,
            new QueueHarnessInputCommand(request.kind(), request.content(),
                request.idempotencyKey())));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/cancel")
    public R<HarnessRunState> cancel(@PathVariable String sessionId, @PathVariable String runId) {
        return R.ok(applicationService.cancel(owner(), sessionId, runId));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/resume")
    public R<HarnessRunState> resume(@PathVariable String sessionId, @PathVariable String runId) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.resume(owner(), sessionId, runId));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/approvals/{approvalId}/resolve")
    @SaCheckPermission("coding:harness:approve")
    public R<HarnessRunState> resolveApproval(
        @PathVariable String sessionId,
        @PathVariable String runId,
        @PathVariable String approvalId,
        @Valid @RequestBody ResolveApprovalRequest request) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.resolveToolApproval(owner(), sessionId, runId, approvalId,
            request.decisionId(), request.decision(), request.expectedRevision(),
            request.argumentsSha256(), request.note()));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/plan/approve")
    public R<HarnessRunState> approvePlan(
        @PathVariable String sessionId,
        @PathVariable String runId,
        @Valid @RequestBody ApprovePlanRequest request) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.approvePlan(owner(), sessionId, runId, request.taskId(),
            request.expectedRevision(), request.expectedHash(), request.idempotencyKey()));
    }

    @PostMapping("/sessions/{sessionId}/runs/{runId}/plan/revision")
    public R<HarnessRunState> requestPlanRevision(
        @PathVariable String sessionId,
        @PathVariable String runId,
        @Valid @RequestBody RequestPlanRevisionRequest request) {
        requireCurrentPermissionCeiling(sessionId);
        return R.ok(applicationService.requestPlanRevision(owner(), sessionId, runId,
            request.taskId(), request.expectedRevision(), request.expectedHash(),
            request.feedbackId(), request.content()));
    }

    private void sendEvent(SseEmitter emitter, HarnessEvent event, Runnable closeSubscription) {
        try {
            emitter.send(SseEmitter.event().id(Long.toString(event.sequence()))
                .name(event.type()).data(event));
            if (event.type().equals("run.completed") || event.type().equals("run.failed")
                || event.type().equals("run.cancelled")) {
                closeSubscription.run();
                emitter.complete();
            }
        } catch (IOException | IllegalStateException error) {
            closeSubscription.run();
            try {
                emitter.completeWithError(error);
            } catch (IllegalStateException ignored) {
                // The response is already complete.
            }
        }
    }

    private long resolveCursor(Long queryCursor, String lastEventId) {
        long cursor = queryCursor == null ? 0 : queryCursor;
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                cursor = Math.max(cursor, Long.parseLong(lastEventId));
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("Last-Event-ID must be a non-negative sequence");
            }
        }
        if (cursor < 0) {
            throw new IllegalArgumentException("Event cursor cannot be negative");
        }
        return cursor;
    }

    private HarnessOwner owner() {
        return new HarnessOwner(LoginHelper.getTenantId(), LoginHelper.getUserId());
    }

    private void requireCurrentPermissionCeiling(String sessionId) {
        HarnessSessionState session = applicationService.getSession(owner(), sessionId);
        if (session.permissionMode() != HarnessPermissionMode.READ_ONLY) {
            StpUtil.checkPermission("coding:harness:write");
        }
    }

    public record CreateSessionRequest(
        String workspacePath,
        @NotBlank @Size(max = 200) String model,
        @NotNull HarnessPermissionMode permissionMode,
        HarnessApprovalPolicy approvalPolicy,
        @Size(max = 200) String title,
        @NotBlank @Size(max = 256) String idempotencyKey,
        @Valid WorkspaceManifest workspaceManifest,
        /** Doubao 思考等级（none/minimal/low/medium/high/xhigh/max）；非 Doubao 模型忽略。 */
        @Size(max = 16) String thinkingLevel,
        /** 验证归属：AGENT（默认）或 EXTERNAL（独立外部验收）。 */
        HarnessVerificationMode verificationMode
    ) {
        /** Backward-compatible constructor for direct callers predating workspace manifests. */
        public CreateSessionRequest(String workspacePath, String model,
                                    HarnessPermissionMode permissionMode,
                                    HarnessApprovalPolicy approvalPolicy, String title,
                                    String idempotencyKey) {
            this(workspacePath, model, permissionMode, approvalPolicy, title, idempotencyKey,
                null, null, null);
        }
    }

    public record CreateRunRequest(
        @NotBlank @Size(max = 200_000) String requirement,
        @Valid HarnessBudget budget,
        @NotBlank @Size(max = 256) String idempotencyKey,
        @Valid @Size(max = 5) List<HarnessImageInput> images
    ) { }

    public record QueueInputRequest(
        @NotNull HarnessInputKind kind,
        @NotBlank @Size(max = 200_000) String content,
        @NotBlank @Size(max = 256) String idempotencyKey
    ) { }

    public record ResolveApprovalRequest(
        @NotBlank @Size(max = 256) String decisionId,
        @NotNull ApprovalDecision decision,
        @Min(0) long expectedRevision,
        @NotBlank @Size(min = 64, max = 64) String argumentsSha256,
        @Size(max = 2_048) String note
    ) { }

    public record ApprovePlanRequest(
        @NotNull UUID taskId,
        @Min(0) long expectedRevision,
        @NotBlank @Size(min = 64, max = 64) String expectedHash,
        @NotBlank @Size(max = 256) String idempotencyKey
    ) { }

    public record RequestPlanRevisionRequest(
        @NotNull UUID taskId,
        @Min(0) long expectedRevision,
        @NotBlank @Size(min = 64, max = 64) String expectedHash,
        @NotBlank @Size(max = 256) String feedbackId,
        @NotBlank @Size(max = 20_000) String content
    ) { }
}
