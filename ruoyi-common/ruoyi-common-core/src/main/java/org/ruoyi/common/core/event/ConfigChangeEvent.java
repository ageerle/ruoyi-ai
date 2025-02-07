package org.ruoyi.common.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 描述：定义一个事件类，用于通知配置变化
 *
 * @author ageerle@163.com
 * date 2024/5/19
 */
public class ConfigChangeEvent extends ApplicationEvent {
    public ConfigChangeEvent(Object source) {
        super(source);
    }
}
