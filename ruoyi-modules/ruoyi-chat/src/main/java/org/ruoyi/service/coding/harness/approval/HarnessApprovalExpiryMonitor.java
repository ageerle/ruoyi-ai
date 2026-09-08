package org.ruoyi.service.coding.harness.approval;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.service.coding.harness.event.HarnessEventOutboxService;
import org.ruoyi.service.coding.harness.model.HarnessApproval;
import org.ruoyi.service.coding.harness.model.HarnessApprovalStatus;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.runtime.HarnessRunRequest;
import org.ruoyi.service.coding.harness.runtime.HarnessScheduler;
import org.ruoyi.service.coding.harness.runtime.HarnessSessionGate;
import org.ruoyi.service.coding.harness.store.HarnessRunScanPage;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Expires abandoned approval waits and continuously redispatches durable runnable work. The
 * rotating cursor also completes a bounded startup-recovery scan without retaining an unbounded
 * in-memory continuation.
 */
@Component
@Slf4j
public final class HarnessApprovalExpiryMonitor {

    private final HarnessStore store;
    private final HarnessScheduler scheduler;
    private final HarnessSessionGate sessionGate;
    private final HarnessEventOutboxService eventOutboxService;
    private final ScheduledExecutorService maintenance;
    private final boolean enabled;
    private final long intervalMillis;
    private final int pageSize;
    private final int maxRuns;
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile ScheduledFuture<?> future;
    private volatile String scanCursor;

    @Autowired
    public HarnessApprovalExpiryMonitor(
        HarnessStore store,
        HarnessScheduler scheduler,
        HarnessSessionGate sessionGate,
        HarnessEventHub eventHub,
        HarnessEventOutboxService eventOutboxService,
        @Qualifier("codingHarnessMaintenanceScheduler") ScheduledExecutorService maintenance,
        @Value("${coding.harness.approvals.expiry-monitor.enabled:true}") boolean enabled,
        @Value("${coding.harness.approvals.expiry-monitor.interval-millis:5000}")
        long intervalMillis,
        @Value("${coding.harness.approvals.expiry-monitor.page-size:64}") int pageSize,
        @Value("${coding.harness.approvals.expiry-monitor.max-runs:1000}") int maxRuns
    ) {
        if (intervalMillis < 100 || pageSize < 1 || pageSize > 1_000 || maxRuns < 1) {
            throw new IllegalArgumentException("Invalid approval expiry monitor limits");
        }
        this.store = store;
        this.scheduler = scheduler;
        this.sessionGate = sessionGate;
        this.eventOutboxService = eventOutboxService;
        this.maintenance = maintenance;
        this.enabled = enabled;
        this.intervalMillis = intervalMillis;
        this.pageSize = pageSize;
        this.maxRuns = maxRuns;
    }

