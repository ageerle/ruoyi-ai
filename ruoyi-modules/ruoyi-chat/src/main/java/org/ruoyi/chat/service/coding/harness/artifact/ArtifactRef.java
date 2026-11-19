package org.ruoyi.chat.service.coding.harness.artifact;

import java.util.Objects;

/** Durable, portable reference to a content-addressed artifact. */
public record ArtifactRef(
    String hash,
    String relativeHandle,
    String mediaType,
    long byteSize,
    String headPreview,
    String tailPreview,
    long createdAt
) {

    public ArtifactRef {
        hash = ArtifactStore.requireHash(hash);
        relativeHandle = requireSafeHandle(relativeHandle);
        mediaType = ArtifactStore.requireMediaType(mediaType);
        if (byteSize < 0) {
            throw new IllegalArgumentException("Artifact byteSize must not be negative");
        }
        headPreview = Objects.requireNonNull(headPreview, "headPreview");
        tailPreview = Objects.requireNonNull(tailPreview, "tailPreview");
        if (createdAt <= 0) {
            throw new IllegalArgumentException("Artifact createdAt must be positive");
        }
    }

    private static String requireSafeHandle(String handle) {
        if (handle == null || handle.isBlank() || handle.startsWith("/")
            || handle.endsWith("/") || handle.contains("\\") || handle.contains(":")) {
            throw new IllegalArgumentException("Artifact relativeHandle is not a safe relative path");
        }
        String[] segments = handle.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                || !segment.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
                throw new IllegalArgumentException("Artifact relativeHandle contains an unsafe segment");
            }
        }
        return handle;
    }
}
