package org.ruoyi.service.coding.harness.event;

import jakarta.annotation.PreDestroy;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Durable-first event publisher with replay and non-blocking live delivery. Slow subscribers are
 * disconnected and can recover from their last sequence; the run worker is never blocked by SSE.
 */
@Service
public class HarnessEventHub {

    private static final int REPLAY_PAGE_SIZE = 1_000;
    private static final int DEFAULT_MAX_SUBSCRIBERS_PER_RUN = 32;
    private static final int DEFAULT_MAX_SUBSCRIBERS_PER_OWNER = 64;
    private static final int DEFAULT_MAX_SUBSCRIBERS_PER_TENANT = 256;
    private static final int DEFAULT_MAX_SUBSCRIBERS_GLOBAL = 512;
    private static final int DEFAULT_MAX_CALLBACK_CONCURRENCY = 32;
    private static final long DEFAULT_CALLBACK_TIMEOUT_MILLIS = 5_000;
    private static final int RUN_LOCK_STRIPES = 256;
    private static final int MAX_LIFECYCLE_EVENT_SCAN = 100_000;
    private static final int MAX_LIFECYCLE_MESSAGE_SCAN = 1_000_000;
    private static final int MAX_TERMINAL_DIAGNOSTIC_LENGTH = 16_384;
    private static final Set<String> FAILED_TERMINAL_CODES = Set.of(
        "TERMINAL_STATE_RECOVERED", "PROVIDER_START_FAILURE", "PROVIDER_REJECTED");
    private static final Set<String> CANCELLED_TERMINAL_CODES = Set.of(
        "TERMINAL_STATE_RECOVERED", "USER_CANCELLED");
    private static final Pattern CANONICAL_DECIMAL_REVISION = Pattern.compile(
        "(?:0|[1-9][0-9]{0,18})");
    private static final Pattern LEGACY_RANDOM_EVENT_ID = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    private final HarnessStore store;
    private final HarnessModelLifecycleIntegrity lifecycleIntegrity;
    private final Executor deliveryExecutor;
    private final int subscriberQueueCapacity;
    private final int maxSubscribersPerRun;
    private final int maxSubscribersPerOwner;
    private final int maxSubscribersPerTenant;
    private final int maxSubscribersGlobal;
    private final long callbackTimeoutMillis;
    private final ExecutorService callbackExecutor;
    private final ExecutorService failureCallbackExecutor;
    private final ConcurrentMap<RunKey, CopyOnWriteArrayList<Subscriber>> subscribers =
        new ConcurrentHashMap<>();
    private final Object subscriberMonitor = new Object();
    private final Map<OwnerKey, Integer> ownerSubscriberCounts = new HashMap<>();
    private final Map<String, Integer> tenantSubscriberCounts = new HashMap<>();
    private final AtomicInteger subscriberCount = new AtomicInteger();
    private final AtomicBoolean hubClosed = new AtomicBoolean(false);
    private final Object[] runLocks = createRunLocks();

    public HarnessEventHub(HarnessStore store,
                           @Qualifier("codingHarnessDeliveryExecutor") Executor deliveryExecutor,
                           @Value("${coding.harness.event-subscriber-capacity:2048}")
                           int subscriberQueueCapacity) {
        this(store, deliveryExecutor, subscriberQueueCapacity,
            DEFAULT_MAX_SUBSCRIBERS_PER_RUN, DEFAULT_MAX_SUBSCRIBERS_PER_OWNER,
            DEFAULT_MAX_SUBSCRIBERS_PER_TENANT, DEFAULT_MAX_SUBSCRIBERS_GLOBAL,
            DEFAULT_MAX_CALLBACK_CONCURRENCY, DEFAULT_CALLBACK_TIMEOUT_MILLIS);
    }

    public HarnessEventHub(HarnessStore store,
                           @Qualifier("codingHarnessDeliveryExecutor") Executor deliveryExecutor,
                           @Value("${coding.harness.event-subscriber-capacity:2048}")
                           int subscriberQueueCapacity,
                           @Value("${coding.harness.event-subscriber-limit-per-run:32}")
                           int maxSubscribersPerRun,
                           @Value("${coding.harness.event-subscriber-limit-global:512}")
                           int maxSubscribersGlobal) {
        this(store, deliveryExecutor, subscriberQueueCapacity, maxSubscribersPerRun,
            Math.min(DEFAULT_MAX_SUBSCRIBERS_PER_OWNER, maxSubscribersGlobal),
            Math.min(DEFAULT_MAX_SUBSCRIBERS_PER_TENANT, maxSubscribersGlobal),
            maxSubscribersGlobal, DEFAULT_MAX_CALLBACK_CONCURRENCY,
            DEFAULT_CALLBACK_TIMEOUT_MILLIS);
    }

