package com.xmzs.system.cofing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 关键字配置
 *
 * @author ashinnotfound
 * @date 2023/08/10
 */
@Data
@Component
@ConfigurationProperties("keyword")
public class KeywordConfig {
    private String reset;
    private String image;
    private String audio;
}
