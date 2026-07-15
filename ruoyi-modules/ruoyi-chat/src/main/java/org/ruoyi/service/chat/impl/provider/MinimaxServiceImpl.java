package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MiniMax chat service.
 * <p>
 * Supports MiniMax-M3 and MiniMax-M2.7 through the OpenAI-compatible and
 * Anthropic-compatible APIs in both global and China regions.
 *
 * @author octopus
 * @date 2026/3/21
 */
@Service
@Slf4j
public class MinimaxServiceImpl implements AbstractChatService {

    private static final String MINIMAX_M3 = "MiniMax-M3";
    private static final String ANTHROPIC_PATH_SUFFIX = "/anthropic";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        String baseUrl = normalizeBaseUrl(chatModelVo.getApiHost());
        boolean thinkingEnabled = Boolean.TRUE.equals(chatRequest.getEnableThinking());
        String thinkingType = thinkingType(chatModelVo.getModelName(), thinkingEnabled);

        if (isAnthropicBaseUrl(baseUrl)) {
            return AnthropicStreamingChatModel.builder()
                .baseUrl(toAnthropicClientBaseUrl(baseUrl))
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .listeners(List.of(new MyChatModelListener()))
                .thinkingType(thinkingType)
                .returnThinking(thinkingEnabled)
                .build();
        }

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(DEFAULT_TIMEOUT)
            .listeners(List.of(new MyChatModelListener()))
            .returnThinking(thinkingEnabled);
        if (thinkingType != null) {
            builder.customParameters(Map.of("thinking", Map.of("type", thinkingType)));
        }
        return builder.build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        String baseUrl = normalizeBaseUrl(chatModelVo.getApiHost());
        if (isAnthropicBaseUrl(baseUrl)) {
            return AnthropicChatModel.builder()
                .baseUrl(toAnthropicClientBaseUrl(baseUrl))
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .build();
        }
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(DEFAULT_TIMEOUT)
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.MINIMAX.getCode();
    }

    static String thinkingType(String modelName, boolean thinkingEnabled) {
        if (!MINIMAX_M3.equals(modelName)) {
            return null;
        }
        return thinkingEnabled ? "adaptive" : "disabled";
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("MiniMax API Host must not be blank");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("MiniMax API Host must be a valid URL", exception);
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("MiniMax API Host must be an absolute URL");
        }
        return uri.toString();
    }

    private static boolean isAnthropicBaseUrl(String baseUrl) {
        return URI.create(baseUrl).getPath().endsWith(ANTHROPIC_PATH_SUFFIX);
    }

    private static String toAnthropicClientBaseUrl(String baseUrl) {
        // LangChain4j appends /messages, while MiniMax exposes /anthropic/v1/messages.
        return baseUrl + "/v1";
    }

}
