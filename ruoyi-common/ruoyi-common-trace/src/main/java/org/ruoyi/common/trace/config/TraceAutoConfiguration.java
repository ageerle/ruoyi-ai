package org.ruoyi.common.trace.config;

import org.ruoyi.common.trace.aspect.TraceNodeAspect;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 通用链路追踪自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(TraceProperties.class)
public class TraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceNodeAspect traceNodeAspect(TraceRecordService traceRecordService, TraceProperties traceProperties) {
        return new TraceNodeAspect(traceRecordService, traceProperties);
    }
}
