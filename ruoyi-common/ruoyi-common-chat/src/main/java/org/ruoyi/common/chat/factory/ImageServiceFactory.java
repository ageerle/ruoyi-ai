package org.ruoyi.common.chat.factory;

import org.ruoyi.common.chat.service.image.IImageGenerationService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文生图服务工厂类
 *
 * @author zengxb
 * @date 2026-02-14
 */
@Component
public class ImageServiceFactory implements ApplicationContextAware {

    private final Map<String, IImageGenerationService> imageSerivceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有IImageGenerationService的实现
        Map<String, IImageGenerationService> serviceMap = applicationContext.getBeansOfType(IImageGenerationService.class);
        for (IImageGenerationService service : serviceMap.values()) {
            if (service != null ) {
                imageSerivceMap.put(service.getProviderName(), service);
            }
        }
    }


    /**
     * 获取原始服务（不包装代理）
     */
    public IImageGenerationService getOriginalService(String category) {
        IImageGenerationService service = imageSerivceMap.get(category);
        if (service == null) {
            throw new IllegalArgumentException("不支持的模型类别: " + category);
        }
        return service;
    }
}
