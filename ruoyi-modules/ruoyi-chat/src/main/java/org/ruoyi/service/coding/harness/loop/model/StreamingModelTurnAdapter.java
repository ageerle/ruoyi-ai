package org.ruoyi.service.coding.harness.loop.model;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Low-level adapter that turns LangChain4j's callback stream into one exactly-once turn result.
 * It creates no executor or thread; the timeout scheduler is owned by the caller.
 */
public final class StreamingModelTurnAdapter {

    private final StreamingChatModel model;
    private final ScheduledExecutorService timeoutScheduler;
    private final Clock clock;

    public StreamingModelTurnAdapter(StreamingChatModel model,
                                     ScheduledExecutorService timeoutScheduler,
                                     Clock clock) {
        this.model = Objects.requireNonNull(model, "model");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "timeoutScheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ModelTurnResult execute(ChatRequest request, Duration timeout,
                                   ModelTurnListener listener)
        throws InterruptedException, ModelTurnException {
        return start(request, timeout, listener).await();
    }

    public ModelTurnHandle start(ChatRequest request, Duration timeout,
                                 ModelTurnListener listener) {
        Objects.requireNonNull(request, "request");
        long timeoutNanos = positiveNanos(timeout);
        TurnState state = new TurnState(listener == null ? ModelTurnListener.NOOP : listener,
            clock, timeout, timeoutScheduler);
        ModelTurnHandle handle = new ModelTurnHandle(state.result, state);

        ScheduledFuture<?> timeoutFuture;
        try {
            timeoutFuture = timeoutScheduler.schedule(state::timedOut, timeoutNanos,
                TimeUnit.NANOSECONDS);
            if (timeoutFuture == null) {
                throw new IllegalStateException("Timeout scheduler returned no future");
            }
        } catch (Throwable failure) {
            state.failed(ModelTurnFailureKind.START_FAILURE,
                "Unable to schedule model turn timeout", failure, true);
            return handle;
        }
        state.bindTimeout(timeoutFuture);

        if (!state.active()) {
            return handle;
        }
        try {
            model.chat(request, new TurnHandler(state));
        } catch (Throwable failure) {
            state.failed(ProviderFailureClassifier.classify(failure),
                safeProviderMessage("Streaming model failed while starting the turn", failure),
                failure, true);
        }
        return handle;
    }

