package org.ruoyi.service.shortdrama.composition;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        if (spec.transitionType() == TransitionType.NONE) {
            return hardCut(filters, media, canvas);
        }
        return transition(filters, spec, media, canvas, frameSeconds);
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
