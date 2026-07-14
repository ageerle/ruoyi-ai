package org.ruoyi.service.shortdrama.composition;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FfmpegVideoComposer {

    private final FfmpegCompositionProperties properties;
    private final FfmpegMediaProbe mediaProbe;
    private final FfmpegFilterGraphBuilder filterGraphBuilder;
    private final FfmpegCommandBuilder commandBuilder;
    private final FfmpegProcessRunner processRunner;

    public CompositionArtifact compose(CompositionSpec spec, Path workDirectory) {
        validateSources(spec);
        Path workDir = workDirectory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(workDir);
        } catch (IOException ex) {
            throw new MediaProcessException("Unable to create composition work directory", ex);
        }

        String jobToken = UUID.randomUUID().toString();
        List<MediaInfo> media = probeInputs(spec, workDir, jobToken);
        FfmpegFilterGraph filterGraph = filterGraphBuilder.build(spec, media);
        Path filterScript = workDir.resolve("filter-" + jobToken + ".txt");
        Path partialOutput = workDir.resolve("composed-" + jobToken + ".part.mp4");
        Path finalOutput = workDir.resolve("composed-" + jobToken + ".mp4");
        Path composeLog = workDir.resolve("ffmpeg-" + jobToken + ".log");

        boolean complete = false;
        try {
            Files.writeString(filterScript, filterGraph.value(), StandardCharsets.UTF_8);
            List<String> command = commandBuilder.build(spec, filterGraph, filterScript, partialOutput);
            processRunner.run(
                command,
                properties.getProcessTimeout(),
                composeLog,
                properties.getMaxProcessOutputBytes()
            );
            validateOutputFile(partialOutput);

            MediaInfo outputInfo = mediaProbe.probe(
                partialOutput,
                filterGraph.expectedDurationSeconds(),
                workDir.resolve("probe-output-" + jobToken + ".log")
            );
            validateOutputMedia(outputInfo, filterGraph);
            moveCompletedFile(partialOutput, finalOutput);
            complete = true;
            return new CompositionArtifact(
                finalOutput,
                outputInfo.durationSeconds(),
                outputInfo.width(),
                outputInfo.height(),
                spec.transitionType()
            );
        } catch (IOException ex) {
            throw new MediaProcessException("Unable to write composed video artifact", ex);
        } finally {
            if (!complete) {
                try {
                    Files.deleteIfExists(partialOutput);
                } catch (IOException ignored) {
                    // Keep the original composition failure as the primary error.
                }
            }
        }
    }

    private void validateSources(CompositionSpec spec) {
        if (spec.sources().size() > properties.getMaxClips()) {
            throw new IllegalArgumentException("Too many video clips; maximum is " + properties.getMaxClips());
        }
        for (int index = 0; index < spec.sources().size(); index++) {
            Path path = spec.sources().get(index).path();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("Video source " + (index + 1) + " is not a readable file");
            }
        }
    }

    private List<MediaInfo> probeInputs(CompositionSpec spec, Path workDir, String jobToken) {
        List<MediaInfo> result = new ArrayList<>(spec.sources().size());
        for (int index = 0; index < spec.sources().size(); index++) {
            CompositionSource source = spec.sources().get(index);
            result.add(mediaProbe.probe(
                source.path(),
                source.fallbackDurationSeconds(),
                workDir.resolve("probe-input-" + jobToken + "-" + index + ".log")
            ));
        }
        return result;
    }

    private void validateOutputFile(Path output) throws IOException {
        if (!Files.isRegularFile(output) || Files.size(output) < properties.getMinimumOutputBytes()) {
            throw new MediaProcessException("FFmpeg did not produce a valid output file");
        }
    }

    private void validateOutputMedia(MediaInfo output, FfmpegFilterGraph expected) {
        if (output.width() != expected.canvas().width() || output.height() != expected.canvas().height()) {
            throw new MediaProcessException("Composed video dimensions do not match the requested aspect ratio");
        }
        if (!output.hasAudio()) {
            throw new MediaProcessException("Composed video is missing its normalized audio track");
        }
        double tolerance = Math.max(0.25, 3.0 / expected.canvas().fps());
        if (Math.abs(output.durationSeconds() - expected.expectedDurationSeconds()) > tolerance) {
            throw new MediaProcessException(
                "Composed video duration differs from the expected timeline duration"
            );
        }
    }

    private static void moveCompletedFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
