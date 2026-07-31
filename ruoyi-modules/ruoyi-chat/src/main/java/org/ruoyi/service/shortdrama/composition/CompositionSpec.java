package org.ruoyi.service.shortdrama.composition;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record CompositionSpec(
    List<CompositionSource> sources,
    TransitionType transitionType,
    double transitionDurationSeconds,
    AspectRatio aspectRatio,
    Path narrationAudioPath,
    boolean watermark
) {

    public CompositionSpec {
        sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("At least one video source is required");
        }
        transitionType = transitionType == null ? TransitionType.NONE : transitionType;
        aspectRatio = aspectRatio == null ? AspectRatio.PORTRAIT : aspectRatio;
        if (!Double.isFinite(transitionDurationSeconds) || transitionDurationSeconds < 0) {
            throw new IllegalArgumentException("transitionDurationSeconds must be non-negative");
        }
        if (transitionType == TransitionType.NONE) {
            transitionDurationSeconds = 0;
        } else if (transitionDurationSeconds <= 0) {
            throw new IllegalArgumentException("A transition duration is required for " + transitionType.value());
        }
    }
}
