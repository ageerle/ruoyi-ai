package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.impl.memory.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 压缩策略管理器
 * 管理多个压缩策略，按优先级执行
 *
 * @author yang
 * @date 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompressionStrategyManager {

    private final List<MemoryCompressionStrategy> strategies;

    /**
     * 执行压缩
     * 按优先级依次尝试各策略，直到成功压缩到目标范围内
     * 如果所有策略执行后仍超限，强制执行截断作为最终兜底
     *
     * @param context 压缩上下文
     * @return 压缩结果
     */
    public CompressionResult execute(CompressionContext context) {
        List<MemoryCompressionStrategy> sortedStrategies = getSortedStrategies();

        List<ChatMessage> currentMessages = context.getMessages();
        int currentTokens = context.getCurrentTokens();
        int originalCount = currentMessages.size();
        int originalTokens = currentTokens;

        List<String> executedStrategies = new ArrayList<>();

        for (MemoryCompressionStrategy strategy : sortedStrategies) {
            // 更新上下文中的消息和 Token
            CompressionContext updatedContext = CompressionContext.builder()
                .memoryId(context.getMemoryId())
                .messages(currentMessages)
                .currentTokens(currentTokens)
                .maxTokens(context.getMaxTokens())
                .reservedForReply(context.getReservedForReply())
                .summarizer(context.getSummarizer())
                .tokenCounter(context.getTokenCounter())
                .preserveSystemMessages(context.isPreserveSystemMessages())
                .summarizeTokenRatio(context.getSummarizeTokenRatio())
                .summarizeThreshold(context.getSummarizeThreshold())
                .build();

            // 检查是否需要压缩
            if (!strategy.needsCompression(updatedContext)) {
                log.debug("[策略管理器] 策略 {} 不需要执行", strategy.getName());
                continue;
            }

            log.info("[策略管理器] 执行策略: {}", strategy.getName());

            // 执行压缩
            CompressionResult result = strategy.compress(updatedContext);

            if (result.isSuccess()) {
                executedStrategies.add(strategy.getName());
                currentMessages = result.getMessages();
                currentTokens = result.getCompressedTokens();

                log.info("[策略管理器] 策略 {} 执行成功, Token: {} → {}",
                    strategy.getName(), result.getOriginalTokens(), result.getCompressedTokens());

                // 检查是否已达到目标
                if (currentTokens <= updatedContext.getEffectiveMaxTokens()) {
                    log.info("[策略管理器] 压缩完成，已达到目标范围");
                    break;
                }

                // 如果策略可组合，继续尝试下一个策略
                if (!strategy.isComposable()) {
                    log.info("[策略管理器] 策略 {} 不可组合，停止后续策略", strategy.getName());
                    break;
                }
            } else {
                log.warn("[策略管理器] 策略 {} 执行失败: {}", strategy.getName(), result.getErrorMessage());
            }
        }

        // 最终保障：检查是否仍超限，如果是则强制截断
        int effectiveMaxTokens = context.getEffectiveMaxTokens();
        if (currentTokens > effectiveMaxTokens) {
            log.warn("[策略管理器] 策略执行后仍超限 ({} > {})，执行强制截断兜底",
                currentTokens, effectiveMaxTokens);
            currentMessages = forceTruncate(currentMessages, effectiveMaxTokens, context);
            currentTokens = context.getTokenCounter().countMessages(currentMessages);
            executedStrategies.add("force-truncation");
        }

        // 构建最终结果
        return CompressionResult.builder()
            .success(true)
            .messages(currentMessages)
            .originalTokens(originalTokens)
            .compressedTokens(currentTokens)
            .originalMessageCount(originalCount)
            .compressedMessageCount(currentMessages.size())
            .strategyName(executedStrategies.isEmpty() ? "none" : executedStrategies.toString())
            .compressionSummary("执行策略: " + executedStrategies)
            .build();
    }

    /**
     * 强制截断作为最终兜底
     * 确保返回的消息永远不会超限
     */
    private List<ChatMessage> forceTruncate(List<ChatMessage> messages, int maxTokens, CompressionContext context) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        boolean preserveSystem = context.isPreserveSystemMessages();
        TokenCounter tokenCounter = context.getTokenCounter();

        // 分离系统消息和普通消息
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> regularMessages = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if (preserveSystem && msg instanceof SystemMessage) {
                systemMessages.add(msg);
            } else {
                regularMessages.add(msg);
            }
        }

        // 计算系统消息占用的 Token
        int systemTokens = tokenCounter.countMessages(systemMessages);
        int availableTokens = maxTokens - systemTokens;

        if (availableTokens <= 0) {
            log.warn("[强制截断] 系统消息已占用全部 Token 空间，仅保留系统消息");
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

        log.info("[强制截断] 完成: 原消息数={} → 截断后={}, 系统消息={}",
            messages.size(), result.size(), systemMessages.size());

        return result;
    }

    /**
     * 获取指定策略
     *
     * @param name 策略名称
     * @return 策略实例，不存在则返回 null
     */
    public MemoryCompressionStrategy getStrategy(String name) {
        return strategies.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有可用策略名称
     */
    public List<String> getAvailableStrategies() {
        return strategies.stream()
            .map(MemoryCompressionStrategy::getName)
            .toList();
    }

    /**
     * 按优先级排序的策略列表
     */
    private List<MemoryCompressionStrategy> getSortedStrategies() {
        return strategies.stream()
            .sorted(Comparator.comparingInt(MemoryCompressionStrategy::getPriority))
            .toList();
    }
}
