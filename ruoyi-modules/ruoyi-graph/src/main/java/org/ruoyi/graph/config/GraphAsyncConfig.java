package org.ruoyi.graph.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图谱构建异步任务配置
 *
 * @author ruoyi
 * @date 2025-10-11
 */
@Slf4j
@EnableAsync
@Configuration
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphAsyncConfig {

    /**
     * 图谱构建专用线程池
     * 用于执行图谱构建任务，避免阻塞主线程池
     */
    @Bean("graphBuildExecutor")
    public Executor graphBuildExecutor() {
        log.info("初始化图谱构建线程池...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：CPU核心数
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);

        // 最大线程数：CPU核心数 * 2
        executor.setMaxPoolSize(processors * 2);

        // 队列容量：100个任务
        executor.setQueueCapacity(100);

        // 线程空闲时间：60秒
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀
        executor.setThreadNamePrefix("graph-build-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("图谱构建线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                processors, processors * 2, 100);

        return executor;
    }
}

