package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 压缩结果
 * 包含压缩后的消息和元数据
 *
 * @author yang
 * @date 2026-04-29
 */
@Data
@Builder
public class CompressionResult {

    /**
     * 压缩后的消息列表
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 压缩后的 Token 数量
     */
    private int compressedTokens;

    /**
     * 压缩前 Token 数量
     */
    private int originalTokens;

    /**
     * 压缩前消息数量
     */
    private int originalMessageCount;

    /**
     * 压缩后消息数量
     */
    private int compressedMessageCount;

    /**
     * 使用的压缩策略名称
     */
    private String strategyName;

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 压缩摘要（可选，用于记录压缩了哪些内容）
     */
    private String compressionSummary;

    /**
     * 被移除的消息数量
     */
    public int getRemovedCount() {
        return originalMessageCount - compressedMessageCount;
    }

    /**
     * 节省的 Token 数量
     */
    public int getSavedTokens() {
        return originalTokens - compressedTokens;
    }

    /**
     * Token 压缩比例
     */
    public double getCompressionRatio() {
        if (originalTokens == 0) {
            return 0;
        }
        return (double) getSavedTokens() / originalTokens;
    }

    /**
     * 创建失败结果
     */
    public static CompressionResult failure(String strategyName, String errorMessage) {
        return CompressionResult.builder()
            .strategyName(strategyName)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * 创建成功结果
     */
    public static CompressionResult success(String strategyName, List<ChatMessage> messages,
                                            int originalTokens, int compressedTokens,
                                            int originalCount, int compressedCount) {
        return CompressionResult.builder()
            .strategyName(strategyName)
            .success(true)
            .messages(messages)
            .originalTokens(originalTokens)
            .compressedTokens(compressedTokens)
            .originalMessageCount(originalCount)
            .compressedMessageCount(compressedCount)
            .build();
    }
}
