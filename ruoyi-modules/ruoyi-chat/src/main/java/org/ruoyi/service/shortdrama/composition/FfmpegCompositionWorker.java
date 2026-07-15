package org.ruoyi.service.shortdrama.composition;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class FfmpegCompositionWorker {

    private final FfmpegVideoComposer videoComposer;

    @Async("videoCompositionExecutor")
    public CompletableFuture<CompositionArtifact> composeAsync(
        CompositionSpec spec,
        Path workDirectory
    ) {
        try {
            return CompletableFuture.completedFuture(videoComposer.compose(spec, workDirectory));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
