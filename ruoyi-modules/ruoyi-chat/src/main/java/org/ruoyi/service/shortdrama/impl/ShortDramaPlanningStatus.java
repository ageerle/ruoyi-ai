package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.time.Duration;
import java.util.*;

/** Submission receipt independent of an SSE connection. An absent/old receipt never proves upstream failure. */
final class ShortDramaPlanningStatus {
    static final String VERSION = "planning-status-v1";
    record Status(Long projectId, Long scriptId, String requestId, String scriptHash, String model,
                  int minimumShotSeconds, String runtimeId, String state, long submittedAt, long updatedAt,
                  Integer panelCount, String error, List<Long> storyboardIds, String storyboardHash) {
        boolean terminal() { return "done".equals(state) || "error".equals(state); }
    }
    private final String runtimeId = UUID.randomUUID().toString();
    private final ShortDramaSceneCheckpoint.Cache cache;
    private final String namespace;
    ShortDramaPlanningStatus() {
        this(VERSION);
    }
    ShortDramaPlanningStatus(String namespace) {
        this(new ShortDramaSceneCheckpoint.Cache() {
            public String get(String key) { return RedisUtils.getCacheObject(key); }
            public void put(String key, String value) { RedisUtils.setCacheObject(key, value, Duration.ofDays(7)); }
        }, namespace);
    }
    ShortDramaPlanningStatus(ShortDramaSceneCheckpoint.Cache cache) { this(cache, VERSION); }
    ShortDramaPlanningStatus(ShortDramaSceneCheckpoint.Cache cache, String namespace) { this.cache = cache; this.namespace = namespace; }
    private String prefix(Long project, Long script) { return "short-drama:" + namespace + ":" + project + ":" + script + ":"; }
    Status read(Long project, Long script, String requestId) {
        String json = cache.get(prefix(project, script) + (requestId == null ? "latest" : "request:" + requestId));
        try { return json == null ? null : AtlasMediaSupport.OBJECT_MAPPER.readValue(json, Status.class); }
        catch (java.io.IOException e) { throw new IllegalStateException("分镜请求状态损坏，请核对原请求，勿重复提交", e); }
    }
    boolean currentRuntime(Status status) { return runtimeId.equals(status.runtimeId()); }
    synchronized Status queued(Long project, Long script, String requestId, String scriptHash, String model, int minimum) {
        Status existing = read(project, script, requestId);
        if (existing != null) throw new IllegalStateException("分镜requestId已有回执，不得覆盖并重复提交");
        long now = System.currentTimeMillis();
        Status status = new Status(project, script, requestId, scriptHash, model, minimum, runtimeId, "queued", now, now, null, "", null, "");
        write(status, true); return status;
    }
    synchronized Status update(Status previous, String state, Integer count, String error) {
        if (!Set.of("queued", "running", "done", "error").contains(state)) throw new IllegalArgumentException("无效的分镜请求状态");
        // Callbacks retain the original queued receipt. Always consult the persisted request so a
        // late phase/heartbeat cannot undo a terminal result (or overwrite a newer latest request).
        Status current = read(previous.projectId(), previous.scriptId(), previous.requestId());
        if (current == null) current = previous;
        if (current.terminal() || ("running".equals(current.state()) && "queued".equals(state))) return current;
        Status status = new Status(current.projectId(), current.scriptId(), current.requestId(), current.scriptHash(),
            current.model(), current.minimumShotSeconds(), current.runtimeId(), state, current.submittedAt(),
            System.currentTimeMillis(), count, ShortDramaSceneCandidateDiagnostics.limit(ShortDramaSceneCandidateDiagnostics.redact(error), 2048),
            current.storyboardIds(), current.storyboardHash());
        write(status, false); return status;
    }
    synchronized Status complete(Status previous, List<Long> storyboardIds, String storyboardHash) {
        if (storyboardIds == null || storyboardIds.isEmpty() || storyboardIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("完成回执必须绑定分镜 ID");
        if (storyboardHash == null || !storyboardHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("完成回执必须绑定分镜内容哈希");
        Status current = read(previous.projectId(), previous.scriptId(), previous.requestId());
        if (current == null) current = previous;
        if (current.terminal()) return current;
        List<Long> ids = List.copyOf(storyboardIds);
        Status status = new Status(current.projectId(), current.scriptId(), current.requestId(), current.scriptHash(),
            current.model(), current.minimumShotSeconds(), current.runtimeId(), "done", current.submittedAt(),
            System.currentTimeMillis(), ids.size(), "", ids, storyboardHash);
        write(status, false); return status;
    }
    private void write(Status status, boolean newSubmission) {
        try {
            String json = AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(status);
            cache.put(prefix(status.projectId(), status.scriptId()) + "request:" + status.requestId(), json);
            Status latest = read(status.projectId(), status.scriptId(), null);
            if (newSubmission || latest == null || latest.requestId().equals(status.requestId()))
                cache.put(prefix(status.projectId(), status.scriptId()) + "latest", json);
        } catch (java.io.IOException e) { throw new IllegalStateException("分镜排队状态保存失败，未提交后台任务", e); }
    }
    static String checkedRequestId(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("requestId须为1至128位字母、数字、下划线或连字符");
        return value;
    }
}
