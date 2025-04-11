package org.ruoyi.chat.factory;


import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.service.chat.impl.OllamaServiceImpl;
import org.ruoyi.chat.service.chat.impl.OpenAIServiceImpl;

import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SseServiceFactory {

    public IChatService getSseService(String type) {
        if ("openai".equals(type)) {
            return new OpenAIServiceImpl();
        } else if ("ollama".equals(type)) {
            return new OllamaServiceImpl();
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
