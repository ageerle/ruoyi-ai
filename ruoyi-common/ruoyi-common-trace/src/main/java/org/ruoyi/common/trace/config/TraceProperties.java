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
         * 是否记录截断后的详情。默认 false，只记录摘要。
         */
        private boolean recordDetail = false;

        /**
         * input payload 最大长度。
         */
        private int maxInputLength = 1000;

        /**
         * output payload 最大长度。
         */
        private int maxOutputLength = 2000;

        /**
         * 错误信息最大长度。
         */
        private int maxErrorLength = 1000;
    }
}
