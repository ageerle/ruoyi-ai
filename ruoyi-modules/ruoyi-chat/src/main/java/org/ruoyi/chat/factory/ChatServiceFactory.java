package org.ruoyi.chat.factory;

import org.ruoyi.chat.service.chat.IChatService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务工厂类
 *
 * @author ageerle@163.com
 * date 2025/5/10
 */
@Component
public class ChatServiceFactory  implements ApplicationContextAware {
    private final Map<String, IChatService> chatServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有IChatService的实现
        Map<String, IChatService> serviceMap = applicationContext.getBeansOfType(IChatService.class);
        for (IChatService service : serviceMap.values()) {
            if (service != null) {
                chatServiceMap.put(service.getCategory(), service);
            }
        }
    }

    /**
     * 根据模型类别获取对应的聊天服务实现
     */
    public IChatService getChatService(String category) {
        IChatService service = chatServiceMap.get(category);
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型类别: " + category);
        }
        return service;
    }
}
