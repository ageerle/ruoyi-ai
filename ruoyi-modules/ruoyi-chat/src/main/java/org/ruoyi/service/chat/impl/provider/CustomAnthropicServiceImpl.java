package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.security.CustomApiCredentialPolicy;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** 自定义 Anthropic Messages 兼容接口。 */
@Service
public class CustomAnthropicServiceImpl implements AbstractChatService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);
    private static final int DEFAULT_MAX_TOKENS = 4096;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo config, ChatRequest request) {
        String baseUrl = validateConfiguration(config);
        return AnthropicStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(config.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(config.getModelName())
            .maxTokens(DEFAULT_MAX_TOKENS)
            .timeout(DEFAULT_TIMEOUT)
            .listeners(List.of(new MyChatModelListener()))
            .returnThinking(Boolean.TRUE.equals(request.getEnableThinking()))
            .build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo config) {
        String baseUrl = validateConfiguration(config);
        return AnthropicChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(config.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(config.getModelName())
            .maxTokens(DEFAULT_MAX_TOKENS)
            .timeout(DEFAULT_TIMEOUT)
            .listeners(List.of(new MyChatModelListener()))
            .build();
    }

    private String validateConfiguration(ChatModelVo config) {
        if (!getProviderName().equals(config.getProviderCode())) {
            throw new IllegalArgumentException("模型厂商与 Anthropic 自定义适配器不匹配");
        }
        return CustomApiCredentialPolicy.requireConfiguration(
            getProviderName(), config.getModelName(), config.getApiHost(), config.getApiKey());
    }

    @Override
    public String getProviderName() {
        return ChatModeType.CUSTOM_ANTHROPIC.getCode();
    }
}
