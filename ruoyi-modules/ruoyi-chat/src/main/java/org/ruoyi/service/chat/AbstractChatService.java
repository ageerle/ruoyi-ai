package org.ruoyi.service.chat;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * 聊天消息Service接口
 *
 * @author ageerle
 * @date 2025-12-14
 */
public interface AbstractChatService {

    /**
     * 创建流式聊天模型
     *
     * @param chatModelVo 模型配置
     * @param chatRequest 聊天请求
     * @return 流式聊天模型实例
     */
    StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest);

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
