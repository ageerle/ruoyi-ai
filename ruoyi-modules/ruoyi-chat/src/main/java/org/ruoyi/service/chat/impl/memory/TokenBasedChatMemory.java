package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.chat.impl.memory.strategy.CompressionContext;
import org.ruoyi.service.chat.impl.memory.strategy.CompressionResult;
import org.ruoyi.service.chat.impl.memory.strategy.CompressionStrategyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于 Token 的聊天内存管理
 * 支持 Token 窗口限制和可选的摘要压缩
 *
 * @author yang
 * @date 2026-04-27
 */
@Slf4j
public class TokenBasedChatMemory implements ChatMemory {

    /**
     * 内存 ID（通常是会话 ID）
     */
    private final Object memoryId;

    /**
     * 最大 Token 数
     */
    private final int maxTokens;

    /**
     * Token 计数器
     */
    private final TokenCounter tokenCounter;

    /**
     * 持久化存储
     */
    private final ChatMemoryStore store;

    /**
     * 触发摘要的 Token 使用比例（如 0.7 表示 70%）
     */
    private final double summarizeTokenRatio;

    /**
     * 触发摘要的消息数量阈值（避免消息太少时摘要无意义）
     */
    private final int summarizeThreshold;

    /**
     * 用于摘要的 LLM 模型（可选）
     */
    private final ChatModel summarizer;

    /**
     * 是否保留系统消息（不被截断）
     */
    private final boolean preserveSystemMessages;

    /**
     * 预留给回复的 Token 数
     */
    private final int reservedForReply;

    /**
     * 压缩策略管理器（可选）
     * 如果设置，优先使用策略管理器进行压缩
     */
    private final CompressionStrategyManager strategyManager;

    /**
     * 构造函数
     */
    private TokenBasedChatMemory(Builder builder) {
        this.memoryId = builder.memoryId;
        this.maxTokens = builder.maxTokens;
        this.tokenCounter = builder.tokenCounter != null ? builder.tokenCounter : new TokenCounter();
        this.store = builder.store;
        this.summarizeTokenRatio = builder.summarizeTokenRatio;
        this.summarizeThreshold = builder.summarizeThreshold;
        this.summarizer = builder.summarizer;
        this.preserveSystemMessages = builder.preserveSystemMessages;
        this.reservedForReply = builder.reservedForReply;
        this.strategyManager = builder.strategyManager;
    }

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = new ArrayList<>(messages());
        messages.add(message);
        store.updateMessages(memoryId, messages);
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> messages = store != null ? store.getMessages(memoryId) : new ArrayList<>();

        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        int totalTokens = tokenCounter.countMessages(messages);
        int effectiveMaxTokens = Math.max(1, maxTokens - reservedForReply);

        // 输出当前状态日志
        log.info("[Token内存管理] 会话={}, 消息数={}, 当前Token={}, Token上限={}, 预留回复空间={}",
                memoryId, messages.size(), totalTokens, maxTokens, reservedForReply);

        // 使用策略管理器进行压缩
        if (strategyManager != null) {
            CompressionContext context = buildCompressionContext(messages, totalTokens);
            CompressionResult result = strategyManager.execute(context);
            if (result.isSuccess() && result.getStrategyName() != null && !result.getStrategyName().equals("none")) {
                log.info("[策略框架] 压缩成功: 策略={}, Token: {} → {}, 消息数: {} → {}",
                        result.getStrategyName(), result.getOriginalTokens(), result.getCompressedTokens(),
                        result.getOriginalMessageCount(), result.getCompressedMessageCount());

                // 最终保障：检查是否仍超限
                List<ChatMessage> resultMessages = result.getMessages();
                int resultTokens = result.getCompressedTokens();
                if (resultTokens > effectiveMaxTokens) {
                    log.warn("[Token内存管理] 策略执行后仍超限，执行紧急截断");
                    return emergencyTruncate(resultMessages, effectiveMaxTokens);
                }
                return resultMessages;
            } else if (result.getErrorMessage() != null) {
                log.warn("[策略框架] 压缩失败: {}", result.getErrorMessage());
                // 策略失败，执行紧急截断
                if (totalTokens > effectiveMaxTokens) {
                    log.warn("[Token内存管理] 策略失败且超限，执行紧急截断");
                    return emergencyTruncate(messages, effectiveMaxTokens);
                }
            }
        }

        // 无策略框架或策略未触发，检查是否需要截断
        if (totalTokens > effectiveMaxTokens) {
            log.warn("[Token内存管理] 无策略框架且超限，执行紧急截断");
            return emergencyTruncate(messages, effectiveMaxTokens);
        }

