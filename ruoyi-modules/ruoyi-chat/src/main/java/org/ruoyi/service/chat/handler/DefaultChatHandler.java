package org.ruoyi.service.chat.handler;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认对话处理器
 * <p>
 * 处理普通对话场景，包含：
 * 1. 历史记忆管理
 * 2. 消息保存
 * 3. 流式对话响应
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Slf4j
@Component
@Order(100)
public class DefaultChatHandler implements ChatHandler {

    private final Map<String, AbstractStreamingChatService> chatServiceMap;

    /**
     * 默认保留的消息窗口大小
     */
    private static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * 是否启用长期记忆
     */
    private static final boolean ENABLE_PERSISTENT_MEMORY = true;

    /**
     * 内存实例缓存
     */
    private static final Map<Object, MessageWindowChatMemory> MEMORY_CACHE = new ConcurrentHashMap<>();

    /**
     * 构造函数，注入所有聊天服务实现
     */
    public DefaultChatHandler(List<AbstractStreamingChatService> chatServices) {
        this.chatServiceMap = chatServices.stream()
            .collect(Collectors.toMap(
                AbstractStreamingChatService::getProviderName,
                Function.identity()
            ));
        log.info("已加载 {} 个聊天服务: {}", chatServiceMap.size(), chatServiceMap.keySet());
    }

    /**
     * 根据 providerCode 获取对应的聊天服务
     */
    private AbstractStreamingChatService getChatService(String providerCode) {
        if (StringUtils.isBlank(providerCode)) {
            // 默认使用千问服务
            return chatServiceMap.get("qianwen");
        }
        AbstractStreamingChatService service = chatServiceMap.get(providerCode.toLowerCase());
        if (service == null) {
            log.warn("未找到提供商 {} 对应的服务，使用默认千问服务", providerCode);
            return chatServiceMap.get("qianwen");
        }
        return service;
    }

    @Override
    public boolean supports(ChatContext context) {
        // 默认处理器，始终支持
        return true;
    }

    @Override
    public SseEmitter handle(ChatContext context) {
        log.info("处理默认对话，用户: {}, 会话: {}",
            context.getUserId(), context.getChatRequest().getSessionId());

        Long userId = context.getUserId();
        String tokenValue = context.getTokenValue();

        // 根据 providerCode 获取对应的聊天服务
        String providerCode = context.getChatModelVo().getProviderCode();
        AbstractStreamingChatService chatService = getChatService(providerCode);
        log.info("使用服务提供商: {}", chatService.getProviderName());

        try {
            // 1. 提取用户消息内容
            String content = extractUserContent(context);

            // 2. 保存用户消息
            chatService.saveChatMessage(context.getChatRequest(), userId, content,
                RoleType.USER.getName(), context.getChatModelVo());

            // 3. 构建包含历史记忆的消息列表
            List<ChatMessage> messagesWithMemory = buildMessagesWithMemory(context.getChatRequest());

            // 4. 创建响应处理器
            StreamingChatResponseHandler handler = createResponseHandler(
                context.getChatRequest(), userId, tokenValue, context.getChatModelVo(), chatService);

            // 5. 构建流式模型并执行对话
            StreamingChatModel streamingModel = chatService.buildStreamingChatModel(
                context.getChatModelVo(), context.getChatRequest());
            streamingModel.chat(messagesWithMemory, handler);

        } catch (Exception e) {
            log.error("对话处理失败: {}", e.getMessage(), e);
            SseMessageUtils.sendMessage(userId, "对话出错：" + e.getMessage());
            SseMessageUtils.completeConnection(userId, tokenValue);
        }

        return context.getEmitter();
    }

    /**
     * 提取用户消息内容
     */
    private String extractUserContent(ChatContext context) {
        return Optional.ofNullable(context.getChatRequest().getMessages())
            .filter(messages -> !messages.isEmpty())
            .map(messages -> messages.get(0).getContent())
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> Optional.ofNullable(context.getChatRequest().getChatMessages())
                .orElse(List.of()).stream()
                .filter(message -> message instanceof UserMessage)
                .map(message -> ((UserMessage) message).singleText())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(""));
    }

    /**
     * 构建包含历史消息的消息列表
     */
    private List<ChatMessage> buildMessagesWithMemory(org.ruoyi.common.chat.domain.dto.request.ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();

        // 添加工作流对话消息
        List<ChatMessage> chatMessages = chatRequest.getChatMessages();
        if (!CollectionUtils.isEmpty(chatMessages)) {
            messages.addAll(chatMessages);
        }

        // 添加历史记忆
        if (ENABLE_PERSISTENT_MEMORY && chatRequest.getSessionId() != null) {
            MessageWindowChatMemory memory = createChatMemory(chatRequest.getSessionId());
            if (memory != null) {
                List<ChatMessage> historicalMessages = memory.messages();
                if (historicalMessages != null && !historicalMessages.isEmpty()) {
                    messages.addAll(historicalMessages);
                    log.debug("已加载 {} 条历史消息用于会话 {}", historicalMessages.size(), chatRequest.getSessionId());
                }
            }
        }

        return messages;
    }

    /**
     * 创建或获取聊天内存实例
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        return MEMORY_CACHE.computeIfAbsent(memoryId, key -> {
            try {
                PersistentChatMemoryStore store = new PersistentChatMemoryStore();
                return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(DEFAULT_MAX_MESSAGES)
                    .chatMemoryStore(store)
                    .build();
            } catch (Exception e) {
                log.warn("创建聊天内存失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 创建响应处理器
     */
    private StreamingChatResponseHandler createResponseHandler(
            org.ruoyi.common.chat.domain.dto.request.ChatRequest chatRequest,
            Long userId,
            String tokenValue,
            org.ruoyi.common.chat.domain.vo.chat.ChatModelVo chatModelVo,
            AbstractStreamingChatService chatService) {

        return new StreamingChatResponseHandler() {
            private final StringBuilder messageBuffer = new StringBuilder();

            @Override
            public void onPartialResponse(String partialResponse) {
                messageBuffer.append(partialResponse);
                SseMessageUtils.sendMessage(userId, partialResponse);
                log.debug("收到消息片段: {}", partialResponse);
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                try {
                    String fullMessage = messageBuffer.toString();
                    if (!fullMessage.isEmpty()) {
                        chatService.saveChatMessage(chatRequest, userId, fullMessage,
                            RoleType.ASSISTANT.getName(), chatModelVo);
                    }
                    SseMessageUtils.completeConnection(userId, tokenValue);
                    log.info("消息结束，已保存到数据库");
                } catch (Exception e) {
                    log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式响应错误: {}", error.getMessage(), error);
                try {
                    SseMessageUtils.sendMessage(userId, "模型调用失败: " + error.getMessage());
                    SseMessageUtils.completeConnection(userId, tokenValue);
                } catch (Exception e) {
                    log.error("发送错误消息失败: {}", e.getMessage(), e);
                }
            }
        };
    }
}
