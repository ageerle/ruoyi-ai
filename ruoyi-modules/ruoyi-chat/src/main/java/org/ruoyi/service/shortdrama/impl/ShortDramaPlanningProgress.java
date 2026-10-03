package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/** Read-only progress, separate from the authoritative submission/terminal receipt. No reasoning text. */
final class ShortDramaPlanningProgress {
    record Call(String id, String phase, String label, String state, long startedAt, long firstContentAt,
                long lastActivityAt, long endedAt, int promptChars, long contentChars, long thinkingChars) {}
    record Card(String key, int scene, int ordinal, String stage, JsonNode panel) {}
    record Snapshot(String requestId, String phase, String message, long startedAt, long updatedAt,
                    List<Call> calls, List<Card> cards) {}
    static String key(Long project, Long script, String request) {
        return "short-drama:planning-progress:v1:" + project + ":" + script + ":" + request;
    }
    static Snapshot read(Long project, Long script, String request) {
        String json = RedisUtils.getCacheObject(key(project, script, request));
        if (json == null) return null;
        try { return AtlasMediaSupport.OBJECT_MAPPER.readValue(json, Snapshot.class); }
        catch (java.io.IOException e) { return null; } // Progress corruption cannot alter a receipt or trigger resubmission.
    }
    static Consumer<Snapshot> persistence(Long project, Long script, String request, Consumer<Snapshot> push) {
        // The Redis key is captured in the submitter's namespace; provider callbacks do not inherit tenant context.
        final String cacheKey = key(project, script, request);
        final String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
        return snapshot -> {
            try { persistInTenant(tenant, () -> {
                RedisUtils.setCacheObject(cacheKey, org.ruoyi.common.json.utils.JsonUtils.toJsonString(snapshot), Duration.ofDays(7));
            }); }
            catch (Exception ignored) { /* Observability must not turn successful generation into a failure. */ }
            push.accept(snapshot);
        };
    }
    static void persistInTenant(String tenant, Runnable write) {
        String previous = org.ruoyi.common.tenant.helper.TenantHelper.getDynamic();
        try { org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, write); }
        finally {
            // dynamic() clears instead of restoring an outer worker scope. Progress must not
            // move the subsequent scene cache, database work or terminal receipt to another tenant.
            if (previous != null && !previous.isBlank())
                org.ruoyi.common.tenant.helper.TenantHelper.setDynamic(previous);
        }
    }
    static final class Tracker {
        private final String requestId;
        private final long startedAt;
        private final Consumer<Snapshot> sink;
        private final Map<String, Call> calls = new LinkedHashMap<>();
        private final Map<String, Card> cards = new LinkedHashMap<>();
        private final Map<Integer, int[]> globalNumbers = new HashMap<>();
        private String phase = "storyboard_plan", message = "正在规划镜头";
        private long publishedAt;
        private boolean terminal;
        Tracker(String requestId, long startedAt, Consumer<Snapshot> sink) {
            this.requestId = requestId; this.startedAt = startedAt; this.sink = sink;
        }
        synchronized void phase(String value, String text) {
            if (terminal) return;
            phase = value; message = text; publish(true);
        }
        synchronized String startCall(String callPhase, String label, int promptChars) {
            String id = UUID.randomUUID().toString(); long now = System.currentTimeMillis();
            if (terminal) return id;
            calls.put(id, new Call(id, callPhase, label, "running", now, 0, now, 0, promptChars, 0, 0));
            publish(true); return id;
        }
        synchronized void activity(String id, String text, boolean thinking) {
            Call call = calls.get(id); if (terminal || call == null || !"running".equals(call.state()) || text == null || text.isEmpty()) return;
            long now = System.currentTimeMillis();
            calls.put(id, new Call(id, call.phase(), call.label(), call.state(), call.startedAt(),
                !thinking && call.firstContentAt() == 0 ? now : call.firstContentAt(), now, 0, call.promptChars(),
                call.contentChars() + (thinking ? 0 : text.length()), call.thinkingChars() + (thinking ? text.length() : 0)));
            publish(false);
        }
        synchronized Call finishCall(String id, boolean success) {
            Call call = calls.get(id); if (call == null || terminal || !"running".equals(call.state())) return call;
            Call finished = new Call(id, call.phase(), call.label(), success ? "done" : "error", call.startedAt(), call.firstContentAt(),
                call.lastActivityAt(), System.currentTimeMillis(), call.promptChars(), call.contentChars(), call.thinkingChars());
            calls.put(id, finished); publish(true); return finished;
        }
        synchronized void card(int scene, int ordinal, String stage, JsonNode panel) {
            if (terminal || panel == null || !panel.isObject()) return;
            String key = scene + ":" + ordinal;
            Card old = cards.get(key);
            if (old != null && rank(old.stage()) > rank(stage)) return;
            var preview = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
            for (String field : List.of("scene_title", "sceneTitle", "segment_goal", "segmentGoal", "description", "location", "video_prompt", "videoPrompt")) {
                JsonNode value = panel.get(field);
                if (value != null && value.isTextual()) preview.put(field, value.asText().substring(0, Math.min(2400, value.asText().length())));
            }
            JsonNode design = panel.has("shot_design") ? panel.get("shot_design") : panel.get("shotDesign");
            if (panel.path("duration").canConvertToInt() && panel.path("duration").asInt() > 0)
                preview.put("duration", panel.path("duration").asInt());
            if (design != null && design.isObject()) {
                var view = preview.putObject("shot_design");
                for (String field : List.of("focus", "viewer_gain", "framing", "movement", "cut_in", "cut_out"))
                    if (design.path(field).isTextual()) view.put(field, design.path(field).asText());
            }
            cards.put(key, new Card(key, scene, ordinal, stage, preview)); publish(true);
        }
        synchronized void clearDrafts(int scene) { cards.values().removeIf(card -> card.scene() == scene && "draft".equals(card.stage())); publish(true); }
        synchronized void assetCard(int category, int ordinal, String stage, JsonNode asset) {
            if (terminal || asset == null || !asset.isObject() || asset.path("name").asText().isBlank()) return;
            String key = "asset:" + category + ":" + ordinal;
            Card old = cards.get(key);
            if (old != null && rank(old.stage()) > rank(stage)) return;
            var preview = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
            for (String field : List.of("name", "description", "visualDescription", "summary", "introduction")) {
                String value = asset.path(field).asText("");
                if (!value.isBlank()) preview.put(field, value.substring(0, Math.min(2400, value.length())));
            }
            cards.put(key, new Card(key, category, ordinal, stage, preview)); publish(true);
        }
        synchronized void bindGlobal(int global, int scene, int ordinal) { globalNumbers.put(global, new int[]{scene, ordinal}); }
        synchronized void ready(int global, JsonNode panel) { int[] address = globalNumbers.get(global); if (address != null) card(address[0], address[1], "ready", panel); }
        synchronized void heartbeat() { if (!terminal) publish(true); }
        synchronized void terminal(String state) { if (terminal) return; phase = state; terminal = true; publish(true); }
        synchronized Snapshot snapshot() {
            return new Snapshot(requestId, phase, message, startedAt, System.currentTimeMillis(), List.copyOf(calls.values()),
                cards.values().stream().sorted(Comparator.comparingInt(Card::scene).thenComparingInt(Card::ordinal)).toList());
        }
        private void publish(boolean force) {
            long now = System.currentTimeMillis(); if (!force && now - publishedAt < 1000) return;
            publishedAt = now; sink.accept(snapshot());
        }
        private int rank(String stage) { return "ready".equals(stage) ? 2 : "planned".equals(stage) ? 1 : 0; }
    }
}
