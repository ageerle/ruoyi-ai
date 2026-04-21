package org.ruoyi.common.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示模式 配置属性
 *
 * @author ruoyi
 */
@Data
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    /**
     * 是否开启演示模式
     */
    private Boolean enabled = false;

    /**
     * 提示消息
     */
    private String message = "演示模式，不允许进行写操作";

    /**
     * 排除的路径（这些路径不受演示模式限制）
     */
    private List<String> excludes = new ArrayList<>();
}