    private long positiveNanos(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Model turn timeout must be positive");
        }
        try {
            return timeout.toNanos();
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Model turn timeout is too large", overflow);
        }
    }

    private static final class TurnHandler implements StreamingChatResponseHandler {
        private final TurnState state;

        private TurnHandler(TurnState state) {
            this.state = state;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            state.textDelta(partialResponse);
        }

        @Override
        public void onPartialResponse(PartialResponse partialResponse,
                                      PartialResponseContext context) {
            state.register(context == null ? null : context.streamingHandle());
            state.textDelta(partialResponse == null ? null : partialResponse.text());
        }

        @Override
        public void onPartialThinking(PartialThinking partialThinking) {
            state.thinkingDelta(partialThinking == null ? null : partialThinking.text());
        }

        @Override
        public void onPartialThinking(PartialThinking partialThinking,
                                      PartialThinkingContext context) {
            state.register(context == null ? null : context.streamingHandle());
            state.thinkingDelta(partialThinking == null ? null : partialThinking.text());
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall) {
            state.partialToolCall(partialToolCall);
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall,
                                      PartialToolCallContext context) {
            state.register(context == null ? null : context.streamingHandle());
            state.partialToolCall(partialToolCall);
        }

        @Override
        public void onCompleteToolCall(CompleteToolCall completeToolCall) {
            state.completeToolCall(completeToolCall);
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            state.completed(completeResponse);
        }

        @Override
        public void onError(Throwable error) {
            Throwable providerError = error == null
                ? new IllegalStateException("Provider supplied a null error") : error;
            state.failed(ProviderFailureClassifier.classify(providerError),
                safeProviderMessage("Streaming model reported an error", providerError),
                providerError, true);
        }
    }

    private static String safeProviderMessage(String prefix, Throwable failure) {
        return prefix + " [" + ProviderFailureClassifier.safeDiagnosticCode(failure) + "]";
    }

    private static final class TurnState implements ModelTurnHandle.Control {
        private final Object lock = new Object();
        private final ModelTurnListener listener;
        private final Clock clock;
        private final Duration timeout;
        private final Instant startedAt;
        private final CompletableFuture<ModelTurnResult> result = new CompletableFuture<>();
        private final Set<StreamingHandle> streamingHandles =
            Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<StreamingHandle> cancelledHandles =
            Collections.newSetFromMap(new IdentityHashMap<>());
        private ScheduledFuture<?> timeoutFuture;
        private final ScheduledExecutorService timeoutScheduler;
        private long lastProgressNanos = System.nanoTime();
        private boolean terminal;

        private TurnState(ModelTurnListener listener, Clock clock, Duration timeout,
                          ScheduledExecutorService timeoutScheduler) {
            this.listener = listener;
            this.clock = clock;
            this.timeout = timeout;
            this.timeoutScheduler = timeoutScheduler;
            this.startedAt = clock.instant();
        }

        private boolean active() {
            synchronized (lock) {
                return !terminal;
            }
        }

        private void bindTimeout(ScheduledFuture<?> future) {
            boolean cancel;
            synchronized (lock) {
                cancel = terminal;
                if (!terminal) {
                    timeoutFuture = future;
                }
            }
            if (cancel) {
                safeCancel(future);
            }
        }

        private void register(StreamingHandle handle) {
            if (handle == null) {
                return;
            }
            boolean cancel;
            synchronized (lock) {
                cancel = terminal && cancelledHandles.add(handle);
                if (!terminal) {
                    streamingHandles.add(handle);
                }
            }
            if (cancel) {
                safeCancel(handle);
            }
        }

        private void textDelta(String delta) {
            if (delta == null || delta.isEmpty()) {
                return;
            }
            notifyListener(() -> listener.onTextDelta(delta));
        }

        private void thinkingDelta(String delta) {
            if (delta == null || delta.isEmpty()) {
                return;
            }
            notifyListener(() -> listener.onThinkingDelta(delta));
        }

        private void partialToolCall(PartialToolCall partial) {
            if (partial == null || (empty(partial.id()) && empty(partial.name())
                && empty(partial.partialArguments()))) {
                return;
            }
            notifyListener(() -> listener.onPartialToolCall(partial));
        }

        private void completeToolCall(CompleteToolCall complete) {
            if (complete == null) {
                return;
            }
            notifyListener(() -> listener.onCompleteToolCall(complete));
        }

        private void notifyListener(ListenerCall callback) {
            Terminal terminalClaim = null;
            synchronized (lock) {
                if (terminal) {
                    return;
                }
                try {
                    lastProgressNanos = System.nanoTime();
                    callback.invoke();
                } catch (Throwable listenerFailure) {
                    terminalClaim = claimFailure(ModelTurnFailureKind.LISTENER_ERROR,
                        "Model turn progress listener failed", listenerFailure, true);
                }
            }
            publish(terminalClaim);
        }

        private void completed(ChatResponse response) {
            if (response == null) {
                failed(ModelTurnFailureKind.PROVIDER_ERROR,
                    "Streaming model completed with a null response",
                    new IllegalStateException("Complete ChatResponse is required"), true);
                return;
            }
            String finishReasonRejection = ProviderFinishReasonGuard.rejectionReason(response);
            if (finishReasonRejection != null) {
                // The provider has already closed the stream. Surface a retryable provider failure
                // without admitting partial text, tool calls, or usage to the durable ledger.
                failed(ModelTurnFailureKind.PROVIDER_ERROR, finishReasonRejection,
                    new IllegalStateException(finishReasonRejection), false);
                return;
            }
            Terminal claim;
            synchronized (lock) {
                if (terminal) {
                    return;
                }
                terminal = true;
                claim = terminal(new ModelTurnResult(response, elapsed()), null, false);
            }
            publish(claim);
        }

        private void timedOut() {
            Terminal claim;
            synchronized (lock) {
                if (terminal) {
                    return;
                }
                long remaining = timeout.toNanos() - (System.nanoTime() - lastProgressNanos);
                if (remaining > 0) {
                    try {
                        bindTimeout(timeoutScheduler.schedule(this::timedOut, remaining,
                            TimeUnit.NANOSECONDS));
                        return;
                    } catch (RuntimeException schedulingFailure) {
                        claim = claimFailure(ModelTurnFailureKind.START_FAILURE,
                            "Unable to schedule model inactivity timeout", schedulingFailure, true);
                    }
                } else {
                    claim = claimFailure(ModelTurnFailureKind.TIMEOUT,
                        "Model response inactive for " + timeout,
                        new java.util.concurrent.TimeoutException("Model response inactive"), true);
                }
            }
            publish(claim);
        }

        private void failed(ModelTurnFailureKind kind, String message, Throwable cause,
                            boolean cancelProvider) {
            Terminal claim;
            synchronized (lock) {
                claim = claimFailure(kind, message, cause, cancelProvider);
            }
            publish(claim);
        }

        private Terminal claimFailure(ModelTurnFailureKind kind, String message, Throwable cause,
                                      boolean cancelProvider) {
            if (terminal) {
                return null;
            }
            terminal = true;
            ModelTurnException failure = new ModelTurnException(kind, message, cause, elapsed());
            return terminal(null, failure, cancelProvider);
        }

        private Terminal terminal(ModelTurnResult success, ModelTurnException failure,
                                  boolean cancelProvider) {
            ScheduledFuture<?> scheduled = timeoutFuture;
            timeoutFuture = null;
            List<StreamingHandle> handles = cancelProvider
                ? new ArrayList<>(streamingHandles) : List.of();
            cancelledHandles.addAll(handles);
            streamingHandles.clear();
            return new Terminal(success, failure, scheduled, handles);
        }

        private void publish(Terminal claim) {
            if (claim == null) {
                return;
            }
            safeCancel(claim.timeoutFuture());
            for (StreamingHandle handle : claim.streamingHandles()) {
                safeCancel(handle);
            }
            if (claim.success() != null) {
                result.complete(claim.success());
            } else {
                result.completeExceptionally(claim.failure());
            }
        }

        @Override
        public boolean cancel() {
            Terminal claim;
            synchronized (lock) {
                claim = claimFailure(ModelTurnFailureKind.CANCELLED,
                    "Model turn was cancelled", null, true);
            }
            publish(claim);
            return claim != null;
        }

        @Override
        public void interrupted() {
            failed(ModelTurnFailureKind.INTERRUPTED,
                "Thread was interrupted while awaiting the model turn", null, true);
        }

        @Override
        public Duration elapsed() {
            try {
                Duration value = Duration.between(startedAt, clock.instant());
                return value.isNegative() ? Duration.ZERO : value;
            } catch (RuntimeException clockFailure) {
                return Duration.ZERO;
            }
        }

        private static boolean empty(String value) {
            return value == null || value.isEmpty();
        }

        private static void safeCancel(ScheduledFuture<?> future) {
            if (future != null) {
                try {
                    future.cancel(false);
                } catch (Throwable ignored) {
                    // Terminal outcome already won; cleanup failures cannot replace it.
                }
            }
        }

        private static void safeCancel(StreamingHandle handle) {
            try {
                handle.cancel();
            } catch (Throwable ignored) {
                // Terminal outcome already won; cleanup failures cannot escape a provider callback.
            }
        }
    }

    @FunctionalInterface
    private interface ListenerCall {
        void invoke();
    }

    private record Terminal(
        ModelTurnResult success,
        ModelTurnException failure,
        ScheduledFuture<?> timeoutFuture,
        List<StreamingHandle> streamingHandles
    ) {
    }
}
