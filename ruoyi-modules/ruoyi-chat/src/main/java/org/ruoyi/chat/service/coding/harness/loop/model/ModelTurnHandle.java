package org.ruoyi.chat.service.coding.harness.loop.model;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/** Cancellable handle for an already-started streaming provider turn. */
public final class ModelTurnHandle {

    interface Control {
        boolean cancel();

        void interrupted();

        Duration elapsed();
    }

    private final CompletableFuture<ModelTurnResult> result;
    private final Control control;

    ModelTurnHandle(CompletableFuture<ModelTurnResult> result, Control control) {
        this.result = result;
        this.control = control;
    }

    /** A read-only stage; completing the returned future cannot settle the underlying turn. */
    public CompletionStage<ModelTurnResult> completion() {
        return result.minimalCompletionStage();
    }

    public boolean cancel() {
        return control.cancel();
    }

    public boolean isDone() {
        return result.isDone();
    }

    public ModelTurnResult await() throws InterruptedException, ModelTurnException {
        try {
            return result.get();
        } catch (InterruptedException interrupted) {
            control.interrupted();
            throw interrupted;
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (cause instanceof ModelTurnException failure) {
                throw failure;
            }
            throw new ModelTurnException(ModelTurnFailureKind.START_FAILURE,
                "Model turn completed with an unknown failure", cause, control.elapsed());
        }
    }
}
