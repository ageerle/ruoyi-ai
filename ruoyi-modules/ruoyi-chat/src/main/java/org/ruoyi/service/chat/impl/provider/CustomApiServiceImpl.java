package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.security.CustomApiCredentialPolicy;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.impl.provider.doubao.DoubaoStreamingChatModel;
import org.ruoyi.service.coding.harness.modelruntime.HarnessModelPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 自定义 API 服务调用
 *
 * 适用于 OpenAI Chat Completions 兼容接口。
 * 通过模型配置中的 apiHost / apiKey / modelName 即可复用，不需要再写死具体供应商。
 *
 * <p>字节 Doubao-Seed-Evolving 模型经自定义 OpenAI 兼容供应商（custom_api）接入时，
 * 返回专用的 {@link DoubaoStreamingChatModel}，以支持思考等级（reasoning_effort/thinking）、
 * 图片精度 xhigh 与 encrypted_content 思考加密原文回传。密钥只来自环境变量引用，
 * 不写入任何代码、SQL、文档或日志。</p>
 *
 * @author better
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomApiServiceImpl implements AbstractChatService {

    /** OpenAI 兼容自定义模型的默认单轮超时（维持历史行为 180 秒）。 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);

    /**
     * Doubao 单轮流式截止时间，可通过 Spring Duration 配置
     * {@code chat.custom-api.doubao.timeout=PT10M} 覆盖；默认 10 分钟，
     * 与 Harness 的 coding.harness.model-timeout.millis 默认 600000ms 协调，
     * 使长思考回合由 Harness 预算与单轮上限统一控制，而不是在接入层被提前切断。
     * 直接 new 本服务（无 Spring 容器）时该字段初始值同样是正确默认值。
     */
    @Value("${chat.custom-api.doubao.timeout:PT10M}")
    private Duration doubaoTimeout = Duration.ofMinutes(10);

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        String baseUrl = validateConfiguration(chatModelVo);
        if (HarnessModelPolicy.isDoubao(chatModelVo.getModelName())) {
            return buildDoubaoStreamingModel(chatModelVo, chatRequest, baseUrl);
        }
        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(chatModelVo.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(chatModelVo.getModelName())
            .timeout(DEFAULT_TIMEOUT)
            .listeners(List.of(new MyChatModelListener()))
            .returnThinking(Boolean.TRUE.equals(chatRequest.getEnableThinking()))
            .build();
    }

    private StreamingChatModel buildDoubaoStreamingModel(ChatModelVo chatModelVo,
                                                         ChatRequest chatRequest,
                                                         String baseUrl) {
        // Doubao 思考等级：默认 high；none 关闭思考（thinking.type=disabled 且不传 reasoning_effort）。
        // 其他模型不会进入此分支，Doubao 专属参数不会泄漏给非 Doubao 模型。
        String reasoningEffort = chatRequest == null || chatRequest.getReasoningEffort() == null
            || chatRequest.getReasoningEffort().isBlank()
            ? "high" : chatRequest.getReasoningEffort().strip();
        // Doubao 默认开启深度思考；仅当思考等级为 none 时关闭（thinking.type=disabled）。
        boolean thinkingEnabled = !"none".equalsIgnoreCase(reasoningEffort);
        return DoubaoStreamingChatModel.builder()
            .endpoint(baseUrl + "/chat/completions")
            .apiKey(chatModelVo.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(chatModelVo.getModelName())
            .timeout(doubaoTimeout)
            .reasoningEffort(reasoningEffort)
            .thinkingEnabled(thinkingEnabled)
            .build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        String baseUrl = validateConfiguration(chatModelVo);
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(chatModelVo.resolveApiKeyForConfiguredEndpoint(getProviderName()))
            .modelName(chatModelVo.getModelName())
            .timeout(DEFAULT_TIMEOUT)
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.CUSTOM_API.getCode();
    }

    private String validateConfiguration(ChatModelVo config) {
        if (!getProviderName().equals(config.getProviderCode())) {
            throw new IllegalArgumentException("模型厂商与 OpenAI 自定义适配器不匹配");
        }
        return CustomApiCredentialPolicy.requireConfiguration(
            getProviderName(), config.getModelName(), config.getApiHost(), config.getApiKey());
    }
}
