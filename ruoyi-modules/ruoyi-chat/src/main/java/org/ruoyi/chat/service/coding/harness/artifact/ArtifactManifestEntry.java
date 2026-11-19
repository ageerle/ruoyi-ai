package org.ruoyi.chat.service.coding.harness.artifact;

/** Durable artifact discovery metadata; content remains available only through bounded reads. */
public record ArtifactManifestEntry(
    String sourceRunId,
    String artifactId,
    long byteSize,
    long createdAt
) { }
