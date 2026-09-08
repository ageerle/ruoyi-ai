package org.ruoyi.service.coding.harness.artifact;

import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Run-scoped facade used by the loop and read_artifact tool; absolute paths never escape it. */
@Service
public class HarnessArtifactRepository {

    private final ArtifactStore store;

    @Autowired
    public HarnessArtifactRepository(
        @Value("${coding.harness.data-dir:./data/coding-harness}") String dataDirectory,
        @Value("${coding.harness.artifacts.max-input-bytes:8388608}") long maxInputBytes,
        @Value("${coding.harness.artifacts.preview-bytes:4096}") int previewBytes,
        @Value("${coding.harness.artifacts.max-read-bytes:262144}") int maxReadBytes) {
        this(new ArtifactStore(Path.of(dataDirectory).resolve("artifacts"), maxInputBytes,
            previewBytes, maxReadBytes));
    }

    public HarnessArtifactRepository(ArtifactStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public ArtifactRef putToolOutput(HarnessOwner owner, String sessionId, String runId,
                                     String content) {
        byte[] bytes = Objects.toString(content, "").getBytes(StandardCharsets.UTF_8);
        return store.put(scope(owner, sessionId, runId), "text/plain; charset=utf-8", bytes);
    }

    public ArtifactRef putImage(HarnessOwner owner, String sessionId, String runId,
                                String mediaType, byte[] content) {
        if (mediaType == null || !mediaType.startsWith("image/")) {
            throw new IllegalArgumentException("Image media type is required");
        }
        return store.put(scope(owner, sessionId, runId), mediaType, content);
    }

    public byte[] readImage(HarnessOwner owner, String sessionId, String runId,
                            String artifactId, int maximumBytes) {
        return store.readAll(scope(owner, sessionId, runId), artifactId, maximumBytes);
    }

    public ArtifactReadResult readText(HarnessOwner owner, String sessionId, String runId,
                                       String artifactId, long offset, int length) {
        byte[] bytes = store.read(scope(owner, sessionId, runId), artifactId, offset, length);
        return new ArtifactReadResult(artifactId, offset, bytes.length,
            new String(bytes, StandardCharsets.UTF_8));
    }

    public ArtifactManifestPage list(HarnessOwner owner, String sessionId, String currentRunId,
                                     String afterCursor, int limit) {
        return store.listSession(scope(owner, sessionId, currentRunId), afterCursor, limit);
    }

    ArtifactScope scope(HarnessOwner owner, String sessionId, String runId) {
        Objects.requireNonNull(owner, "owner");
        return new ArtifactScope("o-" + sha256(owner.tenantId() + "\u0000" + owner.userId())
            .substring(0, 40), sessionId, runId);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
