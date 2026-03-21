package org.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mcp.sse")
public class McpSseConfig {

    /**
     * mcp对外暴露的端点地址
     */
    private String url;

    /**
     * 是否开启
     */
    private boolean enabled;
}
