package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天记忆提供者工厂
 * 为每个会话创建独立的ChatMemoryProvider实例
 * 支持消息窗口滑动和持久化存储
 *
 * @author ageerle@163.com
 * @date 2025/01/10
 */
@Slf4j
public class ChatMemoryProviderFactory {

    /**
     * 默认保留的消息数量（不包括当前消息）
     */
    private static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * 创建聊天记忆提供者
     *
     * @param maxMessages 最多保留的消息数量
     * @return ChatMemoryProvider实例
     */
    public static ChatMemoryProvider createChatMemoryProvider(int maxMessages) {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(store)
                .build();
    }

    /**
     * 使用默认消息数量创建聊天记忆提供者
     *
     * @return ChatMemoryProvider实例
     */
    public static ChatMemoryProvider createChatMemoryProvider() {
        return createChatMemoryProvider(DEFAULT_MAX_MESSAGES);
    }

    /**
     * 创建自定义的聊天记忆提供者
     * 允许更灵活的配置
     *
     * @param maxMessages    最多保留的消息数量
     * @param chatMemoryStore 自定义的存储实现
     * @return ChatMemoryProvider实例
     */
    public static ChatMemoryProvider createChatMemoryProvider(int maxMessages,
                                                              dev.langchain4j.store.memory.chat.ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}