package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.service.media.AtlasMediaSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/** Local production evidence, never a provider credential dump or a paid-submission retry mechanism. */
final class ShortDramaImagePromptEvidence {
    record Evidence(String attemptId, String assetType, Long assetId, Long projectId, Long userId,
                    String model, String referencePurpose, String revisionRequirements,
                    String finalPrompt, String promptSha256, String referenceImageSha256,
                    long createdAt, String predictionId, String providerStatus) {
        boolean revision() { return "identity_revision".equals(referencePurpose) || "location_revision".equals(referencePurpose); }
        Evidence submitted(String prediction, String status) {
            return new Evidence(attemptId, assetType, assetId, projectId, userId, model, referencePurpose,
                revisionRequirements, finalPrompt, promptSha256, referenceImageSha256, createdAt, prediction, status);
        }
    }

    private static final Pattern URL = Pattern.compile("(?i)(?:https?://|data:image/)[^\\s<>\"']+");
    private static final Pattern SECRET = Pattern.compile(
        "(?i)((?:authorization|api[_-]?key|access[_-]?token|client[_-]?secret|password)\\s*[:=]\\s*)(?:Bearer\\s+)?[^\\s,;\"']+");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");
    private final Path root;

    ShortDramaImagePromptEvidence() { this(Path.of("data", "short-drama-image", "prompt-evidence")); }
    ShortDramaImagePromptEvidence(Path root) { this.root = root.toAbsolutePath().normalize(); }

    Evidence prepare(String assetType, Long assetId, Long projectId, Long userId, String model,
                     String purpose, String requirements, String prompt, String referenceImage) {
        Evidence evidence = new Evidence(UUID.randomUUID().toString(), assetType, assetId, projectId, userId,
            model, purpose, redact(requirements), redact(prompt), sha256(prompt),
            referenceImage == null ? null : sha256(referenceImage), System.currentTimeMillis(), null, "prepared");
        write(attemptPath(evidence.attemptId()), evidence, false);
        return evidence;
    }

    void submitted(Evidence evidence, String prediction, String status) {
        Evidence submitted = evidence.submitted(prediction, status);
        // Persist the prediction lookup first so a completed paid task retains its exact edit intent.
        if (prediction != null && !prediction.isBlank()) {
            write(predictionPath(evidence.model(), prediction), submitted, true);
        }
        write(attemptPath(evidence.attemptId()), submitted, true);
    }

    Evidence forPrediction(String assetType, Long assetId, Long userId, String model, String prediction) {
        Path file = predictionPath(model, prediction);
        if (!Files.exists(file)) return null; // Compatibility with tasks submitted before evidence support.
        try {
            Evidence evidence = AtlasMediaSupport.OBJECT_MAPPER.readValue(file.toFile(), Evidence.class);
            if (!assetType.equals(evidence.assetType()) || !assetId.equals(evidence.assetId()) || !userId.equals(evidence.userId())) {
                throw new IllegalArgumentException("图片任务与当前资产不匹配，未修改已批准图片");
            }
            return evidence;
        } catch (IOException ex) {
            throw new IllegalStateException("图片提示词证据无法读取，请核对原任务；未替换已批准图片", ex);
        }
    }

    static String redact(String text) {
        if (text == null) return null;
        String result = URL.matcher(text).replaceAll("[URL redacted]");
        result = SECRET.matcher(result).replaceAll("$1[redacted]");
        return BEARER.matcher(result).replaceAll("Bearer [redacted]");
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private Path attemptPath(String attempt) { return root.resolve("attempts").resolve(attempt + ".json"); }
    private Path predictionPath(String model, String prediction) {
        return root.resolve("predictions").resolve(sha256(model + "\n" + prediction) + ".json");
    }

    private void write(Path file, Evidence evidence, boolean replace) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            if (!replace) {
                Files.writeString(file, AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(evidence), StandardOpenOption.CREATE_NEW);
                return;
            }
            temporary = Files.createTempFile(file.getParent(), "image-prompt-", ".tmp");
            AtlasMediaSupport.OBJECT_MAPPER.writeValue(temporary.toFile(), evidence);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) {
            throw new IllegalStateException(replace
                ? "图片任务可能已受理，证据保存失败，请核对任务ID，勿重新付费提交"
                : "图片提示词证据无法保存，未提交生成任务", ex);
        } finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
}
