package com.xmzs.system.cofing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置
 *
 * @author ashinnotfound
 * @date 2023/03/19
 */
@Data
@Component
@ConfigurationProperties("wechat")
public class WechatConfig {
    private Boolean enable;
    private String qrPath;
}
