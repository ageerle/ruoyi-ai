package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: xiaoen
 * @Description: deepseek 服务调用
 * @Date: Created in 19:12 2026/3/17
 */
@Service
@Slf4j
public class DeepseekServiceImpl extends AbstractStreamingChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .returnThinking(chatRequest.getEnableThinking())
            .build();
    }

    @Override
    public void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest, List<ChatMessage> messagesWithMemory, StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(messagesWithMemory, handler);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.DEEP_SEEK.getCode();
    }

}
