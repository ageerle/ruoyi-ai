package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.security.ChatModelCredentialPolicy;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** PPIO 的 OpenAI Chat Completions 兼容接入。 */
@Service
public class PpioChatServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo config, ChatRequest request) {
        String baseUrl = validateConfiguration(config);
        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(config.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(config.getModelName())
            .listeners(List.of(new MyChatModelListener()))
            .returnThinking(Boolean.TRUE.equals(request.getEnableThinking()))
            .timeout(Duration.ofMinutes(3))
            .build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo config) {
        String baseUrl = validateConfiguration(config);
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(config.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(config.getModelName())
            .listeners(List.of(new MyChatModelListener()))
            .timeout(Duration.ofMinutes(3))
            .build();
    }

    private String validateConfiguration(ChatModelVo config) {
        return ChatModelCredentialPolicy.requirePpioConfiguration(
            config.getProviderCode(), config.getModelName(), config.getApiHost(), config.getApiKey());
    }

    @Override
    public String getProviderName() {
        return ChatModeType.PPIO.getCode();
    }
}
