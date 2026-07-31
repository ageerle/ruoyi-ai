package org.ruoyi.common.chat.factory;

import org.ruoyi.common.chat.service.audio.IAudioGenerationService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Text-to-speech service factory.
 */
@Component
public class AudioServiceFactory implements ApplicationContextAware {

    private final Map<String, IAudioGenerationService> audioServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, IAudioGenerationService> serviceMap = applicationContext.getBeansOfType(IAudioGenerationService.class);
        for (IAudioGenerationService service : serviceMap.values()) {
            if (service != null) {
                audioServiceMap.put(service.getProviderName(), service);
            }
        }
    }

    public IAudioGenerationService getOriginalService(String providerCode) {
        IAudioGenerationService service = audioServiceMap.get(providerCode);
        if (service == null && "custom_api".equals(providerCode)) {
            service = audioServiceMap.get("openai");
        }
        if (service == null) {
            throw new IllegalArgumentException("不支持的语音模型供应商: " + providerCode);
        }
        return service;
    }
}
