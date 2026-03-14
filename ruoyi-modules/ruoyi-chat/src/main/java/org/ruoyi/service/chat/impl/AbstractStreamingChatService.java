package org.ruoyi.service.chat.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.chat.ChatContext;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.chat.service.chatMessage.AbstractChatMessageService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

/**
 * 流式聊天服务抽象基类
 * <p>
 * 提供核心的流式对话能力：
 * 1. 构建流式聊天模型
 * 2. 创建响应处理器
 * 3. 消息持久化
 * <p>
 * 设计原则：
 * - 抽象层只依赖业务模型，不依赖具体SDK
 * - 子类负责将业务模型转换为厂商SDK格式
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Slf4j
@Validated
public abstract class AbstractStreamingChatService extends AbstractChatMessageService implements IChatService {

    /**
     * 定义聊天流程骨架
     * 注意：此方法已被 Handler 模式取代，保留是为了兼容旧调用
     */
    @Override
    @Deprecated
    public SseEmitter chat(ChatContext chatContext) {
        ChatModelVo chatModelVo = chatContext.getChatModelVo();
        ChatRequest chatRequest = chatContext.getChatRequest();
        Long userId = chatContext.getUserId();
        String tokenValue = chatContext.getTokenValue();
        SseEmitter emitter = chatContext.getEmitter();

        try {
            // 提取用户消息内容
            String content = extractUserContent(chatRequest);

            // 保存用户消息
            saveChatMessage(chatRequest, userId, content, RoleType.USER.getName(), chatModelVo);

            // 构建消息列表（由 Handler 负责构建，这里简单处理）
            List<ChatMessage> messages = convertToChatMessages(chatRequest);

            // 创建响应处理器
            StreamingChatResponseHandler handler = createResponseHandler(
                chatRequest, userId, tokenValue, chatModelVo);

            // 调用具体实现的聊天方法
            doChat(chatModelVo, chatRequest, messages, handler);

        } catch (Exception e) {
            SseMessageUtils.sendMessage(userId, "对话出错：" + e.getMessage());
            SseMessageUtils.completeConnection(userId, tokenValue);
            log.error("{}请求失败：{}", getProviderName(), e.getMessage(), e);
        }

        return emitter;
    }

    /**
     * 提取用户消息内容
     */
    private String extractUserContent(ChatRequest chatRequest) {
        return Optional.ofNullable(chatRequest.getMessages())
            .filter(messages -> !messages.isEmpty())
            .map(messages -> messages.get(0).getContent())
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> Optional.ofNullable(chatRequest.getChatMessages())
                .orElse(List.of()).stream()
                .filter(message -> message instanceof UserMessage)
                .map(message -> ((UserMessage) message).singleText())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(""));
    }

    /**
     * 转换消息格式
     */
    private List<ChatMessage> convertToChatMessages(ChatRequest chatRequest) {
        List<ChatMessage> chatMessages = chatRequest.getChatMessages();
        return chatMessages != null ? chatMessages : List.of();
    }

    /**
     * 执行聊天（钩子方法 - 子类必须实现）
     *
     * @param chatModelVo       模型配置
     * @param chatRequest       聊天请求
     * @param messagesWithMemory 消息列表
     * @param handler           响应处理器
     */
    protected abstract void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest,
                                   List<ChatMessage> messagesWithMemory, StreamingChatResponseHandler handler);

    /**
     * 创建标准的响应处理器
     *
     * @param chatRequest 聊天请求
     * @param userId      用户ID
     * @param tokenValue  会话令牌
     * @param chatModelVo 模型配置
     * @return 流式响应处理器
     */
    protected StreamingChatResponseHandler createResponseHandler(ChatRequest chatRequest, Long userId,
                                                                 String tokenValue, ChatModelVo chatModelVo) {
        return new StreamingChatResponseHandler() {
            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                messageBuffer.append(partialResponse);
                SseMessageUtils.sendMessage(userId, partialResponse);
                log.debug("收到{}消息片段: {}", getProviderName(), partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    String fullMessage = messageBuffer.toString();
                    if (!fullMessage.isEmpty()) {
                        saveChatMessage(chatRequest, userId, fullMessage,
                            RoleType.ASSISTANT.getName(), chatModelVo);
                    }
                    SseMessageUtils.completeConnection(userId, tokenValue);
                    log.info("{}消息结束，已保存到数据库", getProviderName());
                } catch (Exception e) {
                    log.error("{}完成响应时出错: {}", getProviderName(), e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("{}流式响应错误: {}", getProviderName(), error.getMessage(), error);
                try {
                    String errorMessage = String.format("模型调用失败: %s", error.getMessage());
                    SseMessageUtils.sendMessage(userId, errorMessage);
                    SseMessageUtils.completeConnection(userId, tokenValue);
                } catch (Exception e) {
                    log.error("发送错误消息失败: {}", e.getMessage(), e);
                }
            }
        };
    }

    /**
     * 获取提供者名称（子类必须实现）
     */
    public abstract String getProviderName();

    /**
     * 创建流式聊天模型（子类必须实现）
     *
     * @param chatModelVo 模型配置
     * @param chatRequest 聊天请求
     * @return 流式聊天模型实例
     */
    public abstract StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest);
}
