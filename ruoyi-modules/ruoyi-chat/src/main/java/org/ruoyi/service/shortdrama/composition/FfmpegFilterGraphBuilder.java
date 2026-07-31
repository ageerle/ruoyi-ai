package org.ruoyi.service.shortdrama.composition;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class FfmpegFilterGraphBuilder {

    private final FfmpegCompositionProperties properties;

    public FfmpegFilterGraphBuilder(FfmpegCompositionProperties properties) {
        this.properties = properties;
    }

    public FfmpegFilterGraph build(CompositionSpec spec, List<MediaInfo> media) {
        if (media.size() != spec.sources().size()) {
            throw new IllegalArgumentException("Media metadata count does not match source count");
        }
        if (media.size() > properties.getMaxClips()) {
            throw new IllegalArgumentException("Too many video clips; maximum is " + properties.getMaxClips());
        }

        VideoCanvas canvas = new VideoCanvas(
            spec.aspectRatio().width(),
            spec.aspectRatio().height(),
            properties.getFps()
        );
        double frameSeconds = 1.0 / canvas.fps();
        List<String> filters = new ArrayList<>();

        for (int index = 0; index < media.size(); index++) {
            MediaInfo info = media.get(index);
            if (info.durationSeconds() < frameSeconds) {
                throw new IllegalArgumentException("Video clip " + (index + 1) + " is shorter than one output frame");
            }
            String duration = seconds(info.durationSeconds());
            filters.add(normalizeVideo(index, info.videoStreamIndex(), duration, canvas));
            filters.add(normalizeAudio(index, duration, info.hasAudio()));
        }

        FfmpegFilterGraph base;
        if (spec.transitionType() == TransitionType.NONE) {
            base = hardCut(filters, media, canvas);
        } else {
            base = transition(filters, spec, media, canvas, frameSeconds);
        }

        // 旁白音轨混入：在最终音轨上 amix 旁白输入（输入 index = sources.size()）
        String audioLabel = base.audioLabel();
        if (spec.narrationAudioPath() != null) {
            audioLabel = mixNarration(filters, base, media, audioLabel);
        }

        // 水印：在最终视频流上 drawtext
        String videoLabel = base.videoLabel();
        if (spec.watermark()) {
            videoLabel = applyWatermark(filters, videoLabel, canvas);
        }

        return new FfmpegFilterGraph(
            String.join(";", filters),
            videoLabel,
            audioLabel,
            base.expectedDurationSeconds(),
            canvas
        );
    }

    private FfmpegFilterGraph hardCut(List<String> filters, List<MediaInfo> media, VideoCanvas canvas) {
        double totalDuration = media.stream().mapToDouble(MediaInfo::durationSeconds).sum();
        if (media.size() == 1) {
            return new FfmpegFilterGraph(
                String.join(";", filters),
                "v0",
                "a0",
                totalDuration,
                canvas
            );
        }

        StringBuilder inputs = new StringBuilder();
        for (int index = 0; index < media.size(); index++) {
            inputs.append("[v").append(index).append("][a").append(index).append("]");
        }
        filters.add(inputs + "concat=n=" + media.size() + ":v=1:a=1[vout][aout]");
        return new FfmpegFilterGraph(
            String.join(";", filters),
            "vout",
            "aout",
            totalDuration,
            canvas
        );
    }

    private FfmpegFilterGraph transition(
        List<String> filters,
        CompositionSpec spec,
        List<MediaInfo> media,
        VideoCanvas canvas,
        double frameSeconds
    ) {
        BigDecimal normalizedTransition = properties.normalizeTransitionDuration(
            spec.transitionType(),
            BigDecimal.valueOf(spec.transitionDurationSeconds())
        );
        double transitionSeconds = normalizedTransition.doubleValue();
        BigDecimal twoTransitionWindows = normalizedTransition.multiply(BigDecimal.valueOf(2));
        for (int index = 1; index < media.size() - 1; index++) {
            if (BigDecimal.valueOf(media.get(index).durationSeconds()).compareTo(twoTransitionWindows) < 0) {
                throw new IllegalArgumentException(
                    "Transition windows overlap in internal clip " + (index + 1)
                );
            }
        }

        String currentVideo = "v0";
        String currentAudio = "a0";
        double currentDuration = media.get(0).durationSeconds();
        for (int index = 1; index < media.size(); index++) {
            double adjacentLimit = Math.min(
                media.get(index - 1).durationSeconds(),
                media.get(index).durationSeconds()
            ) - frameSeconds;
            if (transitionSeconds > adjacentLimit) {
                throw new IllegalArgumentException(
                    "Transition is too long for clips " + index + " and " + (index + 1)
                );
            }

            double offset = currentDuration - transitionSeconds;
            String nextVideo = "vx" + index;
            String nextAudio = "ax" + index;
            filters.add("[" + currentVideo + "][v" + index + "]xfade=transition="
                + spec.transitionType().ffmpegName()
                + ":duration=" + seconds(transitionSeconds)
                + ":offset=" + seconds(offset)
                + "[" + nextVideo + "]");
            filters.add("[" + currentAudio + "][a" + index + "]acrossfade=d="
                + seconds(transitionSeconds)
                + ":c1=tri:c2=tri[" + nextAudio + "]");
            currentVideo = nextVideo;
            currentAudio = nextAudio;
            currentDuration += media.get(index).durationSeconds() - transitionSeconds;
        }

        return new FfmpegFilterGraph(
            String.join(";", filters),
            currentVideo,
            currentAudio,
            currentDuration,
            canvas
        );
    }

    /**
     * 将旁白音轨混入最终音轨。旁白作为额外输入流（index = sources.size()），
     * 先归一化再与主音轨 amix，normalize=0 防止原片音被自动压低，duration=first 以原片长度为准。
     */
    private String mixNarration(List<String> filters, FfmpegFilterGraph base,
                                List<MediaInfo> media, String audioLabel) {
        int narrationIndex = media.size();
        double expectedDuration = base.expectedDurationSeconds();
        String format = "aformat=sample_fmts=fltp:sample_rates=" + properties.getAudioSampleRate()
            + ":channel_layouts=stereo";
        filters.add("[" + narrationIndex + ":a:0]"
            + "aresample=" + properties.getAudioSampleRate() + ":async=1:first_pts=0,"
            + format + ","
            + "atrim=start=0:duration=" + seconds(expectedDuration) + ","
            + "asetpts=PTS-STARTPTS[narr]");
        String mixed = "anarr";
        filters.add("[" + audioLabel + "][narr]amix=inputs=2:duration=first:normalize=0[" + mixed + "]");
        return mixed;
    }

    /**
     * 在最终视频流右下角叠加水印文字。
     * 优先使用显式配置的字体；未配置时探测各平台的常见字体，避免 Windows 版 FFmpeg
     * 在 Fontconfig 配置缺失时因 drawtext 发生原生崩溃。
     */
    private String applyWatermark(List<String> filters, String videoLabel, VideoCanvas canvas) {
        String text = properties.getWatermarkText();
        if (text == null || text.isBlank()) {
            return videoLabel;
        }
        String alpha = formatAlpha(properties.getWatermarkAlpha());
        StringBuilder expr = new StringBuilder();
        expr.append("[").append(videoLabel).append("]drawtext=text='").append(escape(text)).append("'");
        String fontFile = resolveWatermarkFontFile();
        expr.append(":fontfile='").append(escape(fontFile)).append("'");
        expr.append(":fontcolor=white@").append(alpha)
            .append(":fontsize=").append(properties.getWatermarkFontSize())
            .append(":x=w-tw-20:y=h-th-20[wmark]");
        filters.add(expr.toString());
        return "wmark";
    }

    private String resolveWatermarkFontFile() {
        String configured = properties.getWatermarkFontFile();
        if (configured != null && !configured.isBlank()) {
            Path configuredPath = Path.of(configured).toAbsolutePath().normalize();
            if (!Files.isRegularFile(configuredPath) || !Files.isReadable(configuredPath)) {
                throw new IllegalStateException("Configured watermark font is not a readable file: " + configuredPath);
            }
            return configuredPath.toString();
        }

        List<Path> candidates = List.of(
            Path.of("C:/Windows/Fonts/simhei.ttf"),
            Path.of("C:/Windows/Fonts/msyh.ttc"),
            Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
            Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Path.of("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf")
        );
        return candidates.stream()
            .filter(path -> Files.isRegularFile(path) && Files.isReadable(path))
            .findFirst()
            .map(path -> path.toAbsolutePath().normalize().toString())
            .orElseThrow(() -> new IllegalStateException(
                "No readable watermark font was found; configure short-drama.composition.watermark-font-file"
            ));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'");
    }

    private static String formatAlpha(double alpha) {
        if (alpha <= 0) return "0";
        if (alpha >= 1) return "1";
        return BigDecimal.valueOf(alpha).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String normalizeVideo(int inputIndex, int videoStreamIndex, String duration, VideoCanvas canvas) {
        return "[" + inputIndex + ":" + videoStreamIndex + "]"
            + "scale=" + canvas.width() + ":" + canvas.height()
            + ":force_original_aspect_ratio=decrease:force_divisible_by=2,"
            + "pad=" + canvas.width() + ":" + canvas.height()
            + ":(ow-iw)/2:(oh-ih)/2:color=black,"
            + "fps=" + canvas.fps() + ","
            + "setsar=1,format=yuv420p,"
            + "tpad=stop_mode=clone:stop_duration=" + duration + ","
            + "trim=start=0:duration=" + duration + ","
            + "settb=AVTB,setpts=PTS-STARTPTS"
            + "[v" + inputIndex + "]";
    }

    private String normalizeAudio(int index, String duration, boolean hasAudio) {
        String format = "aformat=sample_fmts=fltp:sample_rates=" + properties.getAudioSampleRate()
            + ":channel_layouts=stereo";
        if (hasAudio) {
            return "[" + index + ":a:0]"
                + "aresample=" + properties.getAudioSampleRate() + ":async=1:first_pts=0,"
                + format + ","
                + "apad=pad_dur=" + duration + ","
                + "atrim=start=0:duration=" + duration + ","
                + "asetpts=PTS-STARTPTS"
                + "[a" + index + "]";
        }
        return "anullsrc=r=" + properties.getAudioSampleRate() + ":cl=stereo,"
            + format + ","
            + "atrim=start=0:duration=" + duration + ","
            + "asetpts=PTS-STARTPTS"
            + "[a" + index + "]";
    }

    static String seconds(double value) {
        return BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString();
    }
}
