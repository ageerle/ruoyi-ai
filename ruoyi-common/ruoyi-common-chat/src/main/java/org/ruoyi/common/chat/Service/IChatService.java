package org.ruoyi.common.chat.Service;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.domain.entity.chat.ChatContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公共大模型对话接口
 */
public interface IChatService {

    /**
     * 客户端发送对话消息到服务端
     */
    SseEmitter chat(@Valid ChatContext chatContext);

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
