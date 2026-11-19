package org.ruoyi.chat.service.coding.harness.loop.model;

import java.time.Duration;

/** Explicit failed terminal result; elapsed time is retained for accounting and diagnostics. */
public final class ModelTurnException extends Exception {

    private final ModelTurnFailureKind kind;
    private final Duration elapsed;

    public ModelTurnException(ModelTurnFailureKind kind, String message, Throwable cause,
                              Duration elapsed) {
        super(message, cause);
        if (kind == null || elapsed == null || elapsed.isNegative()) {
            throw new IllegalArgumentException("Invalid model turn failure");
        }
        this.kind = kind;
        this.elapsed = elapsed;
    }

    public ModelTurnFailureKind kind() {
        return kind;
    }

    public Duration elapsed() {
        return elapsed;
    }
}
