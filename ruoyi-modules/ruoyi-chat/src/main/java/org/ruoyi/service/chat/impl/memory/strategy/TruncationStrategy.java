package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.impl.memory.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 截断策略
 * 当超过 Token 限制时，从旧消息开始截断，保留最近的消息
 *
 * @author yang
 * @date 2026-04-29
 */
@Slf4j
@Component
public class TruncationStrategy implements MemoryCompressionStrategy {

    @Override
    public String getName() {
        return "truncation";
    }

    @Override
    public int getPriority() {
        return 100; // 默认优先级
    }

    @Override
    public boolean needsCompression(CompressionContext context) {
        // Token 超过限制时需要压缩
        return context.isOverLimit();
    }

    @Override
    public CompressionResult compress(CompressionContext context) {
        List<ChatMessage> messages = context.getMessages();
        int maxTokens = context.getEffectiveMaxTokens();
        boolean preserveSystem = context.isPreserveSystemMessages();
        TokenCounter tokenCounter = context.getTokenCounter();

        if (messages == null || messages.isEmpty()) {
            return CompressionResult.success(getName(), messages, 0, 0, 0, 0);
        }

        int originalTokens = context.getCurrentTokens();
        int originalCount = messages.size();

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
            log.warn("[截断策略] 系统消息已占用全部 Token 空间，仅保留系统消息");
            return CompressionResult.success(
                getName(),
                systemMessages,
                originalTokens,
                systemTokens,
                originalCount,
                systemMessages.size()
            );
        }

        // 从最新的消息开始保留（从后向前遍历）
        List<ChatMessage> keptMessages = new ArrayList<>();
        int currentTokens = 0;

        for (int i = regularMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = regularMessages.get(i);
            int msgTokens = tokenCounter.countMessage(msg);

            if (currentTokens + msgTokens <= availableTokens) {
                keptMessages.add(0, msg); // 添加到头部保持顺序
                currentTokens += msgTokens;
            } else {
                break; // 达到限制，停止
            }
        }

        // 合并结果：系统消息 + 保留的普通消息
        List<ChatMessage> result = new ArrayList<>(systemMessages);
        result.addAll(keptMessages);

        int compressedTokens = tokenCounter.countMessages(result);

        log.info("[截断策略] 完成: 原消息数={} → 截断后={}, Token: {} → {}",
            originalCount, result.size(), originalTokens, compressedTokens);

        return CompressionResult.success(
            getName(),
            result,
            originalTokens,
            compressedTokens,
            originalCount,
            result.size()
        );
    }
}
