package org.ruoyi.service.shortdrama.composition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class FfmpegFilterGraphBuilderTest {

    private final FfmpegCompositionProperties properties = new FfmpegCompositionProperties();
    private final FfmpegFilterGraphBuilder builder = new FfmpegFilterGraphBuilder(properties);

    @Test
    void buildsHardCutGraphWithNormalizedSilentAudio() {
        CompositionSpec spec = spec(TransitionType.NONE, 0, AspectRatio.PORTRAIT, 2);

        FfmpegFilterGraph graph = builder.build(spec, List.of(
            new MediaInfo(5, 1280, 720, false, 2),
            new MediaInfo(6, 720, 1280, true, 1)
        ));

        assertEquals(11, graph.expectedDurationSeconds(), 0.000001);
        assertEquals(new VideoCanvas(1080, 1920, 30), graph.canvas());
        assertEquals("vout", graph.videoLabel());
        assertEquals("aout", graph.audioLabel());
        assertTrue(graph.value().contains("scale=1080:1920:force_original_aspect_ratio=decrease"));
        assertTrue(graph.value().contains("pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black"));
        assertTrue(graph.value().contains("anullsrc=r=48000:cl=stereo"));
        assertTrue(graph.value().contains("[0:2]scale=1080:1920"));
        assertTrue(graph.value().contains("[1:1]scale=1080:1920"));
        assertTrue(graph.value().contains("[1:a:0]aresample=48000"));
        assertTrue(graph.value().contains("[v0][a0][v1][a1]concat=n=2:v=1:a=1[vout][aout]"));
    }

    @Test
    void chainsDissolveOffsetsAgainstCurrentCompositeDuration() {
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 0.5, AspectRatio.LANDSCAPE, 3);

        FfmpegFilterGraph graph = builder.build(spec, List.of(
            new MediaInfo(5, 1920, 1080, true, 0),
            new MediaInfo(6, 1920, 1080, true, 0),
            new MediaInfo(4, 1920, 1080, false, 0)
        ));

        assertEquals(14, graph.expectedDurationSeconds(), 0.000001);
        assertEquals(new VideoCanvas(1920, 1080, 30), graph.canvas());
        assertTrue(graph.value().contains("xfade=transition=dissolve:duration=0.5:offset=4.5[vx1]"));
        assertTrue(graph.value().contains("[vx1][v2]xfade=transition=dissolve:duration=0.5:offset=10[vx2]"));
        assertTrue(graph.value().contains("acrossfade=d=0.5:c1=tri:c2=tri"));
    }

    @Test
    void mapsFadeAndSlideToDistinctFfmpegTransitions() {
        FfmpegFilterGraph fade = builder.build(
            spec(TransitionType.FADE, 0.5, AspectRatio.SQUARE, 2),
            List.of(new MediaInfo(3, 512, 512, false, 0), new MediaInfo(3, 512, 512, false, 0))
        );
        FfmpegFilterGraph slide = builder.build(
            spec(TransitionType.SLIDE, 0.5, AspectRatio.SQUARE, 2),
            List.of(new MediaInfo(3, 512, 512, false, 0), new MediaInfo(3, 512, 512, false, 0))
        );

        assertTrue(fade.value().contains("xfade=transition=fadeblack"));
        assertTrue(slide.value().contains("xfade=transition=slideleft"));
    }

    @Test
    void rejectsTransitionLongerThanAnAdjacentClip() {
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 1, AspectRatio.PORTRAIT, 2);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.build(
            spec,
            List.of(new MediaInfo(0.5, 720, 1280, false, 0), new MediaInfo(3, 720, 1280, false, 0))
        ));

        assertTrue(error.getMessage().contains("too long"));
    }

    @Test
    void rejectsOverlappingTransitionsInsideAMiddleClip() {
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 0.5, AspectRatio.PORTRAIT, 3);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.build(
            spec,
            List.of(
                new MediaInfo(3, 720, 1280, false, 0),
                new MediaInfo(0.999, 720, 1280, false, 0),
                new MediaInfo(3, 720, 1280, false, 0)
            )
        ));

        assertTrue(error.getMessage().contains("overlap"));
    }

    @Test
    void acceptsTouchingTransitionWindowsInsideAMiddleClip() {
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 0.5, AspectRatio.PORTRAIT, 3);

        FfmpegFilterGraph graph = builder.build(spec, List.of(
            new MediaInfo(3, 720, 1280, false, 0),
            new MediaInfo(1, 720, 1280, false, 0),
            new MediaInfo(3, 720, 1280, false, 0)
        ));

        assertEquals(6, graph.expectedDurationSeconds(), 0.000001);
    }

    @Test
    void rejectsTransitionDurationsWithMoreThanThreeDecimalPlaces() {
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 0.1234, AspectRatio.PORTRAIT, 2);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.build(
            spec,
            List.of(
                new MediaInfo(3, 720, 1280, false, 0),
                new MediaInfo(3, 720, 1280, false, 0)
            )
        ));

        assertTrue(error.getMessage().contains("3 decimal places"));
    }

    @Test
    void requiresAtLeastOneFrameForATransition() {
        CompositionSpec tooShort = spec(TransitionType.DISSOLVE, 0.033, AspectRatio.PORTRAIT, 2);
        List<MediaInfo> media = List.of(
            new MediaInfo(3, 720, 1280, false, 0),
            new MediaInfo(3, 720, 1280, false, 0)
        );

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(tooShort, media)
        );
        assertTrue(error.getMessage().contains("one output frame"));

        FfmpegFilterGraph graph = builder.build(
            spec(TransitionType.DISSOLVE, 0.034, AspectRatio.PORTRAIT, 2),
            media
        );
        assertTrue(graph.value().contains("duration=0.034"));
    }

    @Test
    void usesConfiguredMaximumTransitionDuration() {
        properties.setMaxTransitionSeconds(0.75);
        CompositionSpec spec = spec(TransitionType.DISSOLVE, 0.751, AspectRatio.PORTRAIT, 2);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.build(
            spec,
            List.of(
                new MediaInfo(3, 720, 1280, false, 0),
                new MediaInfo(3, 720, 1280, false, 0)
            )
        ));

        assertTrue(error.getMessage().contains("0.75 seconds"));
    }

    @Test
    void normalizesThePersistedAndExecutedTransitionValueWithoutRounding() {
        BigDecimal normalized = properties.normalizeTransitionDuration(
            TransitionType.DISSOLVE,
            new BigDecimal("0.500")
        );

        assertEquals(new BigDecimal("0.5"), normalized);
    }

    private static CompositionSpec spec(
        TransitionType transition,
        double duration,
        AspectRatio ratio,
        int sourceCount
    ) {
        List<CompositionSource> sources = java.util.stream.IntStream.range(0, sourceCount)
            .mapToObj(index -> new CompositionSource(Path.of("clip-" + index + ".mp4"), null))
            .toList();
        return new CompositionSpec(sources, transition, duration, ratio, null, false);
    }
}
