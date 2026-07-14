package org.ruoyi.service.shortdrama.composition;

import java.nio.file.Path;
import java.util.Objects;

public record CompositionArtifact(
    Path path,
    double durationSeconds,
    int width,
    int height,
    TransitionType transitionType
) {

    public CompositionArtifact {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        transitionType = Objects.requireNonNull(transitionType, "transitionType");
    }
}
