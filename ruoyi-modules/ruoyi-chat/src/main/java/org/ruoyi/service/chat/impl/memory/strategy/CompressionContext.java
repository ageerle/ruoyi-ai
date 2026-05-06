package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Builder;
import lombok.Data;
import org.ruoyi.service.chat.impl.memory.TokenCounter;

import java.util.List;

/**
 * 压缩上下文
 * 包含压缩所需的所有输入信息
 *
 * @author yang
 * @date 2026-04-29
 */
@Data
@Builder
public class CompressionContext {

    /**
     * 内存 ID（通常是会话 ID）
     */
    private Object memoryId;

    /**
     * 原始消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 当前 Token 数量
     */
    private int currentTokens;

    /**
     * Token 上限
     */
    private int maxTokens;

    /**
     * 预留给回复的 Token 数
     */
    @Builder.Default
    private int reservedForReply = 2000;

    /**
     * 有效 Token 上限（maxTokens - reservedForReply）
     * 添加边界检查，确保返回值 >= 1，避免负数或零导致的计算错误
     */
    public int getEffectiveMaxTokens() {
        int effective = maxTokens - reservedForReply;
        // 确保至少返回 1，避免负数或零导致的策略判断失效
        return Math.max(1, effective);
    }

    /**
     * Token 使用比例
     * 添加除零保护，当 effectiveMaxTokens 为 0 时返回 1.0（表示已满）
     */
    public double getUsageRatio() {
        int effectiveMax = maxTokens - reservedForReply;
        if (effectiveMax <= 0) {
            // 当预留空间超过或等于上限时，视为已满
            return 1.0;
        }
        return (double) currentTokens / effectiveMax;
    }

    /**
     * 是否超过 Token 限制
     */
    public boolean isOverLimit() {
        return currentTokens > getEffectiveMaxTokens();
    }

    /**
     * 超出的 Token 数量
     */
    public int getExcessTokens() {
        return Math.max(0, currentTokens - getEffectiveMaxTokens());
    }

    /**
     * 摘要模型（可选，用于摘要策略）
     */
    private ChatModel summarizer;

    /**
     * Token 计数器
     */
    private TokenCounter tokenCounter;

    /**
     * 是否保留系统消息
     */
    @Builder.Default
    private boolean preserveSystemMessages = true;

    /**
     * 摘要触发阈值 - Token 使用比例
     */
    @Builder.Default
    private double summarizeTokenRatio = 0.7;

    /**
     * 摘要触发阈值 - 消息数量
     */
    @Builder.Default
    private int summarizeThreshold = 10;
}