        return messages;
    }

    /**
     * 紧急截断
     * 当策略框架不可用或失败时，确保返回的消息不超限
     */
    private List<ChatMessage> emergencyTruncate(List<ChatMessage> messages, int maxTokens) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        // 分离系统消息和普通消息
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> regularMessages = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if (preserveSystemMessages && msg instanceof SystemMessage) {
                systemMessages.add(msg);
            } else {
                regularMessages.add(msg);
            }
        }

        // 计算系统消息占用的 Token
        int systemTokens = tokenCounter.countMessages(systemMessages);
        int availableTokens = maxTokens - systemTokens;

        if (availableTokens <= 0) {
            log.warn("[紧急截断] 系统消息已占用全部 Token 空间，仅保留系统消息");
            return systemMessages;
        }

        // 从最新的消息开始保留
        List<ChatMessage> keptMessages = new ArrayList<>();
        int currentTokens = 0;

        for (int i = regularMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = regularMessages.get(i);
            int msgTokens = tokenCounter.countMessage(msg);

            if (currentTokens + msgTokens <= availableTokens) {
                keptMessages.add(0, msg);
                currentTokens += msgTokens;
            } else {
                break;
            }
        }

        // 合并结果
        List<ChatMessage> result = new ArrayList<>(systemMessages);
        result.addAll(keptMessages);

        log.info("[紧急截断] 完成: 原消息数={} → 截断后={}, 系统消息={}",
            messages.size(), result.size(), systemMessages.size());

        return result;
    }

    /**
     * 构建压缩上下文
     */
    private CompressionContext buildCompressionContext(List<ChatMessage> messages, int totalTokens) {
        return CompressionContext.builder()
                .memoryId(memoryId)
                .messages(messages)
                .currentTokens(totalTokens)
                .maxTokens(maxTokens)
                .reservedForReply(reservedForReply)
                .summarizer(summarizer)
                .tokenCounter(tokenCounter)
                .preserveSystemMessages(preserveSystemMessages)
                .summarizeTokenRatio(summarizeTokenRatio)
                .summarizeThreshold(summarizeThreshold)
                .build();
    }

    @Override
    public void clear() {
        if (store != null) {
            store.deleteMessages(memoryId);
        }
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 从模型配置创建
     */
    public static TokenBasedChatMemory fromModel(Object memoryId, ChatModelVo model,
                                                   ChatMemoryStore store, ChatModel summarizer) {
        int maxTokens = ModelTokenLimits.getLimit(model.getModelName());
        int inputLimit = ModelTokenLimits.getInputLimit(model.getModelName(), 2000);

        return builder()
            .memoryId(memoryId)
            .maxTokens(inputLimit)
            .store(store)
            .summarizer(summarizer)
            .summarizeThreshold(30)
            .preserveSystemMessages(true)
            .reservedForReply(2000)
            .build();
    }

    /**
     * 从模型配置创建（带策略管理器）
     */
    public static TokenBasedChatMemory fromModel(Object memoryId, ChatModelVo model,
                                                   ChatMemoryStore store, ChatModel summarizer,
                                                   CompressionStrategyManager strategyManager) {
        int maxTokens = ModelTokenLimits.getLimit(model.getModelName());
        int inputLimit = ModelTokenLimits.getInputLimit(model.getModelName(), 2000);

        return builder()
            .memoryId(memoryId)
            .maxTokens(inputLimit)
            .store(store)
            .summarizer(summarizer)
            .summarizeThreshold(30)
            .preserveSystemMessages(true)
            .reservedForReply(2000)
            .strategyManager(strategyManager)
            .build();
    }

    /**
     * 构建器
     */
    public static class Builder {
        private Object memoryId;
        private int maxTokens = 4096;
        private TokenCounter tokenCounter;
        private ChatMemoryStore store;
        private double summarizeTokenRatio = 0.7;
        private int summarizeThreshold = 10;
        private ChatModel summarizer;
        private boolean preserveSystemMessages = true;
        private int reservedForReply = 2000;
        private CompressionStrategyManager strategyManager;

        public Builder memoryId(Object memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder tokenCounter(TokenCounter tokenCounter) {
            this.tokenCounter = tokenCounter;
            return this;
        }

        public Builder store(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        public Builder summarizeTokenRatio(double summarizeTokenRatio) {
            this.summarizeTokenRatio = summarizeTokenRatio;
            return this;
        }

        public Builder summarizeThreshold(int summarizeThreshold) {
            this.summarizeThreshold = summarizeThreshold;
            return this;
        }

        public Builder summarizer(ChatModel summarizer) {
            this.summarizer = summarizer;
            return this;
        }

        public Builder preserveSystemMessages(boolean preserveSystemMessages) {
            this.preserveSystemMessages = preserveSystemMessages;
            return this;
        }

        public Builder reservedForReply(int reservedForReply) {
            this.reservedForReply = reservedForReply;
            return this;
        }

        public Builder strategyManager(CompressionStrategyManager strategyManager) {
            this.strategyManager = strategyManager;
            return this;
        }

        public TokenBasedChatMemory build() {
            Objects.requireNonNull(memoryId, "memoryId 不能为空");
            return new TokenBasedChatMemory(this);
        }
    }
}
