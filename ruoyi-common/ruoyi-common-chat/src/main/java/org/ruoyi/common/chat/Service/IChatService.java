package org.ruoyi.common.chat.Service;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公共大模型对话接口
 */
public interface IChatService {

    /**
     * 客户端发送对话消息到服务端
     */
    SseEmitter chat(ChatModelVo chatModelVo, ChatRequest chatRequest, SseEmitter emitter, Long userId, String tokenValue);

    /**
     * 工作流专用对话
     */
    SseEmitter chat(ChatModelVo chatModelVo, ChatRequest chatRequest,SseEmitter emitter,Long userId,String tokenValue, StreamingChatResponseHandler handler);

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
