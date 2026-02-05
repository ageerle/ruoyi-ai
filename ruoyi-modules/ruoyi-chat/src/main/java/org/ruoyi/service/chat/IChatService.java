package org.ruoyi.service.chat;

import org.ruoyi.domain.dto.request.ChatRequest;
import org.ruoyi.domain.vo.chat.ChatModelVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatService {

    /**
     * 客户端发送对话消息到服务端
     */
    SseEmitter chat(ChatModelVo chatModelVo, ChatRequest chatRequest,SseEmitter emitter,Long userId,String tokenValue);


    /**
     * 获取服务提供商名称
     */
    String getProviderName();

}
