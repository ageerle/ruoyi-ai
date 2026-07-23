package org.ruoyi.service.shortdrama.composition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class FfmpegCommandBuilderTest {

    @Test
    void keepsWindowsPathsAsIndividualProcessBuilderArguments() {
        FfmpegCompositionProperties properties = new FfmpegCompositionProperties();
        properties.setFfmpegPath("C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe");
        FfmpegCommandBuilder builder = new FfmpegCommandBuilder(properties);
        CompositionSource source = new CompositionSource(
            Path.of("C:\\video input\\clip & one.mp4"),
            5.0
        );
        CompositionSpec spec = new CompositionSpec(
            List.of(source),
            TransitionType.NONE,
            0,
            AspectRatio.PORTRAIT,
            null,
            false
        );
        FfmpegFilterGraph graph = new FfmpegFilterGraph(
            "[0:v]null[v0]",
            "v0",
            "a0",
            5,
            new VideoCanvas(1080, 1920, 30)
        );
        Path filter = Path.of("C:\\job dir\\filter.txt");
        Path output = Path.of("C:\\job dir\\output.mp4");

        List<String> command = builder.build(spec, graph, filter, output);

        assertEquals(properties.getFfmpegPath(), command.get(0));
        assertTrue(command.contains(source.path().toString()));
        assertTrue(command.contains(filter.toAbsolutePath().normalize().toString()));
        assertTrue(command.contains(output.toAbsolutePath().normalize().toString()));
        assertFalse(command.contains("cmd.exe"));
        assertFalse(command.contains("/c"));
        assertFalse(command.stream().anyMatch(argument -> argument.startsWith("\"") || argument.endsWith("\"")));
    }
}
