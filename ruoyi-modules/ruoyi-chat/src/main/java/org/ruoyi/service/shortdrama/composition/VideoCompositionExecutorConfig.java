package org.ruoyi.service.shortdrama.composition;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class VideoCompositionExecutorConfig {

    private final FfmpegCompositionProperties properties;

    @Bean(name = "videoCompositionExecutor")
    public ThreadPoolTaskExecutor videoCompositionExecutor() {
        if (properties.getWorkerCoreSize() <= 0
            || properties.getWorkerMaxSize() < properties.getWorkerCoreSize()
            || properties.getWorkerQueueCapacity() < 0) {
            throw new IllegalArgumentException("Invalid video composition executor configuration");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getWorkerCoreSize());
        executor.setMaxPoolSize(properties.getWorkerMaxSize());
        executor.setQueueCapacity(properties.getWorkerQueueCapacity());
        executor.setThreadNamePrefix("video-compose-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
