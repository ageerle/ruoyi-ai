package org.ruoyi.service.shortdrama.composition;

public record MediaInfo(
    double durationSeconds,
    int width,
    int height,
    boolean hasAudio,
    int videoStreamIndex
) {

    public MediaInfo {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Video duration must be positive");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Video dimensions must be positive");
        }
        if (videoStreamIndex < 0) {
            throw new IllegalArgumentException("Video stream index must be non-negative");
        }
    }
}
