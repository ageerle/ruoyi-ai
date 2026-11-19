package org.ruoyi.chat.service.coding.harness.artifact;

public record ArtifactReadResult(
    String artifactId,
    long offset,
    int byteLength,
    String content
) { }
