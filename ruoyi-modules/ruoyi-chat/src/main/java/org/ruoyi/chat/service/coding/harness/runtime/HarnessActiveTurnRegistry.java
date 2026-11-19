package org.ruoyi.chat.service.coding.harness.runtime;

import org.ruoyi.chat.service.coding.harness.loop.model.ModelTurnHandle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Bridges authenticated cancellation to an in-flight provider or tool-batch handle. */
@Service
public class HarnessActiveTurnRegistry {

    private final ConcurrentMap<RunKey, CancellationHandle> active = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunKey, CancellationToken> cancellationTokens =
        new ConcurrentHashMap<>();

    /** Returns the stable cancellation barrier shared by the run lane and its tool workers. */
    public CancellationToken cancellationToken(HarnessRunRequest request) {
        RunKey key = RunKey.of(request);
        return cancellationTokens.computeIfAbsent(key, ignored -> new CancellationToken(true));
    }

    public Registration register(HarnessRunRequest request, ModelTurnHandle handle) {
        if (request == null || handle == null) {
            throw new IllegalArgumentException("Run request and model turn handle are required");
        }
        return register(request, handle::cancel);
    }

    /** Registers the run-lane thread while it synchronously owns cancellable tool futures. */
    public Registration registerInterruptible(HarnessRunRequest request, Thread thread) {
        if (thread == null) {
            throw new IllegalArgumentException("Tool batch thread is required");
        }
        return register(request, () -> {
            thread.interrupt();
            return true;
        });
    }

    private Registration register(HarnessRunRequest request, CancellationHandle handle) {
        RunKey key = RunKey.of(request);
        CancellationToken token = cancellationToken(request);
        CancellationHandle existing = active.putIfAbsent(key, handle);
        if (existing != null && existing != handle) {
            throw new IllegalStateException("A model turn is already active for run " + request.runId());
        }
        if (token.isCancellationRequested()) {
            handle.cancel();
        }
        return () -> active.remove(key, handle);
    }

    public boolean cancel(HarnessRunRequest request) {
        RunKey key = RunKey.of(request);
        cancellationToken(request).cancel();
        CancellationHandle handle = active.get(key);
        return handle != null && handle.cancel();
    }

    public void clearCancellation(HarnessRunRequest request) {
        cancellationTokens.remove(RunKey.of(request));
    }

    public int activeCount() {
        return active.size();
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    @FunctionalInterface
    private interface CancellationHandle {
        boolean cancel();
    }

    /**
     * A one-way, per-run side-effect barrier. Starting an invocation and accepting cancellation
     * are serialized on the same monitor: cancellation that wins prevents invocation admission;
     * invocation that wins is interrupted when cancellation arrives.
     */
    public static final class CancellationToken {
        private static final CancellationToken NONE = new CancellationToken(false);

        private final boolean cancellable;
        private final Object monitor = new Object();
        private final Set<Thread> activeInvocations = new HashSet<>();
        private volatile boolean cancellationRequested;

        private CancellationToken(boolean cancellable) {
            this.cancellable = cancellable;
        }

        /** Compatibility token for executor callers that do not own a durable run. */
        public static CancellationToken none() {
            return NONE;
        }

        public boolean isCancellationRequested() {
            return cancellationRequested;
        }

        /** Checks both the durable token and an interrupt delivered during late registration. */
        public void throwIfCancellationRequested() throws InterruptedException {
            if (cancellationRequested || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Harness run cancellation was requested");
            }
        }

        /** Atomically admits this worker as started, or rejects it after the cancel barrier. */
        public Invocation beginInvocation() throws InterruptedException {
            Thread thread = Thread.currentThread();
            synchronized (monitor) {
                if (cancellationRequested || thread.isInterrupted()) {
                    throw new InterruptedException("Harness run cancellation was requested");
                }
                activeInvocations.add(thread);
            }
            return () -> {
                synchronized (monitor) {
                    activeInvocations.remove(thread);
                }
            };
        }

        private boolean cancel() {
            if (!cancellable) {
                return false;
            }
            ArrayList<Thread> started;
            boolean changed;
            synchronized (monitor) {
                changed = !cancellationRequested;
                cancellationRequested = true;
                started = new ArrayList<>(activeInvocations);
            }
            started.forEach(Thread::interrupt);
            return changed;
        }

        @FunctionalInterface
        public interface Invocation extends AutoCloseable {
            @Override
            void close();
        }
    }

    private record RunKey(String tenantId, Long userId, String sessionId, String runId) {
        private static RunKey of(HarnessRunRequest request) {
            if (request == null) {
                throw new IllegalArgumentException("Run request is required");
            }
            return new RunKey(request.owner().tenantId(), request.owner().userId(),
                request.sessionId(), request.runId());
        }
    }
}
