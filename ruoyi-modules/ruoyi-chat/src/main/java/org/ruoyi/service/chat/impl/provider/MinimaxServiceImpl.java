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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MiniMax chat service.
 * <p>
 * Prefers MiniMax-M3.1-Flash, with MiniMax-M3 as the other current text model,
 * through OpenAI-compatible and Anthropic-compatible APIs in both regions.
 * Explicit legacy/custom database model IDs are passed through unchanged.
 * Flash always uses adaptive thinking; enableThinking controls returned reasoning
 * visibility, while reasoningEffort accepts low/medium/high/xhigh/max (default max).
 *
 * @author octopus
 * @date 2026/3/21
 */
@Service
@Slf4j
public class MinimaxServiceImpl implements AbstractChatService {

    private static final String MINIMAX_FLASH = "MiniMax-M3.1-Flash";
    private static final String MINIMAX_M3 = "MiniMax-M3";
    private static final Set<String> FLASH_EFFORTS = Set.of("low", "medium", "high", "xhigh", "max");
    private static final String ANTHROPIC_PATH_SUFFIX = "/anthropic";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        String baseUrl = normalizeBaseUrl(chatModelVo.getApiHost());
        boolean thinkingEnabled = chatRequest != null && Boolean.TRUE.equals(chatRequest.getEnableThinking());
        String thinkingType = thinkingType(chatModelVo.getModelName(), thinkingEnabled);
        String effort = flashEffort(chatModelVo.getModelName(), chatRequest);

        if (isAnthropicBaseUrl(baseUrl)) {
            return AnthropicStreamingChatModel.builder()
                .baseUrl(toAnthropicClientBaseUrl(baseUrl))
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .listeners(List.of(new MyChatModelListener()))
                .thinkingType(thinkingType)
                .customParameters(anthropicEffort(effort))
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
        return builder.customParameters(openAiParameters(thinkingType, effort)).build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return buildChatModel(chatModelVo, null);
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        String baseUrl = normalizeBaseUrl(chatModelVo.getApiHost());
        String effort = flashEffort(chatModelVo.getModelName(), chatRequest);
        // Preserve the synchronous M3/legacy API defaults.
        String thinkingType = effort == null ? null : "adaptive";
        if (isAnthropicBaseUrl(baseUrl)) {
            return AnthropicChatModel.builder()
                .baseUrl(toAnthropicClientBaseUrl(baseUrl))
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(DEFAULT_TIMEOUT)
                .thinkingType(thinkingType)
                .customParameters(anthropicEffort(effort))
                .build();
        }
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(DEFAULT_TIMEOUT)
            .customParameters(openAiParameters(thinkingType, effort))
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.MINIMAX.getCode();
    }

    private static String flashEffort(String modelName, ChatRequest request) {
        if (!MINIMAX_FLASH.equals(modelName)) {
            return null;
        }
        String effort = request == null ? null : request.getReasoningEffort();
        if (effort == null || effort.isBlank()) {
            return "max";
        }
        if (!FLASH_EFFORTS.contains(effort)) {
            throw new IllegalArgumentException("MiniMax-M3.1-Flash effort must be low, medium, high, xhigh or max");
        }
        return effort;
    }

    private static Map<String, Object> anthropicEffort(String effort) {
        return effort == null ? Map.of() : Map.of("output_config", Map.of("effort", effort));
    }

    private static Map<String, Object> openAiParameters(String thinkingType, String effort) {
        Map<String, Object> parameters = new HashMap<>();
        if (thinkingType != null) {
            parameters.put("thinking", Map.of("type", thinkingType));
        }
        if (effort != null) {
            parameters.put("reasoning_effort", effort);
        }
        return parameters;
    }

    static String thinkingType(String modelName, boolean thinkingEnabled) {
        if (MINIMAX_FLASH.equals(modelName)) {
            return "adaptive";
        }
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
