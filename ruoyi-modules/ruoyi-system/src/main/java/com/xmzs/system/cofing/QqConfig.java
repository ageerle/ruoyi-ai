package com.xmzs.system.cofing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * qq配置
 *
 * @author ashinnotfound
 * @date 2023/03/04
 */
@Data
@Component
@ConfigurationProperties("qq")
public class QqConfig {
    private Boolean enable;
    private Long account;
    private Boolean acceptNewFriend;
    private Boolean acceptNewGroup;
}