    public HarnessEventHub(HarnessStore store,
                           @Qualifier("codingHarnessDeliveryExecutor") Executor deliveryExecutor,
                           @Value("${coding.harness.event-subscriber-capacity:2048}")
                           int subscriberQueueCapacity,
                           @Value("${coding.harness.event-subscriber-limit-per-run:32}")
                           int maxSubscribersPerRun,
                           @Value("${coding.harness.event-subscriber-limit-global:512}")
                           int maxSubscribersGlobal,
                           @Value("${coding.harness.event-callback-max-concurrency:32}")
                           int maxCallbackConcurrency,
                           @Value("${coding.harness.event-callback-timeout-millis:5000}")
                           long callbackTimeoutMillis) {
        this(store, deliveryExecutor, subscriberQueueCapacity, maxSubscribersPerRun,
            Math.min(DEFAULT_MAX_SUBSCRIBERS_PER_OWNER, maxSubscribersGlobal),
            Math.min(DEFAULT_MAX_SUBSCRIBERS_PER_TENANT, maxSubscribersGlobal),
            maxSubscribersGlobal, maxCallbackConcurrency, callbackTimeoutMillis);
    }

    @Autowired
    public HarnessEventHub(HarnessStore store,
                           @Qualifier("codingHarnessDeliveryExecutor") Executor deliveryExecutor,
                           @Value("${coding.harness.event-subscriber-capacity:2048}")
                           int subscriberQueueCapacity,
                           @Value("${coding.harness.event-subscriber-limit-per-run:32}")
                           int maxSubscribersPerRun,
                           @Value("${coding.harness.event-subscriber-limit-per-owner:64}")
                           int maxSubscribersPerOwner,
                           @Value("${coding.harness.event-subscriber-limit-per-tenant:256}")
                           int maxSubscribersPerTenant,
                           @Value("${coding.harness.event-subscriber-limit-global:512}")
                           int maxSubscribersGlobal,
                           @Value("${coding.harness.event-callback-max-concurrency:32}")
                           int maxCallbackConcurrency,
                           @Value("${coding.harness.event-callback-timeout-millis:5000}")
                           long callbackTimeoutMillis) {
        this.store = Objects.requireNonNull(store, "store");
        this.lifecycleIntegrity = new HarnessModelLifecycleIntegrity(store);
        this.deliveryExecutor = Objects.requireNonNull(deliveryExecutor, "deliveryExecutor");
        if (subscriberQueueCapacity < 16) {
            throw new IllegalArgumentException("Harness subscriber queue capacity must be at least 16");
        }
        if (maxSubscribersPerRun < 1 || maxSubscribersPerOwner < 1
            || maxSubscribersPerTenant < 1 || maxSubscribersGlobal < 1) {
            throw new IllegalArgumentException("Harness subscriber limits must be positive");
        }
        if (maxCallbackConcurrency < 1 || callbackTimeoutMillis < 1) {
            throw new IllegalArgumentException("Harness event callback limits must be positive");
        }
        this.subscriberQueueCapacity = subscriberQueueCapacity;
        this.maxSubscribersPerRun = maxSubscribersPerRun;
        this.maxSubscribersPerOwner = maxSubscribersPerOwner;
        this.maxSubscribersPerTenant = maxSubscribersPerTenant;
        this.maxSubscribersGlobal = maxSubscribersGlobal;
        this.callbackTimeoutMillis = callbackTimeoutMillis;
        int boundedCallbackConcurrency = Math.min(maxCallbackConcurrency, maxSubscribersGlobal);
        this.callbackExecutor = createCallbackExecutor(boundedCallbackConcurrency);
        this.failureCallbackExecutor = createFailureCallbackExecutor(
            Math.min(4, boundedCallbackConcurrency), maxSubscribersGlobal);
    }

