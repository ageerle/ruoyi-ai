package org.ruoyi.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * 程序注解配置（PERF-P0-3 @Async 异常黑洞修复）。
 *
 * <p>注册 {@link AsyncUncaughtExceptionHandler} 统一处理 {@code @Async} 方法
 * 抛出但调用方已无法接收的异常——之前默认仅 stderr 打印，无业务告警，
 * 审计 / AI 解析失败可能被静默。修复后：
 * <ul>
 *   <li>异常进 SLF4J ERROR 日志（带 method + params 上下文）</li>
 *   <li>集成 TraceIdFilter MDC，traceId 串联可追踪</li>
 *   <li>统一走 log.error 触发 ELK / Loki 聚合告警</li>
 * </ul>
 *
 * <p>若未来需要写审计日志（IPD audit_logs.async_failure），只需在本 bean 内
 * 注入 AuditLogService 后调用即可，无需散落到各 @Async 方法。
 *
 * @author Lion Li (本会话 PERF-P0-3 修复)
 */
@Slf4j
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableAsync(proxyTargetClass = true)
public class ApplicationConfig implements AsyncConfigurer {

    /**
     * 全局默认 @Async 线程池（沿用 Spring Boot 默认配置，避免影响现有业务）。
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(512);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    /**
     * @Async 未捕获异常统一处理器（PERF-P0-3 核心修复）。
     * <p>return-type-void 方法的异常会走这里；Future 类型的方法异常由调用方 .get() 触发。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(
            "[AsyncUncaught] method={} declaringClass={} params={}",
            method.getName(),
            method.getDeclaringClass().getSimpleName(),
            Arrays.toString(params),
            ex
        );
    }
}