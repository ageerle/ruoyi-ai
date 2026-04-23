package org.ruoyi.service.chat.impl.provider.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.imfangs.dify.client.DifyChatClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.chat.impl.provider.DifyConversationService;

import java.util.List;

/**
 * Dify 流式聊天模型适配器
 * <p>
 * 将 Dify 的回调式流式响应适配为 langchain4j 的 StreamingChatModel 接口，
 * 使 ChatServiceFacade 可以像其他 provider 一样统一调用。
 *
 * @author better
 */
@Slf4j
public class DifyStreamingChatModel implements StreamingChatModel {

    private final ChatModelVo chatModelVo;
    private final ChatRequest chatRequest;
    private final DifyConversationService conversationService;

    public DifyStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest,
                                  DifyConversationService conversationService) {
        this.chatModelVo = chatModelVo;
        this.chatRequest = chatRequest;
        this.conversationService = conversationService;
    }

    @Override
    public void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        // 1. 从 langchain4j 消息列表中提取最后一条用户消息作为 query
        String query = extractUserQuery(messages);

        // 2. 获取 Dify conversation_id（多轮对话连续性）
        String conversationId = null;
        if (chatRequest.getSessionId() != null) {
            conversationId = conversationService.getConversationId(chatRequest.getSessionId());
        }

        // 3. 构建 Dify 请求
        io.github.imfangs.dify.client.model.chat.ChatMessage difyMessage = io.github.imfangs.dify.client.model.chat.ChatMessage.builder()
                .query(query)
                .user(String.valueOf(chatRequest.getUserId()))
                .responseMode(ResponseMode.STREAMING)
                .conversationId(conversationId)
                .autoGenerateName(true)
                .build();

        // 4. 创建 Dify 客户端并发送流式请求
        try {
            DifyChatClient client = DifyClientFactory.createChatClient(
                    normalizeBaseUrl(chatModelVo.getApiHost()),
                    chatModelVo.getApiKey());

            client.sendChatMessageStream(difyMessage, new DifyChatStreamAdapter(handler));
        } catch (Exception e) {
            log.error("Dify 流式对话调用失败", e);
            handler.onError(e);
        }
    }

    @Override
    public void chat(String userMessage, StreamingChatResponseHandler handler) {
        io.github.imfangs.dify.client.model.chat.ChatMessage difyMessage = io.github.imfangs.dify.client.model.chat.ChatMessage.builder()
                .query(userMessage)
                .user(String.valueOf(chatRequest.getUserId()))
                .responseMode(ResponseMode.STREAMING)
                .conversationId(chatRequest.getSessionId() != null
                        ? conversationService.getConversationId(chatRequest.getSessionId()) : null)
                .autoGenerateName(true)
                .build();

        try {
            DifyChatClient client = DifyClientFactory.createChatClient(
                    normalizeBaseUrl(chatModelVo.getApiHost()),
                    chatModelVo.getApiKey());

            client.sendChatMessageStream(difyMessage, new DifyChatStreamAdapter(handler));
        } catch (Exception e) {
            log.error("Dify 流式对话调用失败", e);
            handler.onError(e);
        }
    }

    /**
     * 从 langchain4j 消息列表中提取最后一条用户消息文本
     */
    private String extractUserQuery(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).singleText();
            }
        }
        return "";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Dify API 地址(apiHost)不能为空");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Dify 回调适配器
     * 将 Dify ChatStreamCallback 事件转发给 langchain4j StreamingChatResponseHandler
     */
    private class DifyChatStreamAdapter implements io.github.imfangs.dify.client.callback.ChatStreamCallback {

        private final StreamingChatResponseHandler handler;
        private final StringBuilder fullResponse = new StringBuilder();

        DifyChatStreamAdapter(StreamingChatResponseHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onMessage(MessageEvent event) {
            String answer = event.getAnswer();
            if (answer != null) {
                fullResponse.append(answer);
                handler.onPartialResponse(answer);
            }
            // 保存 Dify conversation_id 以维持多轮对话
            if (event.getConversationId() != null && chatRequest.getSessionId() != null) {
                conversationService.saveMapping(chatRequest.getSessionId(), event.getConversationId());
            }
        }

        @Override
        public void onMessageEnd(MessageEndEvent event) {
            // 保存 conversation_id
            if (event.getConversationId() != null && chatRequest.getSessionId() != null) {
                conversationService.saveMapping(chatRequest.getSessionId(), event.getConversationId());
            }

            // 构建完整的 ChatResponse 交给上层处理
            AiMessage aiMessage = new AiMessage(fullResponse.toString());
            ChatResponse response = ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .id(event.getMessageId())
                    .build();
            handler.onCompleteResponse(response);
        }

        @Override
        public void onError(ErrorEvent event) {
            handler.onError(new RuntimeException(event.getMessage()));
        }

        @Override
        public void onException(Throwable throwable) {
            handler.onError(throwable);
        }
    }
}
