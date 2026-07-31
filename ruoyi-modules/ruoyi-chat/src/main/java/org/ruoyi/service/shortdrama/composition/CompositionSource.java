package org.ruoyi.service.shortdrama.composition;

import java.nio.file.Path;
import java.util.Objects;

public record CompositionSource(Path path, Double fallbackDurationSeconds) {

    public CompositionSource {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (fallbackDurationSeconds != null
            && (!Double.isFinite(fallbackDurationSeconds) || fallbackDurationSeconds <= 0)) {
            throw new IllegalArgumentException("fallbackDurationSeconds must be positive");
        }
    }
}
