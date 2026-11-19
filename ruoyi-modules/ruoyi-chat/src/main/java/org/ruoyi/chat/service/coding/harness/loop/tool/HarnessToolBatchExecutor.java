package org.ruoyi.chat.service.coding.harness.loop.tool;

import org.ruoyi.chat.service.coding.harness.tool.ToolBatchPlan;
import org.ruoyi.chat.service.coding.harness.tool.ToolBatchSlot;
import org.ruoyi.chat.service.coding.harness.tool.ToolExecutionGroup;
import org.ruoyi.chat.service.coding.harness.runtime.HarnessActiveTurnRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Runs only policy-ALLOW slots, respecting declared concurrency and per-call deadlines. */
@Service
public class HarnessToolBatchExecutor {

    private static final long CANCELLATION_GRACE_MILLIS = 10_000;

    private final ExecutorService executor;

    public HarnessToolBatchExecutor(
        @Qualifier("codingHarnessToolExecutor") ExecutorService executor) {
        this.executor = executor;
    }

    public HarnessToolBatchExecution execute(ToolBatchPlan plan,
                                             List<PreparedToolCall> preparedCalls,
                                             HarnessToolRegistry registry)
        throws InterruptedException {
        return execute(plan, preparedCalls, registry,
            HarnessActiveTurnRegistry.CancellationToken.none());
    }

    public HarnessToolBatchExecution execute(ToolBatchPlan plan,
                                             List<PreparedToolCall> preparedCalls,
                                             HarnessToolRegistry registry,
                                             HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        if (plan == null || registry == null) {
            throw new IllegalArgumentException("Tool batch plan and registry are required");
        }
        if (token == null) {
            throw new IllegalArgumentException("Tool batch cancellation token is required");
        }
        Map<String, PreparedToolCall> prepared = indexPrepared(preparedCalls);
        Map<String, HarnessToolExecutionResult> completed = new HashMap<>();

        for (ToolExecutionGroup group : plan.executionGroups()) {
            token.throwIfCancellationRequested();
            List<PreparedToolCall> calls = group.invocations().stream()
                .map(invocation -> requirePrepared(prepared, invocation.callId()))
                .toList();
            if (group.concurrent()) {
                executeConcurrent(calls, registry, completed, Long.MAX_VALUE, token);
            } else {
                PreparedToolCall call = calls.get(0);
                completed.put(call.source().toolCallId(),
                    executeOne(call, registry, Long.MAX_VALUE, token));
            }
        }

        List<HarnessToolExecutionResult> sourceOrdered = new ArrayList<>();
        for (ToolBatchSlot slot : plan.slots()) {
            if (!slot.executable()) {
                continue;
            }
            HarnessToolExecutionResult result = completed.get(slot.invocation().callId());
            if (result == null) {
                throw new IllegalStateException("Allowed tool slot has no execution result: "
                    + slot.invocation().callId());
            }
            sourceOrdered.add(result);
        }
        return new HarnessToolBatchExecution(sourceOrdered);
    }

    /** Executes a policy-authorized subset (including previously approved ASK slots). */
    public HarnessToolBatchExecution executePrepared(List<PreparedToolCall> preparedCalls,
                                                      HarnessToolRegistry registry)
        throws InterruptedException {
        return executePrepared(preparedCalls, registry, Long.MAX_VALUE,
            HarnessActiveTurnRegistry.CancellationToken.none());
    }

    /** Executes with an additional run-wide wall deadline shared by every slot in this batch. */
    public HarnessToolBatchExecution executePrepared(List<PreparedToolCall> preparedCalls,
                                                      HarnessToolRegistry registry,
                                                      long maxBatchMillis)
        throws InterruptedException {
        return executePrepared(preparedCalls, registry, maxBatchMillis,
            HarnessActiveTurnRegistry.CancellationToken.none());
    }

