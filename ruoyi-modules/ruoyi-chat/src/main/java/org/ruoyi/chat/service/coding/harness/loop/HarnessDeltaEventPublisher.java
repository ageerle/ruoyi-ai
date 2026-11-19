package org.ruoyi.chat.service.coding.harness.loop;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import org.ruoyi.chat.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.chat.service.coding.harness.loop.model.ModelTurnListener;
import org.ruoyi.chat.service.coding.harness.model.HarnessEvent;
import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.ruoyi.chat.service.coding.harness.model.HarnessRunState;

import java.util.LinkedHashMap;
import java.util.Map;

/** Coalesces high-frequency deltas before durable publication and flushes at turn settlement. */
final class HarnessDeltaEventPublisher implements ModelTurnListener, AutoCloseable {

    private static final int FLUSH_CHARACTERS = 512;
    private static final long FLUSH_INTERVAL_NANOS = 100_000_000L;

    private final HarnessEventHub eventHub;
    private final HarnessOwner owner;
    private final HarnessRunState run;
    private final String effectId;
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder thinking = new StringBuilder();
    private long lastFlush = System.nanoTime();
    private boolean closed;

    HarnessDeltaEventPublisher(HarnessEventHub eventHub, HarnessOwner owner,
                               HarnessRunState run, String effectId) {
        this.eventHub = eventHub;
        this.owner = owner;
        this.run = run;
        this.effectId = effectId;
    }

    @Override
    public synchronized void onTextDelta(String delta) {
        append(text, delta);
        flushIfNeeded();
    }

    @Override
    public synchronized void onThinkingDelta(String delta) {
        append(thinking, delta);
        flushIfNeeded();
    }

    @Override
    public synchronized void onPartialToolCall(PartialToolCall partial) {
        // Providers may emit one callback per argument token. The complete tool call and final
        // assistant ledger record are authoritative, so partial arguments are intentionally not
        // fsynced as an unbounded event stream.
        ensureOpen();
    }

    @Override
    public synchronized void onCompleteToolCall(CompleteToolCall complete) {
        ensureOpen();
        flushBuffers();
        ToolExecutionRequest call = complete.toolExecutionRequest();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("effectId", effectId);
        data.put("index", complete.index());
        data.put("name", call.name());
        data.put("arguments", call.arguments() == null ? "{}" : call.arguments());
        publish("assistant.tool.complete", call.id(), data);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        flushBuffers();
        closed = true;
    }

    private void append(StringBuilder buffer, String value) {
        ensureOpen();
        if (value != null && !value.isEmpty()) {
            buffer.append(value);
        }
    }

    private void flushIfNeeded() {
        long now = System.nanoTime();
        if (text.length() + thinking.length() >= FLUSH_CHARACTERS
            || now - lastFlush >= FLUSH_INTERVAL_NANOS) {
            flushBuffers();
        }
    }

    private void flushBuffers() {
        if (text.length() > 0) {
            publish("assistant.text.delta", null,
                Map.of("effectId", effectId, "delta", take(text)));
        }
        if (thinking.length() > 0) {
            publish("assistant.thinking.delta", null,
                Map.of("effectId", effectId, "delta", take(thinking)));
        }
        lastFlush = System.nanoTime();
    }

    private String take(StringBuilder buffer) {
        String value = buffer.toString();
        buffer.setLength(0);
        return value;
    }

    private void publish(String type, String toolCallId, Map<String, Object> data) {
        eventHub.publish(owner, HarnessEvent.draft(run.sessionId(), run.runId(), type,
            null, toolCallId, null, data, System.currentTimeMillis()));
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Model delta publisher is closed");
        }
    }

}
