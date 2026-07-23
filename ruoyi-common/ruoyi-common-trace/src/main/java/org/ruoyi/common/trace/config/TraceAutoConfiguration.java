package org.ruoyi.common.trace.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 通用链路追踪自动配置。
 * <p>
 * 仅注册配置属性；节点采集通过 {@code TraceNodeTemplate} / {@code DefaultTraceStreamSpan} 编程式埋点完成。
 */
@AutoConfiguration
@EnableConfigurationProperties(TraceProperties.class)
public class TraceAutoConfiguration {
}