    public HarnessEvent publish(HarnessOwner owner, HarnessEvent draft) {
        Objects.requireNonNull(draft, "draft");
        RunKey key = RunKey.of(owner, draft.sessionId(), draft.runId());
        if (!isTerminalEventType(draft.type())) {
            validateLifecycleDraftIfApplicable(owner, key, draft);
            HarnessEvent durable = store.appendEvent(owner, draft);
            fanout(key, durable);
            return durable;
        }
        synchronized (runLock(key)) {
            HarnessRunState run = store.findRun(owner, key.sessionId(), key.runId()).orElse(null);
            if (run == null) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Terminal event has no durable run");
            }
            if (!run.eventOutbox().isEmpty()) {
                throw new IllegalStateException("Terminal event cannot overtake the durable "
                    + "event outbox for run " + key.runId());
            }
            requirePublishableTerminalDraft(run, draft);
            HarnessEvent existing = findUniqueTerminalEvent(key, owner, run);
            if (existing != null) {
                requireCompatibleTerminalPayload(existing, draft);
                return existing;
            }
            HarnessEvent durable = store.appendEvent(owner, draft);
            fanout(key, durable);
            return durable;
        }
    }

    /**
     * Publishes an outbox draft exactly once by its stable event id.
     *
     * <p>A retry after the ledger append but before the run snapshot acknowledges the draft finds
     * and returns the durable event. Reusing an id for a different logical event fails closed.</p>
     */
    public HarnessEvent publishIdempotent(HarnessOwner owner, HarnessEvent draft) {
        Objects.requireNonNull(draft, "draft");
        RunKey key = RunKey.of(owner, draft.sessionId(), draft.runId());
        synchronized (runLock(key)) {
            HarnessRunState run = store.findRun(owner, key.sessionId(), key.runId()).orElse(null);
            if (run != null && lifecycleIntegrity.isLifecycleEvent(run, draft)) {
                validateLifecycleDraft(run, draft);
            }
            if (isTerminalEventType(draft.type())) {
                requirePublishableTerminalDraft(run, draft);
            }
            HarnessEvent existing = findEventById(key, owner, draft.eventId());
            if (existing != null) {
                requireSameLogicalEvent(existing, draft);
                if (isTerminalEventType(draft.type())) {
                    HarnessEvent terminal = findUniqueTerminalEvent(key, owner, run);
                    if (terminal == null || terminal.sequence() != existing.sequence()) {
                        throw new HarnessModelLifecycleIntegrityException(
                            "Stable terminal event is not the unique terminal sequence");
                    }
                }
                return existing;
            }
            if (isTerminalEventType(draft.type())) {
                // Legacy crash repair used a random id while terminal events were deduplicated by
                // type. A newly stable outbox draft must acknowledge that durable event instead
                // of creating a second terminal signal for the same run.
                HarnessEvent terminal = findUniqueTerminalEvent(key, owner, run);
                if (terminal != null) {
                    requireCompatibleTerminalPayload(terminal, draft);
                    return terminal;
                }
            }
            HarnessEvent durable = store.appendEvent(owner, draft);
            fanout(key, durable);
            return durable;
        }
    }

    private void fanout(RunKey key, HarnessEvent durable) {
        List<Subscriber> current = subscribers.get(key);
        if (current != null) {
            for (Subscriber subscriber : current) {
                subscriber.offer(durable);
            }
        }
    }

    public HarnessEventSubscription subscribe(HarnessOwner owner, String sessionId, String runId,
                                              long afterSequence,
                                              Consumer<HarnessEvent> consumer,
                                              Consumer<Throwable> errorConsumer) {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(errorConsumer, "errorConsumer");
        if (afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence cannot be negative");
        }
        RunKey key = RunKey.of(owner, sessionId, runId);
        Subscriber subscriber = new Subscriber(key, owner, afterSequence, consumer, errorConsumer);
        try {
            // Validate/repair the complete historical terminal boundary before admission. A
            // forged early or wrong terminal event must never reach the controller callback,
            // because that callback closes SSE as soon as it observes any run.* terminal type.
            ensureTerminalEvent(owner, sessionId, runId);
            synchronized (runLock(key)) {
                HarnessRunState run = store.findRun(owner, key.sessionId(), key.runId())
                    .orElse(null);
                findUniqueTerminalEvent(key, owner, run);
            }
            admit(subscriber);
            deliveryExecutor.execute(subscriber::replayThenDrain);
        } catch (RuntimeException admissionFailure) {
            // Terminal repair and initial scheduling are both part of admission. Never retain a
            // subscriber which has no executor task capable of replaying its durable cursor.
            subscriber.close();
            throw admissionFailure;
        }
        return subscriber;
    }

    /** Repairs the state-to-event crash window for SSE and polling/audit callers alike. */
    public void ensureTerminalEvent(HarnessOwner owner, String sessionId, String runId) {
        RunKey key = RunKey.of(owner, sessionId, runId);
        synchronized (runLock(key)) {
            HarnessRunState run = store.findRun(owner, key.sessionId(), key.runId()).orElse(null);
            HarnessEvent existing = findUniqueTerminalEvent(key, owner, run);
            if (existing != null) {
                return;
            }
            if (run == null || !run.status().isTerminal()) {
                return;
            }
            if (run.modelEffect() != null
                && run.modelEffect().status()
                == org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus.PENDING) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Terminal run retains a pending model effect");
            }
            if (!run.eventOutbox().isEmpty()) {
                return;
            }
            if (!lifecycleIntegrity.isComplete(run, MAX_LIFECYCLE_EVENT_SCAN,
                MAX_LIFECYCLE_MESSAGE_SCAN)) {
                return;
            }
            String terminalType = terminalEventType(run.status());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", run.status().name());
            data.put("revision", run.revision());
            data.put("code", "TERMINAL_STATE_RECOVERED");
            publishIdempotent(owner, HarnessEvent.draftWithId(
                "run-terminal-repair:" + run.runId() + ":" + terminalType,
                key.sessionId(), key.runId(), terminalType,
                null, null, null, data, run.updatedAt()));
        }
    }

    private String terminalEventType(HarnessRunStatus status) {
        return switch (status) {
            case COMPLETED -> "run.completed";
            case FAILED -> "run.failed";
            case CANCELLED -> "run.cancelled";
            default -> throw new IllegalArgumentException("Run is not terminal: " + status);
        };
    }

    private boolean isTerminalEventType(String type) {
        return "run.completed".equals(type) || "run.failed".equals(type)
            || "run.cancelled".equals(type);
    }

    private void validateLifecycleDraftIfApplicable(HarnessOwner owner, RunKey key,
                                                    HarnessEvent draft) {
        HarnessRunState run = store.findRun(owner, key.sessionId(), key.runId()).orElse(null);
        if (run != null && lifecycleIntegrity.isLifecycleEvent(run, draft)) {
            validateLifecycleDraft(run, draft);
        }
    }

    private void validateLifecycleDraft(HarnessRunState run, HarnessEvent draft) {
        lifecycleIntegrity.validateLifecycleEvent(run, draft,
            run.modelEffect().status()
                == org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus.SETTLED
                ? lifecycleIntegrity.requireExactSettledResponse(run,
                    MAX_LIFECYCLE_MESSAGE_SCAN) : null);
    }

    private void requireTerminalLifecycleComplete(HarnessRunState run) {
        if (!lifecycleIntegrity.isComplete(run, MAX_LIFECYCLE_EVENT_SCAN,
            MAX_LIFECYCLE_MESSAGE_SCAN)) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event cannot precede the complete model lifecycle for run "
                    + run.runId());
        }
    }

    private void requirePublishableTerminalDraft(HarnessRunState run, HarnessEvent draft) {
        if (run == null) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event has no durable run");
        }
        if (!run.eventOutbox().isEmpty()
            && !draft.eventId().equals(run.eventOutbox().get(0).event().eventId())) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event cannot overtake an earlier durable outbox entry");
        }
        requireTerminalEventMatchesRun(run, draft, false);
        requireTerminalLifecycleComplete(run);
    }

    private HarnessEvent findEventById(RunKey key, HarnessOwner owner, String eventId) {
        long cursor = 0;
        HarnessEvent found = null;
        while (true) {
            List<HarnessEvent> page = store.readEvents(owner, key.sessionId(), key.runId(),
                cursor, REPLAY_PAGE_SIZE);
            long nextCursor = cursor;
            for (HarnessEvent event : page) {
                if (eventId.equals(event.eventId())) {
                    if (found != null) {
                        throw new HarnessModelLifecycleIntegrityException(
                            "Duplicate stable Harness event id: " + eventId);
                    }
                    found = event;
                }
                nextCursor = Math.max(nextCursor, event.sequence());
            }
            if (page.size() < REPLAY_PAGE_SIZE) {
                return found;
            }
            if (nextCursor <= cursor) {
                throw new IllegalStateException("Harness event ledger did not advance for run "
                    + key.runId());
            }
            cursor = nextCursor;
        }
    }

    /**
     * Finds and validates the single terminal ledger boundary. All three terminal types share one
     * cardinality constraint; finding the expected type cannot hide an earlier wrong-type event.
     */
    private HarnessEvent findUniqueTerminalEvent(RunKey key, HarnessOwner owner,
                                                  HarnessRunState run) {
        long cursor = 0;
        HarnessEvent terminal = null;
        while (true) {
            List<HarnessEvent> page = store.readEvents(owner, key.sessionId(), key.runId(),
                cursor, REPLAY_PAGE_SIZE);
            long nextCursor = cursor;
            for (HarnessEvent event : page) {
                if (isTerminalEventType(event.type())) {
                    if (terminal != null) {
                        throw new HarnessModelLifecycleIntegrityException(
                            "Harness run has more than one terminal event sequence");
                    }
                    terminal = event;
                }
                nextCursor = Math.max(nextCursor, event.sequence());
            }
            if (page.size() < REPLAY_PAGE_SIZE) {
                break;
            }
            if (nextCursor <= cursor) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Harness terminal event scan did not advance for run " + key.runId());
            }
            cursor = nextCursor;
        }
        if (terminal != null) {
            if (run == null) {
                throw new HarnessModelLifecycleIntegrityException(
                    "Historical terminal event has no durable run");
            }
            requireTerminalEventMatchesRun(run, terminal, true);
            requireTerminalAfterModelLifecycle(run, terminal);
        }
        return terminal;
    }

    private void requireTerminalEventMatchesRun(HarnessRunState run, HarnessEvent event,
                                                boolean historical) {
        String expectedType;
        try {
            expectedType = terminalEventType(run.status());
        } catch (IllegalArgumentException nonTerminal) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event is bound to a non-terminal run");
        }
        if (!expectedType.equals(event.type())) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event type conflicts with the durable run status");
        }
        if (event.schemaVersion() != HarnessEvent.CURRENT_SCHEMA_VERSION
            || !run.sessionId().equals(event.sessionId())
            || !run.runId().equals(event.runId())
            || event.stepId() != null || event.toolCallId() != null
            || event.approvalId() != null
            || (historical ? event.sequence() <= 0 : event.sequence() != 0)) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event has an invalid schema or envelope");
        }

        BigInteger revision = terminalRevision(run, event, historical);
        String runStatePrefix = "run-state:" + run.runId() + ":" + event.type() + ":";
        if (event.eventId().startsWith("run-state:")
            && !event.eventId().equals(runStatePrefix + revision)) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event id conflicts with its durable revision");
        }
        String repairId = "run-terminal-repair:" + run.runId() + ":" + event.type();
        if (event.eventId().startsWith("run-terminal-repair:")
            && (!repairId.equals(event.eventId())
            || !"TERMINAL_STATE_RECOVERED".equals(event.data().get("code")))) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal repair event has an invalid stable identity");
        }
        requireTerminalPayloadShape(event, historical);
    }

    private BigInteger terminalRevision(HarnessRunState run, HarnessEvent event,
                                        boolean historical) {
        Object value = event.data().get("revision");
        BigInteger revision;
        if (value instanceof Number number && isIntegralNumber(number)) {
            revision = integralValue(number);
        } else if (value instanceof String text
            && CANONICAL_DECIMAL_REVISION.matcher(text).matches()) {
            // The application ObjectMapper intentionally serializes Long values as strings for
            // JavaScript precision. Historical ledgers therefore contain canonical decimal text
            // even though the in-memory event producer supplied a numeric revision.
            revision = new BigInteger(text);
        } else if (isLegacyCancelledTerminal(event, historical)) {
            // The first cancellation format predated terminal revisions. Its exact, UUID-v4
            // status-only envelope is accepted for read compatibility only; new publication and
            // every other missing-revision shape remain fail-closed.
            revision = BigInteger.ZERO;
        } else {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event revision must be an integer");
        }
        if (revision.signum() < 0
            || revision.compareTo(BigInteger.valueOf(run.revision())) > 0) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event revision conflicts with the durable run revision");
        }
        return revision;
    }

    private void requireTerminalPayloadShape(HarnessEvent event, boolean historical) {
        Map<String, Object> data = event.data();
        if (!event.type().substring("run.".length()).toUpperCase()
            .equals(data.get("status"))) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event status conflicts with its type");
        }
        Set<String> keys = data.keySet();
        if (isLegacyCancelledTerminal(event, historical)) {
            return;
        }
        Set<String> base = Set.of("status", "revision");
        if (keys.equals(base)) {
            return;
        }
        if ("run.completed".equals(event.type())) {
            if (keys.equals(Set.of("status", "revision", "terminalReportMessageId"))) {
                requireBoundedString(data.get("terminalReportMessageId"), 128,
                    "terminalReportMessageId");
                return;
            }
            if (keys.equals(Set.of("status", "revision", "code"))
                && "TERMINAL_STATE_RECOVERED".equals(data.get("code"))) {
                return;
            }
        } else if ("run.failed".equals(event.type())) {
            if (keys.equals(Set.of("status", "revision", "message"))) {
                requireBoundedString(data.get("message"), MAX_TERMINAL_DIAGNOSTIC_LENGTH,
                    "message");
                return;
            }
            if (keys.equals(Set.of("status", "revision", "reason"))) {
                requireBoundedString(data.get("reason"), MAX_TERMINAL_DIAGNOSTIC_LENGTH,
                    "reason");
                return;
            }
            if (keys.equals(Set.of("status", "revision", "code"))
                && FAILED_TERMINAL_CODES.contains(data.get("code"))) {
                return;
            }
            if (keys.equals(Set.of("status", "revision", "code", "details"))
                && "PROVIDER_REJECTED".equals(data.get("code"))
                && validLegacyProviderDetails(data.get("details"))) {
                return;
            }
        } else if ("run.cancelled".equals(event.type())) {
            if (keys.equals(Set.of("status", "revision", "reason"))) {
                requireBoundedString(data.get("reason"), MAX_TERMINAL_DIAGNOSTIC_LENGTH,
                    "reason");
                return;
            }
            if (keys.equals(Set.of("status", "revision", "code"))
                && CANCELLED_TERMINAL_CODES.contains(data.get("code"))) {
                return;
            }
        }
        throw new HarnessModelLifecycleIntegrityException(
            "Terminal event contains an unknown or invalid payload shape");
    }

    private boolean isLegacyCancelledTerminal(HarnessEvent event, boolean historical) {
        return historical
            && "run.cancelled".equals(event.type())
            && LEGACY_RANDOM_EVENT_ID.matcher(event.eventId()).matches()
            && event.data().equals(Map.of("status", "CANCELLED"));
    }

    private boolean validLegacyProviderDetails(Object value) {
        if (!(value instanceof Map<?, ?> details)
            || !details.keySet().equals(Set.of("attempt"))) {
            return false;
        }
        Object attempt = details.get("attempt");
        return attempt instanceof Number number && isIntegralNumber(number)
            && integralValue(number).signum() >= 0;
    }

    private void requireBoundedString(Object value, int maxLength, String field) {
        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {
            throw new HarnessModelLifecycleIntegrityException(
                "Terminal event " + field + " is invalid");
        }
    }

    private void requireTerminalAfterModelLifecycle(HarnessRunState run,
                                                    HarnessEvent terminal) {
        var effect = run.modelEffect();
        if (effect == null) {
            return;
        }
        HarnessEvent modelTerminal = lifecycleIntegrity.requireHistoricalTerminalMarker(run,
                MAX_LIFECYCLE_EVENT_SCAN, MAX_LIFECYCLE_MESSAGE_SCAN)
            .orElseThrow(() -> new HarnessModelLifecycleIntegrityException(
                "Terminal run is missing its model lifecycle terminal marker"));
        if (terminal.sequence() <= modelTerminal.sequence()) {
            throw new HarnessModelLifecycleIntegrityException(
                "Run terminal event does not follow the model lifecycle terminal marker");
        }
    }

    private void requireSameLogicalEvent(HarnessEvent durable, HarnessEvent draft) {
        if (durable.schemaVersion() != draft.schemaVersion()
            || !durable.eventId().equals(draft.eventId())
            || !durable.sessionId().equals(draft.sessionId())
            || !durable.runId().equals(draft.runId())
            || durable.timestamp() != draft.timestamp()
            || !durable.type().equals(draft.type())
            || !Objects.equals(durable.stepId(), draft.stepId())
            || !Objects.equals(durable.toolCallId(), draft.toolCallId())
            || !Objects.equals(durable.approvalId(), draft.approvalId())
            || !logicalValueEquals(durable.data(), draft.data())) {
            throw new IllegalStateException("Harness event id is bound to a different payload: "
                + draft.eventId());
        }
    }

    /** Legacy terminal ids may differ, but their complete logical payload may not. */
    private void requireCompatibleTerminalPayload(HarnessEvent durable, HarnessEvent draft) {
        if (durable.schemaVersion() != draft.schemaVersion()
            || !durable.sessionId().equals(draft.sessionId())
            || !durable.runId().equals(draft.runId())
            || !durable.type().equals(draft.type())
            || !Objects.equals(durable.stepId(), draft.stepId())
            || !Objects.equals(durable.toolCallId(), draft.toolCallId())
            || !Objects.equals(durable.approvalId(), draft.approvalId())
            || !logicalValueEquals(durable.data(), draft.data())) {
            throw new IllegalStateException("Terminal event type is bound to a different payload: "
                + draft.type());
        }
    }

    /**
     * Compares persisted event data by its logical JSON value. Jackson may materialize a small
     * integral draft value as {@link Integer} after reload even though the producer supplied a
     * {@link Long}; that representation change must not turn an idempotent replay into a payload
     * conflict.
     */
    private boolean logicalValueEquals(Object durable, Object draft) {
        if (Objects.equals(durable, draft)) {
            return true;
        }
        if (durable instanceof Number durableNumber && draft instanceof Number draftNumber) {
            // JSON reloads commonly narrow Long to Integer. Normalize only integral wrappers;
            // never make a floating-point/string representation loosely compatible with one.
            return isIntegralNumber(durableNumber) && isIntegralNumber(draftNumber)
                && integralValue(durableNumber).equals(integralValue(draftNumber));
        }
        if (durable instanceof Map<?, ?> durableMap && draft instanceof Map<?, ?> draftMap) {
            if (!durableMap.keySet().equals(draftMap.keySet())) {
                return false;
            }
            for (Object key : durableMap.keySet()) {
                if (!logicalValueEquals(durableMap.get(key), draftMap.get(key))) {
                    return false;
                }
            }
            return true;
        }
        if (durable instanceof List<?> durableList && draft instanceof List<?> draftList) {
            if (durableList.size() != draftList.size()) {
                return false;
            }
            for (int index = 0; index < durableList.size(); index++) {
                if (!logicalValueEquals(durableList.get(index), draftList.get(index))) {
                    return false;
                }
            }
            return true;
        }
        if (durable != null && draft != null
            && durable.getClass().isArray() && draft.getClass().isArray()) {
            int length = Array.getLength(durable);
            if (length != Array.getLength(draft)) {
                return false;
            }
            for (int index = 0; index < length; index++) {
                if (!logicalValueEquals(Array.get(durable, index), Array.get(draft, index))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isIntegralNumber(Number value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long || value instanceof BigInteger;
    }

    private BigInteger integralValue(Number value) {
        return value instanceof BigInteger bigInteger
            ? bigInteger : BigInteger.valueOf(value.longValue());
    }

    private Object runLock(RunKey key) {
        return runLocks[Math.floorMod(key.hashCode(), runLocks.length)];
    }

    private Object[] createRunLocks() {
        Object[] locks = new Object[RUN_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }
        return locks;
    }

    public int subscriberCount() {
        return subscriberCount.get();
    }

    public int subscriberCount(HarnessOwner owner) {
        Objects.requireNonNull(owner, "owner");
        synchronized (subscriberMonitor) {
            return ownerSubscriberCounts.getOrDefault(OwnerKey.of(owner), 0);
        }
    }

    public int tenantSubscriberCount(String tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        synchronized (subscriberMonitor) {
            return tenantSubscriberCounts.getOrDefault(tenantId, 0);
        }
    }

    @PreDestroy
    public void close() {
        if (!hubClosed.compareAndSet(false, true)) {
            return;
        }
        synchronized (subscriberMonitor) {
            List<Subscriber> admitted = subscribers.values().stream()
                .flatMap(List::stream)
                .toList();
            admitted.forEach(Subscriber::close);
            subscribers.clear();
            ownerSubscriberCounts.clear();
            tenantSubscriberCounts.clear();
            subscriberCount.set(0);
        }
        callbackExecutor.shutdownNow();
        failureCallbackExecutor.shutdownNow();
    }

    private ExecutorService createCallbackExecutor(int maxConcurrency) {
        AtomicInteger threadIds = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                "coding-harness-event-callback-" + threadIds.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(0, maxConcurrency, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(), threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    private ExecutorService createFailureCallbackExecutor(int maxConcurrency, int queueCapacity) {
        AtomicInteger threadIds = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                "coding-harness-event-failure-" + threadIds.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maxConcurrency, maxConcurrency,
            60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueCapacity), threadFactory,
            new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private void admit(Subscriber subscriber) {
        synchronized (subscriberMonitor) {
            if (hubClosed.get()) {
                throw new IllegalStateException("Harness event hub is closed");
            }
            CopyOnWriteArrayList<Subscriber> current = subscribers.get(subscriber.key);
            if (current != null && current.size() >= maxSubscribersPerRun) {
                throw new IllegalStateException("Harness event subscriber limit reached for run "
                    + subscriber.key.runId());
            }
            if (subscriberCount.get() >= maxSubscribersGlobal) {
                throw new IllegalStateException("Harness global event subscriber limit reached");
            }
            OwnerKey ownerKey = subscriber.key.ownerKey();
            if (tenantSubscriberCounts.getOrDefault(subscriber.key.tenantId(), 0)
                >= maxSubscribersPerTenant) {
                throw new IllegalStateException("Harness event subscriber limit reached for tenant "
                    + subscriber.key.tenantId());
            }
            if (ownerSubscriberCounts.getOrDefault(ownerKey, 0) >= maxSubscribersPerOwner) {
                throw new IllegalStateException("Harness event subscriber limit reached for owner");
            }
            if (current == null) {
                current = new CopyOnWriteArrayList<>();
                subscribers.put(subscriber.key, current);
            }
            current.add(subscriber);
            ownerSubscriberCounts.merge(ownerKey, 1, Integer::sum);
            tenantSubscriberCounts.merge(subscriber.key.tenantId(), 1, Integer::sum);
            subscriberCount.incrementAndGet();
        }
    }

    private void remove(Subscriber subscriber) {
        synchronized (subscriberMonitor) {
            CopyOnWriteArrayList<Subscriber> list = subscribers.get(subscriber.key);
            if (list != null && list.remove(subscriber)) {
                decrement(ownerSubscriberCounts, subscriber.key.ownerKey());
                decrement(tenantSubscriberCounts, subscriber.key.tenantId());
                subscriberCount.decrementAndGet();
                if (list.isEmpty()) {
                    subscribers.remove(subscriber.key, list);
                }
            }
        }
    }

    private <K> void decrement(Map<K, Integer> counts, K key) {
        Integer count = counts.get(key);
        if (count == null || count <= 1) {
            counts.remove(key);
        } else {
            counts.put(key, count - 1);
        }
    }

    private record OwnerKey(String tenantId, Long userId) {
        private static OwnerKey of(HarnessOwner owner) {
            return new OwnerKey(owner.tenantId(), owner.userId());
        }
    }

    private record RunKey(String tenantId, Long userId, String sessionId, String runId) {
        private static RunKey of(HarnessOwner owner, String sessionId, String runId) {
            return new RunKey(owner.tenantId(), owner.userId(), sessionId, runId);
        }

        private OwnerKey ownerKey() {
            return new OwnerKey(tenantId, userId);
        }
    }

    private final class Subscriber implements HarnessEventSubscription {
        private final RunKey key;
        private final HarnessOwner owner;
        private final Consumer<HarnessEvent> consumer;
        private final Consumer<Throwable> errorConsumer;
        private final ArrayBlockingQueue<HarnessEvent> queue =
            new ArrayBlockingQueue<>(subscriberQueueCapacity);
        private final AtomicLong lastDelivered;
        private final AtomicLong highestObserved;
        private final AtomicBoolean replayComplete = new AtomicBoolean(false);
        private final AtomicBoolean draining = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicReference<Future<?>> activeCallback = new AtomicReference<>();

        private Subscriber(RunKey key, HarnessOwner owner, long afterSequence,
                           Consumer<HarnessEvent> consumer, Consumer<Throwable> errorConsumer) {
            this.key = key;
            this.owner = owner;
            this.lastDelivered = new AtomicLong(afterSequence);
            this.highestObserved = new AtomicLong(afterSequence);
            this.consumer = consumer;
            this.errorConsumer = errorConsumer;
        }

        private void offer(HarnessEvent event) {
            try {
                if (closed.get() || event.sequence() <= lastDelivered.get()) {
                    return;
                }
                highestObserved.accumulateAndGet(event.sequence(), Math::max);
                if (!queue.offer(event)) {
                    fail(new IllegalStateException("Harness event subscriber fell behind at sequence "
                        + lastDelivered.get()));
                    return;
                }
                if (closed.get()) {
                    queue.clear();
                    return;
                }
                scheduleDrain();
            } catch (RuntimeException deliveryFailure) {
                // Fanout is best-effort notification of already durable state. It must never make
                // the run publisher observe a transport or executor failure.
                fail(deliveryFailure);
            }
        }

        private void replayThenDrain() {
            if (closed.get()) {
                return;
            }
            try {
                catchUpThrough(Long.MAX_VALUE);
                if (closed.get()) {
                    return;
                }
                replayComplete.set(true);
                scheduleDrain();
            } catch (Throwable error) {
                fail(error);
            }
        }

        private void scheduleDrain() {
            if (!replayComplete.get() || closed.get() || queue.isEmpty()
                || !draining.compareAndSet(false, true)) {
                return;
            }
            try {
                deliveryExecutor.execute(this::drain);
            } catch (RuntimeException rejected) {
                // execute() rejection can happen on a publisher thread. Convert it into an
                // isolated subscriber failure instead of throwing after the event was persisted.
                try {
                    fail(rejected);
                } finally {
                    draining.set(false);
                }
            }
        }

        private void drain() {
            try {
                HarnessEvent event;
                while (!closed.get() && (event = queue.poll()) != null) {
                    deliverAvailable(event);
                }
            } catch (Throwable error) {
                fail(error);
            } finally {
                draining.set(false);
                if (!closed.get() && !queue.isEmpty()) {
                    scheduleDrain();
                }
            }
        }

        private void deliverAvailable(HarnessEvent event) throws Throwable {
            long cursor = lastDelivered.get();
            if (closed.get() || event.sequence() <= cursor) {
                return;
            }
            if (event.sequence() == cursor + 1) {
                deliverNext(event);
            }

            // Concurrent publishers may return from durable append out of order. The highest
            // signal is only a watermark: fill every sequence from the durable ledger and never
            // advance the consumer cursor across a gap.
            long target = highestObserved.get();
            if (!closed.get() && lastDelivered.get() < target) {
                catchUpThrough(target);
            }
        }

        private void catchUpThrough(long targetSequence) throws Throwable {
            while (!closed.get() && lastDelivered.get() < targetSequence) {
                long cursor = lastDelivered.get();
                List<HarnessEvent> page = store.readEvents(owner, key.sessionId(), key.runId(),
                    cursor, REPLAY_PAGE_SIZE);
                if (page.isEmpty()) {
                    return;
                }

                boolean advanced = false;
                for (HarnessEvent event : page) {
                    long current = lastDelivered.get();
                    if (closed.get() || current >= targetSequence) {
                        return;
                    }
                    if (event.sequence() <= current) {
                        continue;
                    }
                    if (event.sequence() != current + 1) {
                        throw new IllegalStateException("Harness durable event sequence gap: expected "
                            + (current + 1) + " but found " + event.sequence());
                    }
                    deliverNext(event);
                    advanced = true;
                }
                if (!advanced) {
                    return;
                }
            }
        }

        private void deliverNext(HarnessEvent event) throws Throwable {
            long cursor = lastDelivered.get();
            if (closed.get() || event.sequence() != cursor + 1) {
                return;
            }
            if (invokeConsumer(event)) {
                lastDelivered.set(event.sequence());
            }
        }

        private boolean invokeConsumer(HarnessEvent event) throws Throwable {
            Future<?> callback = callbackExecutor.submit(() -> consumer.accept(event));
            activeCallback.set(callback);
            if (closed.get()) {
                callback.cancel(true);
            }
            try {
                callback.get(callbackTimeoutMillis, TimeUnit.MILLISECONDS);
                return !closed.get();
            } catch (CancellationException cancelled) {
                if (closed.get()) {
                    return false;
                }
                throw cancelled;
            } catch (ExecutionException failed) {
                throw failed.getCause();
            } catch (TimeoutException timedOut) {
                callback.cancel(true);
                throw new IllegalStateException("Harness event consumer timed out at sequence "
                    + event.sequence(), timedOut);
            } catch (InterruptedException interrupted) {
                callback.cancel(true);
                Thread.currentThread().interrupt();
                throw interrupted;
            } finally {
                activeCallback.compareAndSet(callback, null);
            }
        }

        private void fail(Throwable error) {
            if (closed.compareAndSet(false, true)) {
                cancelActiveCallback();
                remove(this);
                queue.clear();
                notifyFailure(error);
            }
        }

        private void notifyFailure(Throwable error) {
            try {
                failureCallbackExecutor.execute(() -> {
                    try {
                        errorConsumer.accept(error);
                    } catch (Throwable ignored) {
                        // A broken error callback is isolated from live event delivery.
                    }
                });
            } catch (RuntimeException ignored) {
                // Failure notification capacity is bounded. The subscriber is already removed,
                // and a saturated transport callback must never delay a durable event publisher.
            }
        }

        private void cancelActiveCallback() {
            Future<?> callback = activeCallback.get();
            if (callback != null) {
                callback.cancel(true);
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                cancelActiveCallback();
                remove(this);
                queue.clear();
            }
        }
    }
}
