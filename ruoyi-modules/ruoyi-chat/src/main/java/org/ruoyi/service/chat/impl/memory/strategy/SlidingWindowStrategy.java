package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.impl.memory.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口策略
 * 保留最近 N 条消息，丢弃更早的消息
 * 注意：始终保留系统消息，仅对普通消息进行窗口截断
 *
 * @author yang
 * @date 2026-04-29
 */
@Slf4j
@Component
public class SlidingWindowStrategy implements MemoryCompressionStrategy {

    /**
     * 默认窗口大小
     */
    private static final int DEFAULT_WINDOW_SIZE = 20;

    @Override
    public String getName() {
        return "sliding-window";
    }

    @Override
    public int getPriority() {
        return 150; // 低优先级，作为兜底策略
    }

    @Override
    public boolean needsCompression(CompressionContext context) {
        // 滑动窗口策略只检查消息数量，不考虑 Token
        // 仅当消息数超过窗口大小时才需要压缩
        int windowSize = getWindowSize(context);
        return context.getMessages().size() > windowSize;
    }

    @Override
    public CompressionResult compress(CompressionContext context) {
        List<ChatMessage> messages = context.getMessages();
        int windowSize = getWindowSize(context);
        boolean preserveSystem = context.isPreserveSystemMessages();
        TokenCounter tokenCounter = context.getTokenCounter();

        if (messages == null || messages.isEmpty()) {
            return CompressionResult.success(getName(), messages, 0, 0, 0, 0);
        }

        int originalTokens = context.getCurrentTokens();
        int originalCount = messages.size();

        // 分离系统消息和普通消息（与 TruncationStrategy 保持一致）
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> regularMessages = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if (preserveSystem && msg instanceof SystemMessage) {
                systemMessages.add(msg);
            } else {
                regularMessages.add(msg);
            }
        }

        // 保留系统消息 + 最近 N 条普通消息
        int fromIndex = Math.max(0, regularMessages.size() - windowSize);
        List<ChatMessage> keptRegularMessages = new ArrayList<>(regularMessages.subList(fromIndex, regularMessages.size()));

        // 合并结果：系统消息 + 保留的普通消息
        List<ChatMessage> result = new ArrayList<>(systemMessages);
        result.addAll(keptRegularMessages);

        int compressedTokens = tokenCounter.countMessages(result);

        log.info("[滑动窗口策略] 完成: 原消息数={} → 截断后={}, 系统消息={}, 窗口大小={}",
            originalCount, result.size(), systemMessages.size(), windowSize);

        return CompressionResult.success(
            getName(),
            result,
            originalTokens,
            compressedTokens,
            originalCount,
            result.size()
        );
    }

    /**
     * 获取窗口大小
     * 可从上下文或配置中获取
     */
    private int getWindowSize(CompressionContext context) {
        // 可以从上下文的配置中获取，这里使用默认值
        return DEFAULT_WINDOW_SIZE;
    }
}
