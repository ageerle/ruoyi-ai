package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.springframework.stereotype.Service;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.util.List;

/**
 * OllamaAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class OllamaServiceImpl extends AbstractStreamingChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build();
    }


    @Override
    public void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest, List<ChatMessage> messagesWithMemory,StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(messagesWithMemory, handler);
    }




    @Override
    public String getProviderName() {
        return ChatModeType.OLLAMA.getCode();
    }
}
