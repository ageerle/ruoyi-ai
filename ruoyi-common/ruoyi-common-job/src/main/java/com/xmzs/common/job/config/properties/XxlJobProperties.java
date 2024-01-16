package com.xmzs.common.job.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * xxljob配置类
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    private Boolean enabled;

    private String adminAddresses;

    private String accessToken;

    private Executor executor;

    @Data
    @NoArgsConstructor
    public static class Executor {

        private String appname;

        private String address;

        private String ip;

        private int port;

        private String logPath;

        private int logRetentionDays;
    }
}
