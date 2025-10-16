package org.ruoyi.chat.service.chat;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatService {

    /**
     * 客户端发送消息到服务端
     *
     * @param chatRequest 请求对象
     */
    SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter);

    /**
     * 工作流场景：支持 langchain4j 的 StreamingChatResponseHandler
     *
     * @param chatRequest ruoyi-ai 的请求对象
     * @param handler langchain4j 的流式响应处理器
     */
    default void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        throw new UnsupportedOperationException("此服务暂不支持工作流场景");
    }

    /**
     * 获取此服务支持的模型类别
     */
    String getCategory();
}