    public HarnessApprovalExpiryMonitor(
        HarnessStore store,
        HarnessScheduler scheduler,
        HarnessSessionGate sessionGate,
        HarnessEventHub eventHub,
        ScheduledExecutorService maintenance,
        boolean enabled,
        long intervalMillis,
        int pageSize,
        int maxRuns
    ) {
        this(store, scheduler, sessionGate, eventHub,
            new HarnessEventOutboxService(store, eventHub), maintenance, enabled,
            intervalMillis, pageSize, maxRuns);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void start() {
        if (enabled && started.compareAndSet(false, true)) {
            future = maintenance.scheduleWithFixedDelay(this::sweepSafely, 0,
                intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void sweepSafely() {
        try {
            sweepOnce();
        } catch (RuntimeException failure) {
            // One maintenance failure must not terminate future expiry passes.
            log.error("Harness approval expiry sweep failed", failure);
        }
    }

    /** Visible for deterministic tests and operational probes. */
    public int sweepOnce() {
        int scanned = 0;
        int expired = 0;
        String cursor = scanCursor;
        long now = System.currentTimeMillis();
        while (scanned < maxRuns) {
            HarnessRunScanPage page = store.scanRunsForRecovery(cursor,
                Math.min(pageSize, maxRuns - scanned));
            for (HarnessRunState discovered : page.runs()) {
                scanned++;
                HarnessRunState run = eventOutboxService.drainBestEffort(discovered.owner(),
                    discovered);
                // A durable lifecycle/state event is a strict execution barrier. Never expire an
                // approval, start another provider/tool turn, or publish a later direct event
                // while the FIFO head is temporarily unavailable. Cursor rotation retries the
                // drain; once empty, runnable runs are dispatched again below.
                if (!run.eventOutbox().isEmpty()) {
                    continue;
                }
                if (run.status() == HarnessRunStatus.WAITING_FOR_APPROVAL) {
                    expired += expireRun(run, now);
                } else if (run.status() == HarnessRunStatus.QUEUED
                    || run.status() == HarnessRunStatus.RUNNING) {
                    scheduleRunnable(run);
                }
            }
            if (!page.hasMore() || page.runs().isEmpty()) {
                scanCursor = null;
                break;
            }
            cursor = page.nextCursor();
            scanCursor = cursor;
            if (scanned >= maxRuns) {
                break;
            }
        }
        return expired;
    }

    private int expireRun(HarnessRunState discovered, long now) {
        return sessionGate.withSession(discovered.owner(), discovered.sessionId(), () -> {
            HarnessRunState run = store.findRun(discovered.owner(), discovered.sessionId(),
                discovered.runId()).orElse(null);
            if (run == null || run.status() != HarnessRunStatus.WAITING_FOR_APPROVAL) {
                return 0;
            }
            // The discovery snapshot was drained outside the gate and may already be stale. Re-read
            // and drain again while holding the session writer before calculating capacity or
            // mutating approvals; otherwise a concurrent lifecycle/control draft can be overtaken.
            run = eventOutboxService.drainBestEffort(run.owner(), run);
            if (run.status() != HarnessRunStatus.WAITING_FOR_APPROVAL
                || !run.eventOutbox().isEmpty()) {
                return 0;
            }
            HarnessRunState next = run;
            int count = 0;
            int available = Math.max(0, HarnessRunState.MAX_EVENT_OUTBOX_ENTRIES
                - run.eventOutbox().size() - 1);
            for (ToolCallApprovalAggregate approval : run.toolApprovals().values()) {
                // Reserve one FIFO slot for run.queued when this batch expires the final pending
                // approval. Larger batches remain WAITING and are continued by the next sweep.
                if (count >= available) {
                    break;
                }
                if ((approval.state() == ApprovalState.PENDING
                    || approval.state() == ApprovalState.APPROVED)
                    && now >= approval.expiresAt()) {
                    ToolCallApprovalAggregate expired = approval.expire(now);
                    next = next.withToolApproval(expired, now);
                    HarnessApproval preview = next.approvals().get(approval.approvalId());
                    if (preview != null) {
                        next = next.withApproval(new HarnessApproval(preview.approvalId(),
                            preview.toolCallId(), preview.toolName(), preview.capability(),
                            preview.summary(), preview.argumentsPreview(),
                            HarnessApprovalStatus.EXPIRED, preview.createdAt(), now, null,
                            "Approval expired before execution"), now);
                    }
                    next = next.enqueueEvent(HarnessEvent.draftWithId(
                        "approval:" + approval.approvalId() + ":expired:"
                            + expired.revision(), run.sessionId(), run.runId(),
                        "approval.expired", null, approval.toolCallId(),
                        approval.approvalId(), Map.of("state", expired.state().name(),
                            "revision", expired.revision()), now), now);
                    count++;
                }
            }
            if (count == 0) {
                return 0;
            }
            boolean unresolved = next.toolApprovals().values().stream().anyMatch(approval ->
                approval.state() == ApprovalState.PENDING
                    || approval.state() == ApprovalState.APPROVED);
            if (!unresolved) {
                next = next.transition(HarnessRunStatus.QUEUED, null, now);
                next = next.enqueueEvent(HarnessEvent.draftWithId(
                    "run-state:" + run.runId() + ":run.queued:" + (run.revision() + 1),
                    run.sessionId(), run.runId(), "run.queued", null, null, null,
                    Map.of("status", HarnessRunStatus.QUEUED.name(),
                        "revision", run.revision() + 1,
                        "reason", "approval_expired"), now), now);
            }
            HarnessRunState saved = store.saveRun(run.owner(), next, run.revision());
            HarnessRunState drained = eventOutboxService.drainBestEffort(run.owner(), saved);
            if (drained.status() == HarnessRunStatus.QUEUED
                && drained.eventOutbox().isEmpty()) {
                scheduleRunnable(drained);
            }
            return count;
        });
    }

    private void scheduleRunnable(HarnessRunState run) {
        try {
            scheduler.schedule(new HarnessRunRequest(run.owner(), run.sessionId(), run.runId()));
        } catch (RuntimeException rejected) {
            // QUEUED/RUNNING is the durable dispatch state. A later cursor rotation retries it.
            log.warn("Harness runnable-run dispatch will be retried for run {}", run.runId(),
                rejected);
        }
    }

    @PreDestroy
    public void stop() {
        ScheduledFuture<?> scheduled = future;
        if (scheduled != null) {
            scheduled.cancel(false);
        }
    }
}
