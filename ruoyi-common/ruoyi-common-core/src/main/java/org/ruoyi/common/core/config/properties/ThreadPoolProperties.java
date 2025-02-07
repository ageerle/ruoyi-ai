package org.ruoyi.common.core.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池 配置属性
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    /**
     * 是否开启线程池
     */
    private boolean enabled;

    /**
     * 队列最大长度
     */
    private int queueCapacity;

    /**
     * 线程池维护线程所允许的空闲时间
     */
    private int keepAliveSeconds;

}
