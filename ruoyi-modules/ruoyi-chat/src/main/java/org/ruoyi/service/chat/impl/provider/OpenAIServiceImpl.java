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
 * OPENAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class OpenAIServiceImpl extends AbstractStreamingChatService {

    @Override
    protected StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .returnThinking(chatRequest.getEnableThinking())
                .build();
    }

    @Override
    protected void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest, List<ChatMessage> messagesWithMemory, StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(messagesWithMemory, handler);
    }


    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

}
