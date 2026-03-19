package org.ruoyi.factory;

import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.service.chat.AbstractChatService;
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
 * @date 2025-12-13
 */
@Component
public class ChatServiceFactory implements ApplicationContextAware {

    private final Map<String, AbstractChatService> chatServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有IChatService的实现
        Map<String, AbstractChatService> serviceMap = applicationContext.getBeansOfType(AbstractChatService.class);
        for (AbstractChatService service : serviceMap.values()) {
            if (service != null ) {
                chatServiceMap.put(service.getProviderName(), service);
            }
        }
    }


    /**
     * 获取原始服务（不包装代理）
     */
    public AbstractChatService getOriginalService(String category) {
        AbstractChatService service = chatServiceMap.get(category);
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型类别: " + category);
        }
        return service;
    }

}
