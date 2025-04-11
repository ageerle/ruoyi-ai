package org.ruoyi.chat.service.chat;

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
     * @param chatRequest 请求对象
     */
    SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter);
}
