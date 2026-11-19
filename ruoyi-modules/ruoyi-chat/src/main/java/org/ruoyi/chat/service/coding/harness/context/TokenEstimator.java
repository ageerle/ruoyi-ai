package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;
import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;

import java.nio.charset.StandardCharsets;

/** Provider-specific tokenization can be injected without coupling the domain to a model SDK. */
@FunctionalInterface
public interface TokenEstimator {

    long estimateText(String text);

    default long estimateMessage(HarnessMessage message) {
        // Keep the domain projection aligned with the final provider preflight. Roles, message
        // framing, tool-call envelopes and separators consume input even when every user string is
        // empty. Without this overhead ContextEngine can declare a tail safe, only for the exact
        // serialized request to exceed the remaining cumulative budget a few lines later.
        long tokens = 32;
        tokens = safeAdd(tokens,
            safeAdd(estimateText(message.content()), estimateText(message.thinking())));
        tokens = safeAdd(tokens, estimateText(message.toolCallId()));
        tokens = safeAdd(tokens, estimateText(message.toolName()));
        for (HarnessToolCall call : message.toolCalls()) {
            tokens = safeAdd(tokens, 16);
            tokens = safeAdd(tokens, estimateText(call.toolCallId()));
            tokens = safeAdd(tokens, estimateText(call.toolName()));
            tokens = safeAdd(tokens, estimateText(call.arguments()));
        }
        return tokens;
    }

    static TokenEstimator approximate() {
        return text -> {
            if (text == null || text.isEmpty()) {
                return 0;
            }
            return Math.max(1, (text.length() + 3L) / 4L);
        };
    }

    /**
     * Provider-neutral fail-closed upper bound. A byte-level tokenizer cannot emit more tokens
     * than the number of UTF-8 bytes in non-empty input, so this safely handles CJK, emoji and
     * adversarial high-entropy text when an exact provider tokenizer is unavailable.
     */
    static TokenEstimator conservativeUtf8() {
        return text -> text == null || text.isEmpty()
            ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Token estimate overflow", exception);
        }
    }
}
