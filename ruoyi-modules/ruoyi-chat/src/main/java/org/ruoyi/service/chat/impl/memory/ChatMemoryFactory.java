package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.chat.impl.memory.strategy.CompressionStrategyManager;
import org.springframework.stereotype.Component;

/**
 * ChatMemory 工厂
 * 根据配置创建不同策略的 ChatMemory 实例
 *
 * @author yang
 * @date 2026-04-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryFactory {

    private final ChatMemoryProperties properties;
    private final PersistentChatMemoryStore persistentStore;
    private final TokenCounter tokenCounter;
    private final CompressionStrategyManager strategyManager;

    /**
     * 创建 ChatMemory 实例
     *
     * @param memoryId 内存 ID（通常是会话 ID）
     * @param model    模型配置
     * @return ChatMemory 实例
     */
    public ChatMemory create(Object memoryId, ChatModelVo model) {
        // 自动创建摘要模型
        ChatModel summarizer = createSummarizerModel(model);
        return create(memoryId, model, summarizer);
    }

    /**
     * 创建 ChatMemory 实例（带摘要模型）
     *
     * @param memoryId   内存 ID
     * @param model      模型配置
     * @param summarizer 用于摘要的 LLM 模型（可选）
     * @return ChatMemory 实例
     */
    public ChatMemory create(Object memoryId, ChatModelVo model, ChatModel summarizer) {
        if (!properties.getEnabled()) {
            log.debug("长期记忆已禁用");
            return null;
        }

        String strategy = properties.getStrategy();
        log.info("创建 ChatMemory: strategy={}, memoryId={}", strategy, memoryId);

        // 检查模型是否已知，未知模型回退到消息数量策略
        if (Boolean.TRUE.equals(properties.getFallbackToMessageStrategy())
                && ("token".equalsIgnoreCase(strategy) || "hybrid".equalsIgnoreCase(strategy))) {
            if (model != null && model.getModelName() != null) {
                int tokenLimit = ModelTokenLimits.getLimitOrUnknown(model.getModelName());
                if (tokenLimit == ModelTokenLimits.UNKNOWN_LIMIT) {
                    log.info("模型 [{}] 不在已知列表中，回退到固定消息数量策略 (maxMessages={})",
                            model.getModelName(), properties.getFallbackMaxMessages());
                    return createFallbackMessageMemory(memoryId);
                }
            }
        }

        return switch (strategy.toLowerCase()) {
            case "message" -> createMessageBasedMemory(memoryId);
            case "token" -> createTokenBasedMemory(memoryId, model, summarizer);
            case "hybrid" -> createHybridMemory(memoryId, model, summarizer);
            default -> {
                log.warn("未知的内存策略: {}, 使用默认 token 策略", strategy);
                yield createTokenBasedMemory(memoryId, model, summarizer);
            }
        };
    }

    /**
     * 创建基于消息数量的内存（原有策略）
     */
    private ChatMemory createMessageBasedMemory(Object memoryId) {
        int maxMessages = properties.getMaxMessages();
        log.debug("创建消息窗口内存: maxMessages={}", maxMessages);

        return MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(maxMessages)
            .chatMemoryStore(persistentStore)
            .build();
    }

    /**
     * 创建回退的消息数量内存（用于未知模型）
     */
    private ChatMemory createFallbackMessageMemory(Object memoryId) {
        int maxMessages = properties.getFallbackMaxMessages() != null
                ? properties.getFallbackMaxMessages()
                : 20;
        log.debug("创建回退消息窗口内存: maxMessages={}", maxMessages);

        return MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(maxMessages)
            .chatMemoryStore(persistentStore)
            .build();
    }

    /**
     * 创建基于 Token 的内存
     */
    private ChatMemory createTokenBasedMemory(Object memoryId, ChatModelVo model, ChatModel summarizer) {
        int maxTokens = resolveMaxTokens(model);
        int reservedForReply = properties.getReservedForReply();
        double summarizeTokenRatio = properties.getSummarizeTokenRatio() != null
                ? properties.getSummarizeTokenRatio() : 0.7;

        log.info("[Token内存] 创建Token窗口内存: maxTokens={}, reservedForReply={}, 模型={}, 摘要触发比例={}",
                maxTokens, reservedForReply, model != null ? model.getModelName() : "未知", summarizeTokenRatio);

        // 判断是否使用策略框架
        boolean useStrategyFramework = properties.getUseStrategyFramework() != null
                ? properties.getUseStrategyFramework() : true;

        return TokenBasedChatMemory.builder()
            .memoryId(memoryId)
            .maxTokens(maxTokens)
            .tokenCounter(tokenCounter)
            .store(persistentStore)
            .summarizeTokenRatio(summarizeTokenRatio)
            .summarizeThreshold(properties.getSummarizeThreshold())
            .summarizer(summarizer)
            .preserveSystemMessages(properties.getPreserveSystemMessages())
            .reservedForReply(reservedForReply)
            .strategyManager(useStrategyFramework ? strategyManager : null)
            .build();
    }

    /**
     * 创建混合策略内存（Token + 摘要）
     */
    private ChatMemory createHybridMemory(Object memoryId, ChatModelVo model, ChatModel summarizer) {
        int maxTokens = resolveMaxTokens(model);
        int reservedForReply = properties.getReservedForReply();
        double summarizeTokenRatio = properties.getSummarizeTokenRatio() != null
                ? properties.getSummarizeTokenRatio() : 0.7;
        int summarizeThreshold = properties.getSummarizeThreshold();

        log.info("[Hybrid内存] 创建混合策略内存: maxTokens={}, summarizeTokenRatio={}, summarizeThreshold={}",
            maxTokens, summarizeTokenRatio, summarizeThreshold);

        // 判断是否使用策略框架
        boolean useStrategyFramework = properties.getUseStrategyFramework() != null
                ? properties.getUseStrategyFramework() : true;

        return TokenBasedChatMemory.builder()
            .memoryId(memoryId)
            .maxTokens(maxTokens)
            .tokenCounter(tokenCounter)
            .store(persistentStore)
            .summarizeTokenRatio(summarizeTokenRatio)
            .summarizeThreshold(summarizeThreshold)
            .summarizer(summarizer)
            .preserveSystemMessages(properties.getPreserveSystemMessages())
            .reservedForReply(reservedForReply)
            .strategyManager(useStrategyFramework ? strategyManager : null)
            .build();
    }

    /**
     * 解析最大 Token 数
     * 优先使用配置值，否则根据模型自动获取
     */
    private int resolveMaxTokens(ChatModelVo model) {
        // 优先使用配置值
        if (properties.getMaxTokens() != null && properties.getMaxTokens() > 0) {
            return properties.getMaxTokens();
        }

        // 根据模型自动获取
        if (model != null && model.getModelName() != null) {
            int modelLimit = ModelTokenLimits.getLimit(model.getModelName());
            int inputLimit = ModelTokenLimits.getInputLimit(model.getModelName(), properties.getReservedForReply());
            log.debug("模型 {} 的 Token 限制: {}, 输入限制: {}", model.getModelName(), modelLimit, inputLimit);
            return inputLimit;
        }

        // 默认值
        return 4096;
    }

    /**
     * 获取模型的完整 Token 限制（用于显示）
     */
    public int getModelTokenLimit(String modelName) {
        return ModelTokenLimits.getLimit(modelName);
    }

    /**
     * 创建用于摘要的 LLM 模型
     * 根据配置选择摘要模型策略：
     * - current: 使用当前对话模型（默认，质量高）
     * - smart: 智能映射到轻量级模型（成本低）
     * - custom: 使用自定义模型
     *
     * @param model 原始模型配置
     * @return 摘要模型实例，如果无法创建则返回 null
     */
    private ChatModel createSummarizerModel(ChatModelVo model) {
        log.debug("[摘要模型] 开始创建，model={}", model != null ? model.getModelName() : "null");

        if (model == null || model.getApiKey() == null || model.getApiHost() == null) {
            log.warn("[摘要模型] 创建失败：缺少必要参数");
            return null;
        }

        try {
            String originalModel = model.getModelName();
            String summarizerModelName;
            String strategy = properties.getSummarizerStrategy();

            // 根据配置选择摘要模型
            if ("smart".equals(strategy)) {
                summarizerModelName = getSmartSummarizerModel(originalModel);
                log.info("[摘要模型] 智能映射策略: {} → {}", originalModel, summarizerModelName);
            } else if ("custom".equals(strategy)) {
                summarizerModelName = properties.getSummarizerCustomModel();
                if (summarizerModelName == null || summarizerModelName.isEmpty()) {
                    log.warn("[摘要模型] 自定义模型未配置，回退到当前模型");
                    summarizerModelName = originalModel;
                }
                log.info("[摘要模型] 自定义策略: 使用 {}", summarizerModelName);
            } else {
                summarizerModelName = originalModel;
                log.info("[摘要模型] 当前模型策略: 使用 {}", summarizerModelName);
            }

            String baseUrl = fixApiBaseUrl(model.getApiHost(), summarizerModelName);

            return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(model.getApiKey())
                .modelName(summarizerModelName)
                .timeout(java.time.Duration.ofSeconds(60))
                .build();
        } catch (Exception e) {
            log.warn("[摘要模型] 创建异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 智能映射摘要模型
     * 根据原模型自动选择性价比高的轻量级模型
     *
     * @param originalModel 原始模型名称
     * @return 摘要模型名称
     */
    private String getSmartSummarizerModel(String originalModel) {
        if (originalModel == null || originalModel.isEmpty()) {
            return originalModel;
        }

        String model = originalModel.toLowerCase();

        // GLM 系列 → flash（智谱最便宜，有免费额度）
        if (model.contains("glm-5")) return "glm-5-flash";
        if (model.contains("glm-4.5")) return "glm-4.5-air";
        if (model.contains("glm-4")) return "glm-4-flash";

        // GPT 系列 → mini
        if (model.contains("gpt-4")) return "gpt-4o-mini";
        if (model.contains("gpt-3.5")) return "gpt-3.5-turbo";

        // Claude 系列 → haiku
        if (model.contains("claude")) return "claude-3-5-haiku";

        // Gemini → flash
        if (model.contains("gemini")) return "gemini-2.0-flash";

        // DeepSeek 本身便宜，保持原模型
        if (model.contains("deepseek")) return originalModel;

        // Qwen → turbo
        if (model.contains("qwen")) return "qwen-turbo";

        // Doubao → lite
        if (model.contains("doubao")) return "doubao-1.5-lite";

        // 其他模型保持原样
        return originalModel;
    }

    /**
     * 修正 API 地址（用于 OpenAI 兼容格式）
     * 某些供应商的 API 地址需要特殊处理：
     * - 智谱 GLM: 需要添加 /api/paas/v4/ 路径
     * - 千问 Qwen: 需要添加 /compatible-mode/v1 路径
     *
     * @param baseUrl   原始 API 地址
     * @param modelName 模型名称（用于判断供应商）
     * @return 修正后的 API 地址
     */
    public static String fixApiBaseUrl(String baseUrl, String modelName) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }

        // 智谱 GLM 系列：OpenAI 兼容格式需要完整路径
        if (modelName != null && modelName.toLowerCase().contains("glm")) {
            if (!baseUrl.contains("/api/paas") && !baseUrl.contains("/v4")) {
                baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
                baseUrl = baseUrl + "api/paas/v4/";
                log.debug("[API地址修正] 智谱 GLM: 添加路径 /api/paas/v4/");
            }
        }

        // 千问 Qwen 系列：OpenAI 兼容格式需要完整路径
        if (modelName != null && modelName.toLowerCase().contains("qwen")) {
            if (!baseUrl.contains("/compatible-mode")) {
                baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
                baseUrl = baseUrl + "compatible-mode/v1/";
                log.debug("[API地址修正] 千问 Qwen: 添加路径 /compatible-mode/v1/");
            }
        }

        return baseUrl;
    }
}
