package org.ruoyi.service.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.time.Duration;

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
     * 创建同步聊天模型（供 Agent/SupervisorAgent 使用）
     * 默认实现使用 OpenAI 兼容协议，适用于 OpenAI、DeepSeek、PPIO 等兼容接口的 provider。
     * ZhiPu、QianWen、Ollama 等需覆盖此方法使用各自 SDK。
     *
     * @param chatModelVo 模型配置
     * @return 同步聊天模型实例
     */
    default ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofSeconds(120))
            .build();
    }

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
