package org.ruoyi.service.chat.impl.provider;


import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.security.ChatModelCredentialPolicy;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.ChatModelListenerProvider;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;


/**
 * Deepseek服务调用
 *
 * @author xiaoen
 * @date 2026/3/17
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeepseekServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        validateConfiguration(chatModelVo);
        boolean thinkingEnabled = Boolean.TRUE.equals(chatRequest.getEnableThinking());
        boolean replayThinking = thinkingEnabled
            || Boolean.TRUE.equals(chatRequest.getReplayThinking());
        var builder = OpenAiStreamingChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(chatModelVo.getModelName())
            .listeners(List.of(new MyChatModelListener()))
            .customParameters(thinkingParameters(thinkingEnabled))
            .returnThinking(thinkingEnabled)
            // DeepSeek requires reasoning_content on every later tool-bearing request.
            .sendThinking(replayThinking)
            .parallelToolCalls(true)
            .timeout(Duration.ofMinutes(3));
        if (thinkingEnabled) {
            builder.reasoningEffort("high");
        }
        return builder.build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        validateConfiguration(chatModelVo);
        return AbstractChatService.super.buildChatModel(chatModelVo);
    }

    private void validateConfiguration(ChatModelVo chatModelVo) {
        ChatModelCredentialPolicy.requireDeepSeekConfiguration(
            chatModelVo.getProviderCode(), chatModelVo.getModelName(), chatModelVo.getApiHost(),
            chatModelVo.getApiKey());
    }

    static Map<String, Object> thinkingParameters(boolean enabled) {
        return Map.of("thinking", Map.of("type", enabled ? "enabled" : "disabled"));
    }


    @Override
    public String getProviderName() {
        return ChatModeType.DEEP_SEEK.getCode();
    }

}
