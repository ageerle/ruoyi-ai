package org.ruoyi.common.chat.service.chat;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.validation.Valid;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公共大模型对话接口
 */
public interface IChatService {

    /**
     * 客户端发送对话消息到服务端
     */
    SseEmitter chat(@Valid ChatRequest chatRequest);

    /**
     * 支持外部 handler 的对话接口（跨模块调用）
     * 同时发送到 SSE 和外部 handler
     *
     * @param chatRequest    聊天请求
     * @param externalHandler 外部响应处理器（可为 null）
     */
    void chat(@Valid ChatRequest chatRequest, StreamingChatResponseHandler externalHandler);

}
