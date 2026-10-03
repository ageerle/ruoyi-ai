package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.service.media.AtlasMediaSupport;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Durable paid-submission receipts. These are deliberately separate from image continuity. */
public final class ShortDramaVideoSubmissionStore {
    public record VideoSnapshot(String videoId, String videoUrl, String status, String lastFrameUrl) {
        static VideoSnapshot of(ShortDramaStoryboard shot) {
            return new VideoSnapshot(shot.getVideoId(), shot.getVideoUrl(), shot.getVideoStatus(), shot.getLastFrameUrl());
        }
    }
    public record Submission(String requestId, Long storyboardId, Long projectId, Long userId,
                             String parametersHash, String requestedModel, String actualModel, String providerCode,
                             String predictionId, String videoUrl, String lastFrameUrl, String status, long createdAt, long updatedAt,
                             String error, VideoSnapshot previousVideo) {
        Submission update(String prediction, String state, String message) {
            return new Submission(requestId, storyboardId, projectId, userId, parametersHash, requestedModel,
                actualModel, providerCode, prediction, videoUrl, lastFrameUrl, state, createdAt, System.currentTimeMillis(), message, previousVideo);
        }
        Submission result(String prediction, String url, String lastFrame, String state, String message) {
            return new Submission(requestId, storyboardId, projectId, userId, parametersHash, requestedModel,
                actualModel, providerCode, prediction, url, lastFrame, state, createdAt, System.currentTimeMillis(), message, previousVideo);
        }
    }

    private final Path root;
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    public ShortDramaVideoSubmissionStore() { this(Path.of("data", "short-drama-video", "submissions")); }
    ShortDramaVideoSubmissionStore(Path root) { this.root = root.toAbsolutePath().normalize(); }
    Object lock(Long storyboard) { return locks.computeIfAbsent(storyboard, ignored -> new Object()); }
    static String requestId(String id) {
        if (id == null || id.isBlank()) return UUID.randomUUID().toString();
        try {
            String canonical = UUID.fromString(id).toString();
            if (!canonical.equalsIgnoreCase(id)) throw new IllegalArgumentException();
            return canonical;
        } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("requestId 必须为完整 UUID"); }
    }
    private Path directory(Long storyboard) { return root.resolve(storyboard.toString()); }
    private Path file(Long storyboard, String id) { return directory(storyboard).resolve(requestId(id) + ".json"); }
    void voiceSnapshot(Long storyboard,String id,Object snapshot) {
        try {
            Path folder=directory(storyboard).resolve("voices");Files.createDirectories(folder);
            Files.writeString(folder.resolve(requestId(id)+".json"),AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(snapshot),StandardOpenOption.CREATE_NEW);
        } catch(IOException e){throw new IllegalStateException("声音绑定提交快照无法保存，视频未提交",e);}
    }
    public Submission read(Long storyboard, String id) {
        Path path = file(storyboard, id);
        if (!Files.exists(path)) return null;
        try { return AtlasMediaSupport.OBJECT_MAPPER.readValue(path.toFile(), Submission.class); }
        catch (IOException ex) { throw new IllegalStateException("视频提交记录无法读取，已阻止再次付费提交", ex); }
    }
    public Submission forPrediction(Long storyboard, String prediction) {
        if (prediction == null || prediction.isBlank()) return null;
        if (prediction.startsWith("local:")) return read(storyboard, prediction.substring(6));
        Path directory = directory(storyboard);
        if (!Files.isDirectory(directory)) return null;
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                Submission receipt = AtlasMediaSupport.OBJECT_MAPPER.readValue(path.toFile(), Submission.class);
                if (prediction.equals(receipt.predictionId())) return receipt;
            }
            return null;
        } catch (IOException ex) { throw new IllegalStateException("视频提交记录无法读取，已阻止重复操作", ex); }
    }
    /** CREATE_NEW prevents reuse of the same request even across processes/restarts. */
    boolean claim(Submission submission) {
        try {
            Files.createDirectories(directory(submission.storyboardId()));
            Files.writeString(file(submission.storyboardId(), submission.requestId()),
                AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(submission), StandardOpenOption.CREATE_NEW);
            return true;
        } catch (FileAlreadyExistsException ex) { return false; }
        catch (IOException ex) { throw new IllegalStateException("无法持久化视频提交记录，未提交付费任务", ex); }
    }
    void save(Submission submission) {
        Path temporary = null;
        try {
            Files.createDirectories(directory(submission.storyboardId()));
            temporary = Files.createTempFile(directory(submission.storyboardId()), "submission-", ".tmp");
            AtlasMediaSupport.OBJECT_MAPPER.writeValue(temporary.toFile(), submission);
            try { Files.move(temporary, file(submission.storyboardId(), submission.requestId()),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file(submission.storyboardId(), submission.requestId()), StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) { throw new IllegalStateException("视频任务已可能受理，提交记录保存失败；请核对任务，禁止重交", ex); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
    static String hash(Object parameters) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
            AtlasMediaSupport.OBJECT_MAPPER.writeValueAsBytes(parameters))); }
        catch (Exception ex) { throw new IllegalArgumentException("视频请求参数无法序列化", ex); }
    }
    static void match(Submission previous, String hash) {
        if (previous != null && !previous.parametersHash().equals(hash))
            throw new IllegalArgumentException("requestId 已用于不同视频参数，请使用新 UUID；原任务不会重交");
    }
}
