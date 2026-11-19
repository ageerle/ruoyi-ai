package org.ruoyi.chat.service.coding.harness.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Owner-fair, cancellation-safe scheduler. A session is FIFO and single-writer, while an owner
 * can reserve only a bounded number of executor slots across all of its sessions.
 */
@Slf4j
@Service
public class HarnessScheduler {

    private static final int DEFAULT_MAX_CONCURRENT_RUNS_PER_OWNER = 1;
    private static final int DEFAULT_MAX_PENDING_RUNS_PER_OWNER = 64;
    private static final int DEFAULT_MAX_PENDING_RUNS_PER_TENANT = 512;
    private static final int DEFAULT_MAX_PENDING_RUNS_GLOBAL = 2_048;
    private static final long DEFAULT_RETRY_DELAY_MILLIS = 100;
    private static final long MAX_RETRY_DELAY_MILLIS = 5_000;
    private static final ScheduledExecutorService FALLBACK_RETRY_SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "coding-harness-dispatch-retry");
            thread.setDaemon(true);
            return thread;
        });

    private final Executor executor;
    private final HarnessRunProcessor processor;
    private final int maxConcurrentRunsPerOwner;
    private final int maxPendingRunsPerOwner;
    private final int maxPendingRunsPerTenant;
    private final int maxPendingRunsGlobal;
    private final RetryScheduler retryScheduler;
    private final long retryDelayMillis;
    private final Object monitor = new Object();
    private final Map<SessionKey, Lane> lanes = new HashMap<>();
    private final Map<OwnerKey, OwnerQueue> ownerQueues = new HashMap<>();
    private final ArrayDeque<OwnerKey> readyOwners = new ArrayDeque<>();
    private final Set<OwnerKey> readyOwnerIds = new HashSet<>();
    private final Set<RunKey> pendingRuns = new HashSet<>();
    private final Map<OwnerKey, Integer> pendingRunsByOwner = new HashMap<>();
    private final Map<String, Integer> pendingRunsByTenant = new HashMap<>();
    private int pendingRunCount;
    private boolean retryScheduled;
    private int retryAttempt;

    public HarnessScheduler(Executor executor, HarnessRunProcessor processor) {
        this(executor, processor, DEFAULT_MAX_CONCURRENT_RUNS_PER_OWNER);
    }

    public HarnessScheduler(Executor executor, HarnessRunProcessor processor,
                            int maxConcurrentRunsPerOwner) {
        this(executor, processor, maxConcurrentRunsPerOwner,
            DEFAULT_MAX_PENDING_RUNS_PER_OWNER, DEFAULT_MAX_PENDING_RUNS_PER_TENANT,
            DEFAULT_MAX_PENDING_RUNS_GLOBAL,
            (task, delay) -> FALLBACK_RETRY_SCHEDULER.schedule(
                task, delay, TimeUnit.MILLISECONDS), DEFAULT_RETRY_DELAY_MILLIS);
    }

    @Autowired
    public HarnessScheduler(@Qualifier("codingHarnessRunExecutor") Executor executor,
                            HarnessRunProcessor processor,
                            @Qualifier("codingHarnessMaintenanceScheduler")
                            ScheduledExecutorService maintenanceScheduler,
                            @Value("${coding.harness.runner.max-concurrent-per-owner:1}")
                            int maxConcurrentRunsPerOwner,
                            @Value("${coding.harness.runner.max-pending-per-owner:64}")
                            int maxPendingRunsPerOwner,
                            @Value("${coding.harness.runner.max-pending-per-tenant:512}")
                            int maxPendingRunsPerTenant,
                            @Value("${coding.harness.runner.max-pending-global:2048}")
                            int maxPendingRunsGlobal,
                            @Value("${coding.harness.runner.retry-delay-millis:100}")
                            long retryDelayMillis) {
        this(executor, processor, maxConcurrentRunsPerOwner, maxPendingRunsPerOwner,
            maxPendingRunsPerTenant, maxPendingRunsGlobal,
            (task, delay) -> maintenanceScheduler.schedule(task, delay, TimeUnit.MILLISECONDS),
            retryDelayMillis);
    }

    HarnessScheduler(Executor executor, HarnessRunProcessor processor,
                     int maxConcurrentRunsPerOwner, RetryScheduler retryScheduler,
                     long retryDelayMillis) {
        this(executor, processor, maxConcurrentRunsPerOwner,
            DEFAULT_MAX_PENDING_RUNS_PER_OWNER, DEFAULT_MAX_PENDING_RUNS_PER_TENANT,
            DEFAULT_MAX_PENDING_RUNS_GLOBAL, retryScheduler, retryDelayMillis);
    }

    HarnessScheduler(Executor executor, HarnessRunProcessor processor,
                     int maxConcurrentRunsPerOwner, int maxPendingRunsPerOwner,
                     int maxPendingRunsPerTenant, int maxPendingRunsGlobal,
                     RetryScheduler retryScheduler, long retryDelayMillis) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.processor = Objects.requireNonNull(processor, "processor");
        this.retryScheduler = Objects.requireNonNull(retryScheduler, "retryScheduler");
        if (maxConcurrentRunsPerOwner < 1) {
            throw new IllegalArgumentException(
                "Harness owner run concurrency limit must be positive");
        }
        if (retryDelayMillis < 1 || retryDelayMillis > MAX_RETRY_DELAY_MILLIS) {
            throw new IllegalArgumentException("Harness runner retry delay is invalid");
        }
        if (maxPendingRunsPerOwner < 1 || maxPendingRunsPerTenant < 1
            || maxPendingRunsGlobal < 1) {
            throw new IllegalArgumentException("Harness pending-run limits must be positive");
        }
        this.maxConcurrentRunsPerOwner = maxConcurrentRunsPerOwner;
        this.maxPendingRunsPerOwner = maxPendingRunsPerOwner;
        this.maxPendingRunsPerTenant = maxPendingRunsPerTenant;
        this.maxPendingRunsGlobal = maxPendingRunsGlobal;
        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * Reserves pending capacity without making the request runnable. New-run creation uses this
     * before its first durable write so an HTTP 429 can truthfully mean that nothing was accepted.
     */
    public Admission reserve(HarnessRunRequest request) {
        Objects.requireNonNull(request, "request");
        synchronized (monitor) {
            RunKey runKey = RunKey.of(request);
            if (knownRun(runKey)) {
                return new ExistingAdmission();
            }
            CapacityFailure failure = capacityFailure(runKey.sessionKey().ownerKey());
            if (failure != null) {
                throw new HarnessSchedulerCapacityException(failure.scope(), failure.limit());
            }
            admitPending(runKey);
            return new PendingAdmission(runKey, request);
        }
    }

    /**
     * Admits an already-durable QUEUED request when capacity is available. At capacity the caller
     * keeps the run as a durable outbox entry and a later maintenance sweep retries it.
     */
    public HarnessScheduleResult schedule(HarnessRunRequest request) {
        Objects.requireNonNull(request, "request");
        List<Dispatch> dispatches;
        HarnessScheduleResult result;
        synchronized (monitor) {
            SessionKey key = SessionKey.of(request);
            RunKey runKey = new RunKey(key, request.runId());
            if (knownRun(runKey)) {
                result = HarnessScheduleResult.ALREADY_SCHEDULED;
            } else {
                CapacityFailure failure = capacityFailure(key.ownerKey());
                if (failure != null) {
                    return HarnessScheduleResult.DEFERRED_CAPACITY;
                }
                admitPending(runKey);
                Lane lane = lanes.computeIfAbsent(key, ignored -> new Lane(key));
                lane.queuedRunIds.add(request.runId());
                lane.queue.addLast(request);
                result = HarnessScheduleResult.SCHEDULED;
                markReady(lane, false);
            }
            dispatches = reserveGlobally();
        }
        submit(dispatches);
        return result;
    }

    /**
     * Removes a run only while it is still in this scheduler's durable retry queue. Repeated
     * cancellation is intentionally a no-op and can never release the active session writer.
     */
    public boolean cancelQueued(HarnessRunRequest request) {
        Objects.requireNonNull(request, "request");
        synchronized (monitor) {
            SessionKey key = SessionKey.of(request);
            Lane lane = lanes.get(key);
            if (lane == null || request.runId().equals(lane.activeRunId)
                || !lane.queuedRunIds.remove(request.runId())) {
                return false;
            }
            lane.queue.removeIf(queued -> queued.runId().equals(request.runId()));
            releasePending(RunKey.of(request));
            if (lane.queue.isEmpty()) {
                unmarkReady(lane);
                retireIfIdle(lane);
            }
            return true;
        }
    }

    public int laneCount() {
        synchronized (monitor) {
            return lanes.size();
        }
    }

    /** Safe explicit cleanup hook; callers may invoke it from maintenance without racing enqueue. */
    public int removeIdleLanes() {
        synchronized (monitor) {
            int before = lanes.size();
            for (Lane lane : List.copyOf(lanes.values())) {
                retireIfIdle(lane);
            }
            return before - lanes.size();
        }
    }

    private HarnessScheduleResult commit(PendingAdmission admission) {
        List<Dispatch> dispatches;
        synchronized (monitor) {
            admission.requireOpen("commit");
            if (!pendingRuns.contains(admission.runKey)) {
                throw new IllegalStateException("Harness scheduler admission was already released");
            }
            SessionKey key = admission.runKey.sessionKey();
            Lane lane = lanes.computeIfAbsent(key, ignored -> new Lane(key));
            if (admission.request.runId().equals(lane.activeRunId)
                || !lane.queuedRunIds.add(admission.request.runId())) {
                // A direct durable redispatch may race the creator, but pendingRuns deduplication
                // normally prevents this branch. Never enqueue a second copy.
                releasePending(admission.runKey);
                admission.state = AdmissionState.COMMITTED;
                return HarnessScheduleResult.ALREADY_SCHEDULED;
            }
            lane.queue.addLast(admission.request);
            admission.state = AdmissionState.COMMITTED;
            markReady(lane, false);
            dispatches = reserveGlobally();
        }
        submit(dispatches);
        return HarnessScheduleResult.SCHEDULED;
    }

    private void close(PendingAdmission admission) {
        synchronized (monitor) {
            if (admission.state != AdmissionState.OPEN) {
                return;
            }
            admission.state = AdmissionState.RELEASED;
            releasePending(admission.runKey);
        }
    }

    private boolean knownRun(RunKey runKey) {
        if (pendingRuns.contains(runKey)) {
            return true;
        }
        Lane lane = lanes.get(runKey.sessionKey());
        return lane != null && runKey.runId().equals(lane.activeRunId);
    }

    private CapacityFailure capacityFailure(OwnerKey ownerKey) {
        int ownerPending = pendingRunsByOwner.getOrDefault(ownerKey, 0);
        if (ownerPending >= maxPendingRunsPerOwner) {
            return new CapacityFailure(HarnessSchedulerCapacityException.Scope.OWNER,
                maxPendingRunsPerOwner);
        }
        int tenantPending = pendingRunsByTenant.getOrDefault(ownerKey.tenantId(), 0);
        if (tenantPending >= maxPendingRunsPerTenant) {
            return new CapacityFailure(HarnessSchedulerCapacityException.Scope.TENANT,
                maxPendingRunsPerTenant);
        }
        if (pendingRunCount >= maxPendingRunsGlobal) {
            return new CapacityFailure(HarnessSchedulerCapacityException.Scope.GLOBAL,
                maxPendingRunsGlobal);
        }
        return null;
    }

    private void admitPending(RunKey runKey) {
        if (!pendingRuns.add(runKey)) {
            throw new IllegalStateException("Harness run already owns pending admission");
        }
        OwnerKey ownerKey = runKey.sessionKey().ownerKey();
        pendingRunsByOwner.merge(ownerKey, 1, Integer::sum);
        pendingRunsByTenant.merge(ownerKey.tenantId(), 1, Integer::sum);
        pendingRunCount++;
    }

    private void releasePending(RunKey runKey) {
        if (!pendingRuns.remove(runKey)) {
            return;
        }
        OwnerKey ownerKey = runKey.sessionKey().ownerKey();
        decrement(pendingRunsByOwner, ownerKey);
        decrement(pendingRunsByTenant, ownerKey.tenantId());
        if (pendingRunCount <= 0) {
            throw new IllegalStateException("Harness pending-run counter underflow");
        }
        pendingRunCount--;
    }

    private <K> void decrement(Map<K, Integer> counts, K key) {
        Integer count = counts.get(key);
        if (count == null || count < 1) {
            throw new IllegalStateException("Harness pending-run counter is inconsistent");
        }
        if (count == 1) {
            counts.remove(key);
        } else {
            counts.put(key, count - 1);
        }
    }

    private void submit(List<Dispatch> dispatches) {
        boolean rejectedAny = false;
        boolean acceptedAny = false;
        for (Dispatch dispatch : dispatches) {
            try {
                executor.execute(() -> processOne(dispatch));
                acceptedAny = true;
            } catch (RuntimeException rejected) {
                rejectedAny = true;
                rollbackReservation(dispatch);
                log.warn("Harness runner rejected owner {} session {} run {}; durable retry remains queued",
                    dispatch.key().ownerKey(), dispatch.key().sessionId(),
                    dispatch.request().runId(), rejected);
            }
        }
        synchronized (monitor) {
            if (acceptedAny && !rejectedAny) {
                retryAttempt = 0;
            }
        }
        if (rejectedAny) {
            scheduleRetry();
        }
    }

    private void processOne(Dispatch dispatch) {
        // Executor-queued work remains pending. Only crossing into the processor releases pending
        // admission, so an active run never consumes its owner's pending quota.
        synchronized (monitor) {
            releasePending(dispatch.runKey());
        }
        try {
            processor.process(dispatch.request());
        } catch (Throwable error) {
            log.error("Harness run processor failed for session {} run {}",
                dispatch.request().sessionId(), dispatch.request().runId(), error);
        } finally {
            List<Dispatch> next;
            synchronized (monitor) {
                OwnerKey ownerKey = dispatch.key().ownerKey();
                Lane lane = lanes.get(dispatch.key());
                OwnerQueue ownerQueue = ownerQueues.get(ownerKey);
                if (lane != null && dispatch.request().runId().equals(lane.activeRunId)) {
                    lane.activeRunId = null;
                }
                if (ownerQueue != null && ownerQueue.inFlight > 0) {
                    ownerQueue.inFlight--;
                }
                if (lane != null) {
                    markReady(lane, false);
                    retireIfIdle(lane);
                }
                markOwnerReady(ownerKey, false);
                next = reserveGlobally();
                retireOwnerIfIdle(ownerKey);
            }
            // Every completion advances the global owner queue, including work whose earlier
            // dispatch was rejected while a different owner occupied the executor.
            submit(next);
        }
    }

    private List<Dispatch> reserveGlobally() {
        List<Dispatch> result = new ArrayList<>();
        while (!readyOwners.isEmpty()) {
            OwnerKey ownerKey = readyOwners.removeFirst();
            readyOwnerIds.remove(ownerKey);
            OwnerQueue ownerQueue = ownerQueues.get(ownerKey);
            if (ownerQueue == null || ownerQueue.inFlight >= maxConcurrentRunsPerOwner) {
                continue;
            }
            Dispatch dispatch = reserveOne(ownerKey, ownerQueue);
            if (dispatch != null) {
                result.add(dispatch);
            }
            // Re-adding at the tail provides owner round-robin when a configurable owner limit
            // permits more than one active session.
            markOwnerReady(ownerKey, false);
        }
        return result;
    }

    private Dispatch reserveOne(OwnerKey ownerKey, OwnerQueue ownerQueue) {
        while (!ownerQueue.readySessions.isEmpty()) {
            SessionKey key = ownerQueue.readySessions.removeFirst();
            ownerQueue.readySessionIds.remove(key);
            Lane lane = lanes.get(key);
            if (lane == null || lane.activeRunId != null || lane.queue.isEmpty()) {
                continue;
            }
            HarnessRunRequest request = lane.queue.removeFirst();
            lane.queuedRunIds.remove(request.runId());
            lane.activeRunId = request.runId();
            ownerQueue.inFlight++;
            return new Dispatch(new RunKey(key, request.runId()), request);
        }
        retireOwnerIfIdle(ownerKey);
        return null;
    }

    private void rollbackReservation(Dispatch dispatch) {
        synchronized (monitor) {
            Lane lane = lanes.get(dispatch.key());
            OwnerQueue ownerQueue = ownerQueues.get(dispatch.key().ownerKey());
            if (lane == null || !dispatch.request().runId().equals(lane.activeRunId)) {
                return;
            }
            lane.activeRunId = null;
            lane.queue.addFirst(dispatch.request());
            lane.queuedRunIds.add(dispatch.request().runId());
            if (ownerQueue != null && ownerQueue.inFlight > 0) {
                ownerQueue.inFlight--;
            }
            markReady(lane, true);
        }
    }

    private void scheduleRetry() {
        long delay;
        synchronized (monitor) {
            if (retryScheduled || readyOwnerIds.isEmpty()) {
                return;
            }
            retryScheduled = true;
            delay = retryDelayForAttempt(retryAttempt);
            retryAttempt = Math.min(retryAttempt + 1, 16);
        }
        try {
            retryScheduler.schedule(this::retryReadyOwners, delay);
        } catch (RuntimeException schedulingFailure) {
            synchronized (monitor) {
                retryScheduled = false;
            }
            log.warn("Harness runner retry scheduling failed; durable maintenance can redispatch",
                schedulingFailure);
        }
    }

    private void retryReadyOwners() {
        List<Dispatch> dispatches;
        synchronized (monitor) {
            retryScheduled = false;
            dispatches = reserveGlobally();
            if (dispatches.isEmpty()) {
                retryAttempt = 0;
            }
        }
        submit(dispatches);
    }

    private long retryDelayForAttempt(int attempt) {
        long delay = retryDelayMillis;
        for (int index = 0; index < Math.min(attempt, 16); index++) {
            delay = Math.min(MAX_RETRY_DELAY_MILLIS, delay * 2);
        }
        return delay;
    }

    private void markReady(Lane lane, boolean first) {
        if (lane.activeRunId != null || lane.queue.isEmpty()) {
            return;
        }
        OwnerQueue ownerQueue = ownerQueues.computeIfAbsent(lane.key.ownerKey(),
            ignored -> new OwnerQueue());
        if (ownerQueue.readySessionIds.add(lane.key)) {
            if (first) {
                ownerQueue.readySessions.addFirst(lane.key);
            } else {
                ownerQueue.readySessions.addLast(lane.key);
            }
        }
        markOwnerReady(lane.key.ownerKey(), false);
    }

    private void markOwnerReady(OwnerKey ownerKey, boolean first) {
        OwnerQueue ownerQueue = ownerQueues.get(ownerKey);
        if (ownerQueue == null || ownerQueue.inFlight >= maxConcurrentRunsPerOwner
            || ownerQueue.readySessions.isEmpty() || !readyOwnerIds.add(ownerKey)) {
            return;
        }
        if (first) {
            readyOwners.addFirst(ownerKey);
        } else {
            readyOwners.addLast(ownerKey);
        }
    }

    private void unmarkReady(Lane lane) {
        OwnerQueue ownerQueue = ownerQueues.get(lane.key.ownerKey());
        if (ownerQueue != null && ownerQueue.readySessionIds.remove(lane.key)) {
            ownerQueue.readySessions.remove(lane.key);
            if (ownerQueue.readySessions.isEmpty()) {
                unmarkOwnerReady(lane.key.ownerKey());
            }
        }
    }

    private void unmarkOwnerReady(OwnerKey ownerKey) {
        if (readyOwnerIds.remove(ownerKey)) {
            readyOwners.remove(ownerKey);
        }
    }

    private void retireIfIdle(Lane lane) {
        if (lane.activeRunId == null && lane.queue.isEmpty()) {
            unmarkReady(lane);
            lanes.remove(lane.key, lane);
            retireOwnerIfIdle(lane.key.ownerKey());
        }
    }

    private void retireOwnerIfIdle(OwnerKey ownerKey) {
        OwnerQueue ownerQueue = ownerQueues.get(ownerKey);
        if (ownerQueue == null || ownerQueue.inFlight != 0
            || !ownerQueue.readySessions.isEmpty()) {
            return;
        }
        boolean hasLane = lanes.keySet().stream()
            .anyMatch(key -> key.ownerKey().equals(ownerKey));
        if (!hasLane) {
            unmarkOwnerReady(ownerKey);
            ownerQueues.remove(ownerKey, ownerQueue);
        }
    }

    @FunctionalInterface
    interface RetryScheduler {
        void schedule(Runnable task, long delayMillis);
    }

    /** One-shot reservation. Closing an uncommitted token releases all pending counters. */
    public interface Admission extends AutoCloseable {

        HarnessScheduleResult commit();

        @Override
        void close();
    }

    private record OwnerKey(String tenantId, Long userId) {
    }

    private record SessionKey(OwnerKey ownerKey, String sessionId) {
        private static SessionKey of(HarnessRunRequest request) {
            return new SessionKey(new OwnerKey(request.owner().tenantId(),
                request.owner().userId()), request.sessionId());
        }
    }

    private record RunKey(SessionKey sessionKey, String runId) {
        private static RunKey of(HarnessRunRequest request) {
            return new RunKey(SessionKey.of(request), request.runId());
        }
    }

    private record Dispatch(RunKey runKey, HarnessRunRequest request) {
        private SessionKey key() {
            return runKey.sessionKey();
        }
    }

    private record CapacityFailure(HarnessSchedulerCapacityException.Scope scope, int limit) {
    }

    private enum AdmissionState {
        OPEN,
        COMMITTED,
        RELEASED
    }

    private final class PendingAdmission implements Admission {
        private final RunKey runKey;
        private final HarnessRunRequest request;
        private AdmissionState state = AdmissionState.OPEN;

        private PendingAdmission(RunKey runKey, HarnessRunRequest request) {
            this.runKey = runKey;
            this.request = request;
        }

        @Override
        public HarnessScheduleResult commit() {
            return HarnessScheduler.this.commit(this);
        }

        @Override
        public void close() {
            HarnessScheduler.this.close(this);
        }

        private void requireOpen(String action) {
            if (state != AdmissionState.OPEN) {
                throw new IllegalStateException("Harness scheduler admission cannot " + action
                    + " from " + state);
            }
        }
    }

    private final class ExistingAdmission implements Admission {
        private AdmissionState state = AdmissionState.OPEN;

        @Override
        public HarnessScheduleResult commit() {
            synchronized (monitor) {
                requireOpen("commit");
                state = AdmissionState.COMMITTED;
                return HarnessScheduleResult.ALREADY_SCHEDULED;
            }
        }

        @Override
        public void close() {
            synchronized (monitor) {
                if (state == AdmissionState.OPEN) {
                    state = AdmissionState.RELEASED;
                }
            }
        }

        private void requireOpen(String action) {
            if (state != AdmissionState.OPEN) {
                throw new IllegalStateException("Harness scheduler admission cannot " + action
                    + " from " + state);
            }
        }
    }

    private static final class Lane {
        private final SessionKey key;
        private final ArrayDeque<HarnessRunRequest> queue = new ArrayDeque<>();
        private final Set<String> queuedRunIds = new HashSet<>();
        private String activeRunId;

        private Lane(SessionKey key) {
            this.key = key;
        }
    }

    private static final class OwnerQueue {
        private final ArrayDeque<SessionKey> readySessions = new ArrayDeque<>();
        private final Set<SessionKey> readySessionIds = new HashSet<>();
        private int inFlight;
    }
}
