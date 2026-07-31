package org.ruoyi.common.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 通用链路追踪配置。
 */
@Data
@ConfigurationProperties(prefix = "trace")
public class TraceProperties {

    /**
     * 是否启用链路追踪。
     */
    private boolean enabled = true;

    /**
     * payload 记录策略。
     */
    private Payload payload = new Payload();

    @Data
    public static class Payload {

        /**
         * 错误信息最大长度。
         */
        private int maxErrorLength = 1000;
    }
}
