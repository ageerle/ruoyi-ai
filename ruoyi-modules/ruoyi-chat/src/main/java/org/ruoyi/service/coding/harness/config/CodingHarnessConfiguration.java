package org.ruoyi.service.coding.harness.config;

import org.ruoyi.service.coding.harness.journal.FileRunJournalProjector;
import org.ruoyi.service.coding.harness.journal.RunJournalProjector;
import org.ruoyi.service.coding.harness.journal.StructuredContextSnapshotFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CodingHarnessConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "coding.harness.journal", name = "enabled",
        havingValue = "true")
    public StructuredContextSnapshotFactory codingHarnessStructuredContextSnapshotFactory() {
        return new StructuredContextSnapshotFactory();
    }

    @Bean
    @ConditionalOnProperty(prefix = "coding.harness.journal", name = "enabled",
        havingValue = "true")
    public RunJournalProjector codingHarnessRunJournalProjector(
        @Value("${coding.harness.journal.output-directory:./data/coding-harness-journal}")
        String outputDirectory) {
        if (outputDirectory == null || outputDirectory.isBlank()) {
            throw new IllegalArgumentException("Coding Harness journal output directory is required");
        }
        return new FileRunJournalProjector(Path.of(outputDirectory));
    }

    @Bean(name = "codingHarnessDeliveryExecutor")
    public Executor codingHarnessDeliveryExecutor(
        @Value("${coding.harness.delivery.core-pool-size:2}") int corePoolSize,
        @Value("${coding.harness.delivery.max-pool-size:8}") int maxPoolSize,
        @Value("${coding.harness.delivery.queue-capacity:2048}") int queueCapacity) {
        return scalableBoundedExecutor("coding-harness-events-", corePoolSize, maxPoolSize,
            queueCapacity, 10);
    }

    @Bean(name = "codingHarnessRunExecutor")
    public Executor codingHarnessRunExecutor(
        @Value("${coding.harness.runner.core-pool-size:2}") int corePoolSize,
        @Value("${coding.harness.runner.max-pool-size:4}") int maxPoolSize,
        @Value("${coding.harness.runner.queue-capacity:256}") int queueCapacity) {
        return scalableBoundedExecutor("coding-harness-run-", corePoolSize, maxPoolSize,
            queueCapacity, 30);
    }

    private ThreadPoolTaskExecutor scalableBoundedExecutor(String threadNamePrefix,
                                                            int corePoolSize,
                                                            int maxPoolSize,
                                                            int queueCapacity,
                                                            int awaitTerminationSeconds) {
        if (corePoolSize <= 0 || maxPoolSize < corePoolSize || queueCapacity <= 0) {
            throw new IllegalArgumentException("Invalid coding Harness executor limits");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        // ThreadPoolExecutor fills its queue before growing beyond corePoolSize. Harness work can
        // block for a model/SSE consumer, so use maxPoolSize as the actual concurrency and let all
        // idle core threads time out instead of silently running at the smaller core setting.
        executor.setCorePoolSize(maxPoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "codingHarnessToolExecutor", destroyMethod = "shutdown")
    public ExecutorService codingHarnessToolExecutor(
        @Value("${coding.harness.tools.core-pool-size:2}") int corePoolSize,
        @Value("${coding.harness.tools.max-pool-size:8}") int maxPoolSize,
        @Value("${coding.harness.tools.queue-capacity:256}") int queueCapacity) {
        if (corePoolSize <= 0 || maxPoolSize < corePoolSize || queueCapacity <= 0) {
            throw new IllegalArgumentException("Invalid coding Harness tool executor limits");
        }
        AtomicInteger threadIds = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                "coding-harness-tool-" + threadIds.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity), threadFactory,
            new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean(name = "codingHarnessModelTimeoutScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService codingHarnessModelTimeoutScheduler(
        @Value("${coding.harness.model-timeout.threads:1}") int threads) {
        if (threads <= 0 || threads > 8) {
            throw new IllegalArgumentException("Invalid coding Harness model timeout thread count");
        }
        AtomicInteger threadIds = new AtomicInteger();
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(threads,
            runnable -> {
                Thread thread = new Thread(runnable,
                    "coding-harness-model-timeout-" + threadIds.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return scheduler;
    }

    @Bean(name = "codingHarnessMaintenanceScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService codingHarnessMaintenanceScheduler() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,
            runnable -> {
                Thread thread = new Thread(runnable, "coding-harness-maintenance");
                thread.setDaemon(true);
                return thread;
            });
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return scheduler;
    }
}
