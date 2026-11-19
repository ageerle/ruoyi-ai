package org.ruoyi.chat.service.coding.harness.artifact;

import java.util.List;

public record ArtifactManifestPage(
    List<ArtifactManifestEntry> artifacts,
    String nextCursor,
    boolean hasMore
) {
    public ArtifactManifestPage {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
