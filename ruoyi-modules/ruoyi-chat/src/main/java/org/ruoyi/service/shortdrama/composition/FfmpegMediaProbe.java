package org.ruoyi.service.shortdrama.composition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FfmpegMediaProbe {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FfmpegCompositionProperties properties;
    private final FfmpegProcessRunner processRunner;

    public MediaInfo probe(Path input, Double fallbackDurationSeconds, Path logPath) {
        List<String> command = new ArrayList<>();
        command.add(properties.getFfprobePath());
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("stream=index,codec_type,width,height,duration:stream_tags=rotate:stream_side_data=rotation:stream_disposition=attached_pic:format=duration");
        command.add("-of");
        command.add("json");
        command.add(input.toAbsolutePath().normalize().toString());

        MediaProcessResult result = processRunner.run(
            command,
            properties.getProbeTimeout(),
            logPath,
            properties.getMaxProcessOutputBytes()
        );
        return parseProbeOutput(result.output(), fallbackDurationSeconds);
    }

    static MediaInfo parseProbeOutput(String json, Double fallbackDurationSeconds) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new MediaProcessException("ffprobe returned invalid JSON", ex);
        }
        if (root == null) {
            throw new MediaProcessException("ffprobe returned an empty response");
        }

        JsonNode video = null;
        boolean hasAudio = false;
        JsonNode streams = root.path("streams");
        if (streams.isArray()) {
            for (JsonNode stream : streams) {
                String codecType = stream.path("codec_type").asText("");
                if ("audio".equals(codecType)) {
                    hasAudio = true;
                } else if (video == null && "video".equals(codecType)
                    && stream.path("disposition").path("attached_pic").asInt(0) != 1) {
                    video = stream;
                }
            }
        }
        if (video == null) {
            throw new MediaProcessException("Input does not contain a video stream");
        }
        int videoStreamIndex = video.path("index").asInt(-1);
        if (videoStreamIndex < 0) {
            throw new MediaProcessException("ffprobe did not report the selected video stream index");
        }

        int width = video.path("width").asInt(0);
        int height = video.path("height").asInt(0);
        int rotation = readRotation(video);
        if (Math.floorMod(rotation, 180) != 0) {
            int originalWidth = width;
            width = height;
            height = originalWidth;
        }

        Double duration = positiveNumber(video.path("duration"));
        if (duration == null) {
            duration = positiveNumber(root.path("format").path("duration"));
        }
        if (duration == null && fallbackDurationSeconds != null
            && Double.isFinite(fallbackDurationSeconds) && fallbackDurationSeconds > 0) {
            duration = fallbackDurationSeconds;
        }
        if (duration == null) {
            throw new MediaProcessException("Unable to determine input video duration");
        }
        return new MediaInfo(duration, width, height, hasAudio, videoStreamIndex);
    }

    private static int readRotation(JsonNode video) {
        JsonNode sideData = video.path("side_data_list");
        if (sideData.isArray()) {
            for (JsonNode item : sideData) {
                if (item.has("rotation")) {
                    return item.path("rotation").asInt(0);
                }
            }
        }
        String tag = video.path("tags").path("rotate").asText(null);
        if (tag != null) {
            try {
                return Integer.parseInt(tag);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static Double positiveNumber(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            double value = node.isNumber() ? node.asDouble() : Double.parseDouble(node.asText());
            return Double.isFinite(value) && value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
