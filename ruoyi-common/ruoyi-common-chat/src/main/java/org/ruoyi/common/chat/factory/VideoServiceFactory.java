package org.ruoyi.common.chat.factory;

import org.ruoyi.common.chat.service.video.IVideoGenerationService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Text-to-video service factory.
 */
@Component
public class VideoServiceFactory implements ApplicationContextAware {

    private final Map<String, IVideoGenerationService> videoServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, IVideoGenerationService> serviceMap = applicationContext.getBeansOfType(IVideoGenerationService.class);
        for (IVideoGenerationService service : serviceMap.values()) {
            if (service != null) {
                videoServiceMap.put(service.getProviderName(), service);
            }
        }
    }

    public IVideoGenerationService getOriginalService(String providerCode) {
        IVideoGenerationService service = videoServiceMap.get(providerCode);
        if (service == null && "custom_api".equals(providerCode)) {
            service = videoServiceMap.get("openai");
        }
        if (service == null) {
            throw new IllegalArgumentException("不支持的视频模型供应商: " + providerCode);
        }
        return service;
    }
}
