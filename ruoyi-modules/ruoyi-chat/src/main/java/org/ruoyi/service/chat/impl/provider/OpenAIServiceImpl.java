package org.ruoyi.service.chat.impl.provider;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.ChatModelListenerProvider;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


/**
 * OPENAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements AbstractChatService {

    @Override
    public ChatModel buildChatModel(ChatModelVo config, ChatRequest request) {
        return OpenAiChatModel.builder()
            .baseUrl(config.getApiHost()).apiKey(config.getApiKey()).modelName(config.getModelName())
            .timeout(Duration.ofSeconds(120))
            .reasoningEffort(request.getReasoningEffort())
            .customParameters(SeedThinkingParameters.forRequest(config.getModelName(), request))
            .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .reasoningEffort(chatRequest.getReasoningEffort())
                .customParameters(SeedThinkingParameters.forRequest(chatModelVo.getModelName(), chatRequest))
                .timeout(Duration.ofMinutes(30))
                .listeners(List.of(new MyChatModelListener()))
                .returnThinking(chatRequest.getEnableThinking())
                .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

}