    /** Executes with the same cancellation barrier used by the owning durable run lane. */
    public HarnessToolBatchExecution executePrepared(List<PreparedToolCall> preparedCalls,
                                                      HarnessToolRegistry registry,
                                                      long maxBatchMillis,
                                                      HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        if (registry == null) {
            throw new IllegalArgumentException("Tool registry is required");
        }
        if (token == null) {
            throw new IllegalArgumentException("Tool batch cancellation token is required");
        }
        if (maxBatchMillis <= 0) {
            throw new IllegalArgumentException("Tool batch wall budget must be positive");
        }
        token.throwIfCancellationRequested();
        long batchDeadline = maxBatchMillis == Long.MAX_VALUE ? Long.MAX_VALUE
            : deadline(maxBatchMillis);
        List<PreparedToolCall> calls = preparedCalls == null
            ? List.of() : List.copyOf(preparedCalls);
        indexPrepared(calls);
        Map<String, HarnessToolExecutionResult> completed = new HashMap<>();
        List<PreparedToolCall> concurrent = new ArrayList<>();
        for (PreparedToolCall call : calls) {
            if (call.descriptor().concurrencySafe()) {
                concurrent.add(call);
                continue;
            }
            flushConcurrent(concurrent, registry, completed, batchDeadline, token);
            token.throwIfCancellationRequested();
            completed.put(call.source().toolCallId(),
                executeOne(call, registry, batchDeadline, token));
        }
        flushConcurrent(concurrent, registry, completed, batchDeadline, token);
        return new HarnessToolBatchExecution(calls.stream()
            .map(call -> completed.get(call.source().toolCallId()))
            .toList());
    }

    private void flushConcurrent(List<PreparedToolCall> calls, HarnessToolRegistry registry,
                                 Map<String, HarnessToolExecutionResult> completed,
                                 long batchDeadline,
                                 HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        if (!calls.isEmpty()) {
            executeConcurrent(List.copyOf(calls), registry, completed, batchDeadline, token);
            calls.clear();
        }
    }

    private void executeConcurrent(List<PreparedToolCall> calls, HarnessToolRegistry registry,
                                   Map<String, HarnessToolExecutionResult> completed,
                                   long batchDeadline,
                                   HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        Map<String, Pending> pending = new LinkedHashMap<>();
        try {
            for (PreparedToolCall call : calls) {
                token.throwIfCancellationRequested();
                long deadline = Math.min(deadline(call.descriptor().timeoutMillis()),
                    batchDeadline);
                if (deadline - System.nanoTime() <= 0) {
                    completed.put(call.source().toolCallId(), failure(call,
                        "run_deadline_exhausted", "Run wall-time expired before tool start"));
                    continue;
                }
                try {
                    TrackedFutureTask future = submit(call, registry, token);
                    pending.put(call.source().toolCallId(), new Pending(call, deadline, future));
                } catch (RejectedExecutionException rejected) {
                    completed.put(call.source().toolCallId(), failure(call,
                        "tool_executor_rejected", "Tool executor queue rejected the call"));
                }
            }
            for (Pending item : pending.values()) {
                completed.put(item.call().source().toolCallId(), await(item));
            }
        } catch (InterruptedException interrupted) {
            requireStoppedAfterCancellation(pending.values());
            throw interrupted;
        }
    }

    private HarnessToolExecutionResult executeOne(PreparedToolCall call,
                                                   HarnessToolRegistry registry,
                                                   long batchDeadline,
                                                   HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        Pending pending;
        try {
            token.throwIfCancellationRequested();
            long deadline = Math.min(deadline(call.descriptor().timeoutMillis()), batchDeadline);
            if (deadline - System.nanoTime() <= 0) {
                return failure(call, "run_deadline_exhausted",
                    "Run wall-time expired before tool start");
            }
            pending = new Pending(call, deadline, submit(call, registry, token));
        } catch (RejectedExecutionException rejected) {
            return failure(call, "tool_executor_rejected",
                "Tool executor queue rejected the call");
        }
        try {
            return await(pending);
        } catch (InterruptedException interrupted) {
            requireStoppedAfterCancellation(List.of(pending));
            throw interrupted;
        }
    }

