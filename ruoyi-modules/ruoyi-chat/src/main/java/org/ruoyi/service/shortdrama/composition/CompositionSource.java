package org.ruoyi.service.shortdrama.composition;

import java.nio.file.Path;
import java.util.Objects;

public record CompositionSource(Path path, Double fallbackDurationSeconds, Double plannedDurationSeconds) {

    public CompositionSource(Path path, Double fallbackDurationSeconds) { this(path, fallbackDurationSeconds, null); }

    public CompositionSource {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (fallbackDurationSeconds != null
            && (!Double.isFinite(fallbackDurationSeconds) || fallbackDurationSeconds <= 0)) {
            throw new IllegalArgumentException("fallbackDurationSeconds must be positive");
        }
        if (plannedDurationSeconds != null && (!Double.isFinite(plannedDurationSeconds) || plannedDurationSeconds <= 0)) {
            throw new IllegalArgumentException("plannedDurationSeconds must be positive");
        }
    }

    MediaInfo forTimeline(MediaInfo media) {
        if (plannedDurationSeconds == null) return media;
        if (media.durationSeconds() + 0.15 < plannedDurationSeconds) throw new IllegalArgumentException("源视频短于镜头预算，请修订或补齐视频");
        return new MediaInfo(plannedDurationSeconds, media.width(), media.height(), media.hasAudio(), media.videoStreamIndex());
    }
}
