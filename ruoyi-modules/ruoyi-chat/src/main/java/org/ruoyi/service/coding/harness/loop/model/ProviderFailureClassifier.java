package org.ruoyi.service.coding.harness.loop.model;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/** Provider-neutral classification for errors that require a control-flow change, not retries. */
final class ProviderFailureClassifier {

    private static final int MAX_CAUSE_DEPTH = 12;

    private ProviderFailureClassifier() { }

    static ModelTurnFailureKind classify(Throwable failure) {
        return switch (safeDiagnosticCode(failure)) {
            case "CONTEXT_OVERFLOW" -> ModelTurnFailureKind.CONTEXT_OVERFLOW;
            case "REQUEST_REJECTED", "AUTHENTICATION_REJECTED", "REASONING_REPLAY_REQUIRED" ->
                ModelTurnFailureKind.REQUEST_REJECTED;
            case "MODEL_TIMEOUT" -> ModelTurnFailureKind.TIMEOUT;
            default -> ModelTurnFailureKind.PROVIDER_ERROR;
        };
    }

    /** Stable diagnostics only: never persist or log a provider body or throwable message. */
    static String safeDiagnosticCode(Throwable failure) {
        if (isContextOverflow(failure)) {
            return "CONTEXT_OVERFLOW";
        }
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH
            && seen.add(current); depth++, current = current.getCause()) {
            String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
            if (current instanceof java.util.concurrent.TimeoutException
                || current instanceof java.net.http.HttpTimeoutException) {
                return "MODEL_TIMEOUT";
            }
            if (message.contains("reasoning_content")
                || message.contains("thinking mode") && message.contains("passed back")) {
                return "REASONING_REPLAY_REQUIRED";
            }
            if (message.contains("429") || message.contains("rate limit")
                || message.contains("too many requests")) {
                return "RATE_LIMITED";
            }
            if (message.contains("401") || message.contains("403")
                || message.contains("unauthorized") || message.contains("forbidden")) {
                return "AUTHENTICATION_REJECTED";
            }
            if (message.contains("400") || message.contains("bad request")
                || message.contains("invalid request")) {
                return "REQUEST_REJECTED";
            }
            if (message.contains("500") || message.contains("502")
                || message.contains("503") || message.contains("504")
                || message.contains("service unavailable") || message.contains("bad gateway")) {
                return "PROVIDER_UNAVAILABLE";
            }
        }
        return "PROVIDER_FAILURE";
    }

    static boolean isContextOverflow(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH
            && seen.add(current); depth++, current = current.getCause()) {
            String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("context_length_exceeded")
                || message.contains("maximum context length")
                || message.contains("context window") && containsExceeded(message)
                || message.contains("context length") && containsExceeded(message)
                || message.contains("input token") && containsExceeded(message)
                || message.contains("prompt is too long")
                || message.contains("request too large") && message.contains("token")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsExceeded(String message) {
        return message.contains("exceed") || message.contains("too long")
            || message.contains("too many") || message.contains("maximum")
            || message.contains("limit");
    }
}
