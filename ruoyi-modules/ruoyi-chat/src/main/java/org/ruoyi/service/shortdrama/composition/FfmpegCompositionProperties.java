package org.ruoyi.service.shortdrama.composition;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "short-drama.composition")
public class FfmpegCompositionProperties {

    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private int fps = 30;
    private int audioSampleRate = 48_000;
    private String videoCodec = "libx264";
    private String audioCodec = "aac";
    private String preset = "medium";
    private int crf = 20;
    private String audioBitrate = "192k";
    private int maxClips = 100;
    private double maxTransitionSeconds = 2.0;
    private long minimumOutputBytes = 1_024;
    private int maxProcessOutputBytes = 1_048_576;
    private long maxSourceBytes = 512L * 1024 * 1024;
    private long maxTotalSourceBytes = 2L * 1024 * 1024 * 1024;
    private Duration probeTimeout = Duration.ofSeconds(30);
    private Duration processTimeout = Duration.ofMinutes(30);
    private Duration jobStaleAfter = Duration.ofMinutes(45);
    private int workerCoreSize = 1;
    private int workerMaxSize = 2;
    private int workerQueueCapacity = 8;
    private String storageMode = "local";
    private String localOutputDirectory = "logs/short-drama-compositions";

    /** 成片是否默认加水印（前端开关未传时使用该默认值） */
    private boolean watermarkEnabled = true;
    /** 水印文字 */
    private String watermarkText = "视频由ruoyi-drama生成";
    /** 水印字体大小 */
    private int watermarkFontSize = 28;
    /** 水印透明度 0.0-1.0 */
    private double watermarkAlpha = 0.6;
    /** 水印字体文件路径（为空则用系统默认字体） */
    private String watermarkFontFile;

    public BigDecimal normalizeTransitionDuration(TransitionType type, BigDecimal requested) {
        if (type == null || type == TransitionType.NONE) {
            return BigDecimal.ZERO;
        }
        if (requested == null || requested.signum() <= 0) {
            throw new IllegalArgumentException("Transition duration must be positive");
        }
        if (fps <= 0) {
            throw new IllegalArgumentException("Composition fps must be positive");
        }
        if (!Double.isFinite(maxTransitionSeconds) || maxTransitionSeconds <= 0) {
            throw new IllegalArgumentException("Maximum transition duration must be positive and finite");
        }

        BigDecimal normalized = requested.stripTrailingZeros();
        if (normalized.scale() > 3) {
            throw new IllegalArgumentException("Transition duration supports at most 3 decimal places");
        }
        if (normalized.multiply(BigDecimal.valueOf(fps)).compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Transition duration must be at least one output frame");
        }
        if (normalized.compareTo(BigDecimal.valueOf(maxTransitionSeconds)) > 0) {
            throw new IllegalArgumentException(
                "Transition duration exceeds " + BigDecimal.valueOf(maxTransitionSeconds).stripTrailingZeros().toPlainString()
                    + " seconds"
            );
        }
        return normalized;
    }
}
