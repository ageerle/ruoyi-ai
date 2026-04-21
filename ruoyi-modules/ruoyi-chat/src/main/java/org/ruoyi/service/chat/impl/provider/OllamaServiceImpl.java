package org.ruoyi.service.chat.impl.provider;


import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.ChatModelListenerProvider;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OllamaAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OllamaServiceImpl implements AbstractChatService {

    private final ChatModelListenerProvider listenerProvider;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .listeners(List.of(new MyChatModelListener()))
                .build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return OllamaChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .modelName(chatModelVo.getModelName())
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OLLAMA.getCode();
    }
}
