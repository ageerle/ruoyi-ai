package org.ruoyi.service.chat.impl.memory;

import lombok.RequiredArgsConstructor;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory 配置类
 *
 * @author yang
 * @date 2026-04-27
 */
@Configuration
@EnableConfigurationProperties(ChatMemoryProperties.class)
@RequiredArgsConstructor
public class ChatMemoryConfig {

    private final IChatMessageService chatMessageService;

    /**
     * 持久化存储 Bean
     */
    @Bean
    public PersistentChatMemoryStore persistentChatMemoryStore() {
        return new PersistentChatMemoryStore(chatMessageService);
    }

    /**
     * Token 计数器 Bean
     */
    @Bean
    public TokenCounter tokenCounter() {
        return new TokenCounter();
    }
}
