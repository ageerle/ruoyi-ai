package org.ruoyi.service.shortdrama.composition;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class FfmpegCommandBuilder {

    private final FfmpegCompositionProperties properties;

    public FfmpegCommandBuilder(FfmpegCompositionProperties properties) {
        this.properties = properties;
    }

    public List<String> build(
        CompositionSpec spec,
        FfmpegFilterGraph filterGraph,
        Path filterScript,
        Path output
    ) {
        List<String> command = new ArrayList<>();
        command.add(properties.getFfmpegPath());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        command.add("-nostdin");
        command.add("-y");
        for (CompositionSource source : spec.sources()) {
            command.add("-i");
            command.add(source.path().toString());
        }
        // 旁白音轨作为额外输入流（index = sources.size()）
        if (spec.narrationAudioPath() != null) {
            command.add("-i");
            command.add(spec.narrationAudioPath().toString());
        }
        command.add("-filter_complex_script");
        command.add(filterScript.toAbsolutePath().normalize().toString());
        command.add("-map");
        command.add("[" + filterGraph.videoLabel() + "]");
        command.add("-map");
        command.add("[" + filterGraph.audioLabel() + "]");
        command.add("-c:v");
        command.add(properties.getVideoCodec());
        command.add("-preset");
        command.add(properties.getPreset());
        command.add("-crf");
        command.add(Integer.toString(properties.getCrf()));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-r");
        command.add(Integer.toString(filterGraph.canvas().fps()));
        command.add("-c:a");
        command.add(properties.getAudioCodec());
        command.add("-b:a");
        command.add(properties.getAudioBitrate());
        command.add("-ar");
        command.add(Integer.toString(properties.getAudioSampleRate()));
        command.add("-ac");
        command.add("2");
        command.add("-t");
        command.add(FfmpegFilterGraphBuilder.seconds(filterGraph.expectedDurationSeconds()));
        command.add("-map_metadata");
        command.add("-1");
        command.add("-map_chapters");
        command.add("-1");
        command.add("-max_muxing_queue_size");
        command.add("1024");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.toAbsolutePath().normalize().toString());
        return List.copyOf(command);
    }
}
