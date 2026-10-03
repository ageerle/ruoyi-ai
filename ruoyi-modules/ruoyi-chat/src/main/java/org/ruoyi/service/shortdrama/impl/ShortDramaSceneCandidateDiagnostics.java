package org.ruoyi.service.shortdrama.impl;

import cn.hutool.crypto.digest.DigestUtil;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.service.media.AtlasMediaSupport;

import java.time.Duration;
import java.util.*;
import java.util.function.LongSupplier;

/** Raw native responses for diagnosis only. This store is never read by checkpoint/import recovery. */
final class ShortDramaSceneCandidateDiagnostics {
    static final String VERSION = "scene-candidate-v1";
    static final int MAX_RESPONSE_CHARS = 65_536, MAX_ERROR_CHARS = 2_048, MAX_CANDIDATES = 20;
    static final long RETENTION_MILLIS = Duration.ofDays(7).toMillis();
    record Candidate(String candidateId, ShortDramaSceneCheckpoint.Identity identity, String requestId,
                     int attempt, String mode, String state, boolean validated, long receivedAt, long checkedAt,
                     String responseHash, int originalResponseChars, boolean responseTruncated, boolean credentialsRedacted,
                     String modelResponse, String validationStatus, String validationError) {}
    private final ShortDramaSceneCheckpoint.Cache cache;
    private final LongSupplier clock;
    ShortDramaSceneCandidateDiagnostics() {
        this(new ShortDramaSceneCheckpoint.Cache() {
            public String get(String key) { return RedisUtils.getCacheObject(key); }
            public void put(String key, String value) { RedisUtils.setCacheObject(key, value, Duration.ofDays(7)); }
        }, System::currentTimeMillis);
    }
    ShortDramaSceneCandidateDiagnostics(ShortDramaSceneCheckpoint.Cache cache, LongSupplier clock) {
        this.cache = cache; this.clock = clock;
    }
    private String key(ShortDramaSceneCheckpoint.Identity identity) {
        return "short-drama:scene-candidates:" + VERSION + ":" + identity.prefix();
    }
    List<Candidate> read(ShortDramaSceneCheckpoint.Identity identity) {
        try {
            String json = cache.get(key(identity));
            if (json == null) return List.of();
            List<Candidate> saved = AtlasMediaSupport.OBJECT_MAPPER.readValue(json,
                AtlasMediaSupport.OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Candidate.class));
            long now = clock.getAsLong();
            return saved.stream().filter(c -> Objects.equals(c.identity(), identity) && c.receivedAt() <= now
                && now - c.receivedAt() < RETENTION_MILLIS).toList();
        } catch (java.io.IOException e) { return List.of(); }
    }
    synchronized String recordResponse(ShortDramaSceneCheckpoint.Identity identity, String requestId, int attempt,
                                       String mode, String response) {
        String raw = Objects.toString(response, ""), safe = redact(raw), id = UUID.randomUUID().toString();
        var entries = new ArrayList<>(read(identity));
        entries.add(new Candidate(id, identity, requestId, attempt, mode, "unreviewed_candidate", false,
            clock.getAsLong(), 0, DigestUtil.sha256Hex(raw), raw.length(), safe.length() > MAX_RESPONSE_CHARS,
            !raw.equals(safe), limit(safe, MAX_RESPONSE_CHARS), "pending", ""));
        write(identity, entries); return id;
    }
    synchronized void checked(ShortDramaSceneCheckpoint.Identity identity, String candidateId, String error) {
        var entries = new ArrayList<>(read(identity));
        for (int i = 0; i < entries.size(); i++) {
            Candidate c = entries.get(i);
            if (!Objects.equals(c.candidateId(), candidateId)) continue;
            String safeError = limit(redact(Objects.toString(error, "")), MAX_ERROR_CHARS);
            entries.set(i, new Candidate(c.candidateId(), c.identity(), c.requestId(), c.attempt(), c.mode(),
                c.state(), false, c.receivedAt(), clock.getAsLong(), c.responseHash(), c.originalResponseChars(),
                c.responseTruncated(), c.credentialsRedacted() || !Objects.toString(error, "").equals(redact(Objects.toString(error, ""))),
                c.modelResponse(), error == null ? "automatic_checks_passed_unreviewed" : "automatic_checks_failed", safeError));
            write(identity, entries); return;
        }
    }
    private void write(ShortDramaSceneCheckpoint.Identity identity, List<Candidate> entries) {
        if (entries.size() > MAX_CANDIDATES) entries = entries.subList(entries.size() - MAX_CANDIDATES, entries.size());
        try { cache.put(key(identity), AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(entries)); }
        catch (java.io.IOException e) { throw new IllegalStateException("未审阅候选诊断序列化失败", e); }
    }
    static String redact(String text) {
        return Objects.toString(text, "")
            .replaceAll("(?i)\\bBearer\\s+[^\\s\"',}]+", "Bearer [REDACTED]")
            .replaceAll("(?i)\\bsk-[A-Za-z0-9_-]{8,}", "[REDACTED]")
            .replaceAll("(?i)((?:api[_-]?key|access[_-]?token|authorization|password|secret)\\s*[\"']?\\s*[:=]\\s*[\"']?)[^\\s\"',}]+", "$1[REDACTED]");
    }
    static String limit(String value, int max) {
        if (value.length() <= max) return value;
        int end = max;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }
}
