package org.ruoyi.common.chat.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 配置项
 *
 * @author zendwang
 */
@ConfigurationProperties("websocket")
@Data
public class WebSocketProperties {

    private Boolean enabled;

    /**
     * 路径
     */
    private String path;

    /**
     *  设置访问源地址
     */
    private String allowedOrigins;
}
