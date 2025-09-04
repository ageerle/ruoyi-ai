package org.ruoyi.chat.factory;

import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.service.chat.proxy.BillingChatServiceProxy;
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
    private IChatCostService chatCostService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取计费服务
        this.chatCostService = applicationContext.getBean(IChatCostService.class);
        
        // 初始化时收集所有IChatService的实现
        Map<String, IChatService> serviceMap = applicationContext.getBeansOfType(IChatService.class);
        for (IChatService service : serviceMap.values()) {
            if (service != null && !isBillingProxy(service)) {
                // 只收集非代理的原始服务
                chatServiceMap.put(service.getCategory(), service);
            }
        }
    }

    /**
     * 根据模型类别获取对应的聊天服务实现
     * 自动应用计费代理包装
     */
    public IChatService getChatService(String category) {
        IChatService originalService = chatServiceMap.get(category);
        if (originalService == null) {
            throw new IllegalArgumentException("不支持的模型类别: " + category);
        }

        // 自动包装为计费代理
        return new BillingChatServiceProxy(originalService, chatCostService);
    }

    /**
     * 获取原始服务（不包装代理）
     */
    public IChatService getOriginalService(String category) {
        IChatService service = chatServiceMap.get(category);
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型类别: " + category);
        }
        return service;
    }

    /**
     * 判断是否为计费代理实例
     */
    private boolean isBillingProxy(IChatService service) {
        return service instanceof BillingChatServiceProxy;
    }
}