    private HarnessToolExecutionResult await(Pending pending) throws InterruptedException {
        long remaining = pending.deadlineNanos() - System.nanoTime();
        if (remaining <= 0) {
            requireStoppedAfterCancellation(List.of(pending));
            return failure(pending.call(), "tool_timeout", "Tool call exceeded its deadline");
        }
        try {
            return pending.future().get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeout) {
            requireStoppedAfterCancellation(List.of(pending));
            return failure(pending.call(), "tool_timeout", "Tool call exceeded its deadline");
        } catch (ExecutionException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof InterruptedException interrupted) {
                throw interrupted;
            }
            return failure(pending.call(), "tool_execution_failed",
                cause == null || cause.getMessage() == null
                    ? "Tool execution failed" : cause.getMessage());
        }
    }

    private TrackedFutureTask submit(PreparedToolCall call, HarnessToolRegistry registry,
                                     HarnessActiveTurnRegistry.CancellationToken token)
        throws InterruptedException {
        token.throwIfCancellationRequested();
        TrackedFutureTask task = new TrackedFutureTask(() -> {
            try (HarnessActiveTurnRegistry.CancellationToken.Invocation ignored =
                     token.beginInvocation()) {
                return registry.execute(call.source());
            }
        });
        // Close the late-registration window: even when cancellation interrupts the lane between
        // its last run-state read and this submit, the shared token prevents executor admission.
        token.throwIfCancellationRequested();
        executor.execute(task);
        return task;
    }

    private void requireStoppedAfterCancellation(Iterable<Pending> pending) {
        List<TrackedFutureTask> tasks = new ArrayList<>();
        for (Pending item : pending) {
            item.future().cancel(true);
            tasks.add(item.future());
        }
        long deadline = deadline(CANCELLATION_GRACE_MILLIS);
        boolean interrupted = false;
        boolean allStopped = true;
        for (TrackedFutureTask task : tasks) {
            while (task.runnerActive() && System.nanoTime() < deadline) {
                try {
                    task.awaitRunnerExit(deadline - System.nanoTime());
                } catch (InterruptedException repeated) {
                    interrupted = true;
                }
            }
            allStopped &= !task.runnerActive();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!allStopped) {
            throw new ToolBatchCancellationTimeoutException(
                "A cancelled tool did not stop within " + CANCELLATION_GRACE_MILLIS + " ms");
        }
    }

    private Map<String, PreparedToolCall> indexPrepared(List<PreparedToolCall> calls) {
        Map<String, PreparedToolCall> indexed = new LinkedHashMap<>();
        if (calls != null) {
            for (PreparedToolCall call : calls) {
                if (call == null || indexed.putIfAbsent(call.source().toolCallId(), call) != null) {
                    throw new IllegalArgumentException("Prepared tool calls must have unique ids");
                }
            }
        }
        return indexed;
    }

    private PreparedToolCall requirePrepared(Map<String, PreparedToolCall> prepared,
                                             String callId) {
        PreparedToolCall call = prepared.get(callId);
        if (call == null) {
            throw new IllegalArgumentException("Tool batch references an unprepared call: " + callId);
        }
        return call;
    }

    private long deadline(long timeoutMillis) {
        try {
            return Math.addExact(System.nanoTime(),
                TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private HarnessToolExecutionResult failure(PreparedToolCall call, String code,
                                                String message) {
        return new HarnessToolExecutionResult(call.source().toolCallId(),
            call.source().toolName(), true, code, message, 0);
    }

    private record Pending(PreparedToolCall call, long deadlineNanos,
                           TrackedFutureTask future) { }

    private static final class TrackedFutureTask extends FutureTask<HarnessToolExecutionResult> {
        private final java.util.concurrent.CountDownLatch runnerExited =
            new java.util.concurrent.CountDownLatch(1);
        private volatile boolean runnerActive;

        private TrackedFutureTask(java.util.concurrent.Callable<HarnessToolExecutionResult> task) {
            super(task);
        }

        @Override
        public void run() {
            runnerActive = true;
            try {
                super.run();
            } finally {
                runnerActive = false;
                runnerExited.countDown();
            }
        }

        private boolean runnerActive() {
            return runnerActive;
        }

        private void awaitRunnerExit(long remainingNanos) throws InterruptedException {
            if (remainingNanos > 0) {
                runnerExited.await(remainingNanos, TimeUnit.NANOSECONDS);
            }
        }
    }
}
