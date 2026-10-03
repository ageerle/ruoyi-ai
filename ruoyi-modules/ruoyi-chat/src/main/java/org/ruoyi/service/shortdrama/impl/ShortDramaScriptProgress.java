package org.ruoyi.service.shortdrama.impl;

import java.util.function.Consumer;

/** Observable output milestones, never provider reasoning text. */
final class ShortDramaScriptProgress {
    record Snapshot(String state, long startedAt, long updatedAt, long lastActivityAt,
                    long firstContentMs, long firstScriptMs, long elapsedMs,
                    int promptChars, int contentChars, int scriptChars, int thinkingChars) {}
    private final long startedAt = System.currentTimeMillis();
    private final Consumer<Snapshot> sink;
    private long lastActivityAt, firstContentMs = -1, firstScriptMs = -1, publishedAt;
    private int promptChars, contentChars, scriptChars, thinkingChars;
    private String state = "waiting";

    ShortDramaScriptProgress(Consumer<Snapshot> sink) { this.sink = sink; }
    synchronized void start(int chars) { promptChars = chars; publish(true); }
    synchronized void thinking(int chars) { thinkingChars += chars; activity(); publish(false); }
    synchronized void content(int chars) {
        contentChars += chars; activity();
        boolean first = firstContentMs < 0;
        if (first) firstContentMs = lastActivityAt - startedAt;
        state = "writing"; publish(first);
    }
    synchronized void script(int chars) {
        scriptChars += chars;
        boolean first = firstScriptMs < 0;
        if (first) firstScriptMs = System.currentTimeMillis() - startedAt;
        publish(first);
    }
    synchronized void state(String value) { state = value; publish(true); }
    private void activity() { lastActivityAt = System.currentTimeMillis(); }
    private void publish(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - publishedAt < 250) return;
        publishedAt = now;
        sink.accept(new Snapshot(state, startedAt, now, lastActivityAt, firstContentMs, firstScriptMs,
            now - startedAt, promptChars, contentChars, scriptChars, thinkingChars));
    }
}
