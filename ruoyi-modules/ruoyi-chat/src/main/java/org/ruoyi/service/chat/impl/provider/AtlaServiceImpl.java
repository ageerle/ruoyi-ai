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
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.impl.provider.atlas.AtlasDeadlineHttpClientBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Duration;

/**
 * Atlas Cloud服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AtlaServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(new AtlasDeadlineHttpClientBuilder())
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(Duration.ofMinutes(6))
                .maxTokens(16384)
                .reasoningEffort(chatRequest.getReasoningEffort())
                .customParameters(SeedThinkingParameters.forRequest(chatModelVo.getModelName(), chatRequest))
                .listeners(List.of(new MyChatModelListener()))
                .returnThinking(chatRequest.getEnableThinking())
                .build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return buildChatModel(chatModelVo, new ChatRequest());
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo, ChatRequest request) {
        return OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofMinutes(20))
            .maxRetries(0)
            .maxTokens(16384)
            .reasoningEffort(request.getReasoningEffort())
            .customParameters(SeedThinkingParameters.forRequest(chatModelVo.getModelName(), request))
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }

}
