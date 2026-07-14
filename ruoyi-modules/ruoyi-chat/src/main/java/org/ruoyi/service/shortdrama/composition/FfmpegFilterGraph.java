package org.ruoyi.service.shortdrama.composition;

public record FfmpegFilterGraph(
    String value,
    String videoLabel,
    String audioLabel,
    double expectedDurationSeconds,
    VideoCanvas canvas
) {
}
