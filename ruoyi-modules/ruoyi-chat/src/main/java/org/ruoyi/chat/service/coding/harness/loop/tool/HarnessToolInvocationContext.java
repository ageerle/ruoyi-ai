package org.ruoyi.chat.service.coding.harness.loop.tool;

import org.ruoyi.chat.service.coding.harness.approval.ToolCallApprovalAggregate;
import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Thread-bound identity for a registry invocation; model arguments cannot forge this value. */
public final class HarnessToolInvocationContext {

    private static final ThreadLocal<Invocation> CURRENT = new ThreadLocal<>();

    private HarnessToolInvocationContext() { }

    static Scope open(HarnessToolCall call) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Nested Harness tool invocation is not supported");
        }
        Invocation invocation = new Invocation(call.toolCallId(), call.toolName(),
            ToolCallApprovalAggregate.sha256(call.arguments().getBytes(StandardCharsets.UTF_8)));
        CURRENT.set(invocation);
        return CURRENT::remove;
    }

    public static Optional<Invocation> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public record Invocation(String toolCallId, String toolName, String argumentsSha256) { }

    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
