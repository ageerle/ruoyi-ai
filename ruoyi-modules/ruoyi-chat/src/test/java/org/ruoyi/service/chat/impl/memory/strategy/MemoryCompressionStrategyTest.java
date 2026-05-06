package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.ruoyi.service.chat.impl.memory.TokenCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 内存压缩策略测试类
 * 测试三种策略：滑动窗口、Token截断、摘要压缩
 *
 * @author yang
 * @date 2026-05-06
 */
@Tag("dev")
class MemoryCompressionStrategyTest {

    private TokenCounter tokenCounter;
    private SlidingWindowStrategy slidingWindowStrategy;
    private TruncationStrategy truncationStrategy;
    private SummarizationStrategy summarizationStrategy;

    @BeforeEach
    void setUp() {
        tokenCounter = new TokenCounter();
        slidingWindowStrategy = new SlidingWindowStrategy();
        truncationStrategy = new TruncationStrategy();
        summarizationStrategy = new SummarizationStrategy();
    }

    // ========== 滑动窗口策略测试 ==========

    @Test
    @DisplayName("滑动窗口策略 - 消息数超过窗口大小时截断")
    void testSlidingWindow_ExceedWindowSize() {
        // 创建测试消息：1条系统消息 + 30条普通消息
        List<ChatMessage> messages = createTestMessages(1, 30);

        // 创建上下文
        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(100000)  // Token充足，测试消息数量截断
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        // 验证需要压缩
        assertTrue(slidingWindowStrategy.needsCompression(context));

        // 执行压缩
        CompressionResult result = slidingWindowStrategy.compress(context);

        // 验证结果
        assertTrue(result.isSuccess());
        // 系统消息(1) + 最近20条普通消息 = 21条
        assertEquals(21, result.getCompressedMessageCount());
        // 系统消息应保留
        assertTrue(hasSystemMessage(result.getMessages()));
    }

    @Test
    @DisplayName("滑动窗口策略 - 消息数未超过窗口大小时不截断")
    void testSlidingWindow_BelowWindowSize() {
        // 创建测试消息：1条系统消息 + 5对普通消息（= 10条普通消息，总共11条，小于窗口大小20）
        List<ChatMessage> messages = createTestMessages(1, 5);

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(100000)  // Token充足，不超限
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        // 验证不需要压缩（消息数未超限且Token未超限）
        assertFalse(context.isOverLimit());  // Token未超限
        assertFalse(slidingWindowStrategy.needsCompression(context));

        // 执行压缩（即使不需要，执行也应返回原消息）
        CompressionResult result = slidingWindowStrategy.compress(context);

        assertTrue(result.isSuccess());
        assertEquals(11, result.getCompressedMessageCount());
    }

    @Test
    @DisplayName("滑动窗口策略 - 系统消息始终保留")
    void testSlidingWindow_PreserveSystemMessage() {
        // 创建测试消息：3条系统消息 + 25条普通消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("系统提示1"));
        messages.add(SystemMessage.from("系统提示2"));
        messages.add(SystemMessage.from("系统提示3"));
        for (int i = 0; i < 25; i++) {
            messages.add(UserMessage.from("用户消息" + i));
            messages.add(AiMessage.from("AI回复" + i));
        }

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(100000)
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        CompressionResult result = slidingWindowStrategy.compress(context);

        // 验证所有系统消息保留
        assertTrue(result.isSuccess());
        int systemCount = countSystemMessages(result.getMessages());
        assertEquals(3, systemCount);
    }

    // ========== Token截断策略测试 ==========

    @Test
    @DisplayName("Token截断策略 - Token超限时截断")
    void testTruncation_ExceedTokenLimit() {
        // 创建大量消息使Token超限
        List<ChatMessage> messages = createLargeMessages(50);

        int currentTokens = tokenCounter.countMessages(messages);
        // 设置较小的Token限制
        int maxTokens = 2000;

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(currentTokens)
            .maxTokens(maxTokens)
            .reservedForReply(500)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        // 验证需要压缩
        assertTrue(context.isOverLimit());
        assertTrue(truncationStrategy.needsCompression(context));

        // 执行压缩
        CompressionResult result = truncationStrategy.compress(context);

        // 验证结果
        assertTrue(result.isSuccess());
        // 新Token数应小于有效上限
        assertTrue(result.getCompressedTokens() <= context.getEffectiveMaxTokens());
        // 系统消息应保留
        assertTrue(hasSystemMessage(result.getMessages()));
    }

    @Test
    @DisplayName("Token截断策略 - Token未超限时不截断")
    void testTruncation_BelowTokenLimit() {
        List<ChatMessage> messages = createTestMessages(1, 10);

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(10000)  // Token充足
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        // 验证不需要压缩
        assertFalse(context.isOverLimit());
        assertFalse(truncationStrategy.needsCompression(context));
    }

    @Test
    @DisplayName("Token截断策略 - 系统消息占用全部空间时的降级")
    void testTruncation_SystemMessagesExceedLimit() {
        // 创建超长系统消息
        List<ChatMessage> messages = new ArrayList<>();
        String longSystemContent = "这是一个非常长的系统提示内容".repeat(1000);
        messages.add(SystemMessage.from(longSystemContent));

        // 设置极小的Token限制
        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(100)  // 极小限制
            .reservedForReply(50)
            .tokenCounter(tokenCounter)
            .preserveSystemMessages(true)
            .build();

        CompressionResult result = truncationStrategy.compress(context);

        // 即使超限，系统消息也应保留
        assertTrue(result.isSuccess());
        assertTrue(hasSystemMessage(result.getMessages()));
    }

    // ========== 摘要策略测试 ==========

    @Test
    @DisplayName("摘要策略 - 达到比例阈值时触发摘要")
    void testSummarization_TriggerAtRatio() {
        List<ChatMessage> messages = createTestMessages(1, 20);

        int currentTokens = tokenCounter.countMessages(messages);
        // 设置Token限制使使用率达到70%
        int maxTokens = (int) (currentTokens / 0.7) + 100;

        // Mock摘要模型
        ChatModel mockSummarizer = mock(ChatModel.class);
        when(mockSummarizer.chat(anyString())).thenReturn("这是对话摘要内容");

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(currentTokens)
            .maxTokens(maxTokens)
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .summarizer(mockSummarizer)
            .summarizeThreshold(10)
            .summarizeTokenRatio(0.7)
            .preserveSystemMessages(true)
            .build();

        // 验证需要压缩（使用率达到70%）
        assertTrue(context.getUsageRatio() >= 0.7);
        assertTrue(summarizationStrategy.needsCompression(context));

        // 执行压缩
        CompressionResult result = summarizationStrategy.compress(context);

        // 验证结果
        assertTrue(result.isSuccess());
        // 摘要后消息数应减少
        assertTrue(result.getCompressedMessageCount() < messages.size());
        // 系统消息应保留
        assertTrue(hasSystemMessage(result.getMessages()));
        // 应包含摘要消息
        assertTrue(hasSummaryMessage(result.getMessages()));
    }

    @Test
    @DisplayName("摘要策略 - 消息数不足时不触发摘要")
    void testSummarization_BelowThreshold() {
        // 消息数小于阈值（10条）
        List<ChatMessage> messages = createTestMessages(1, 5);

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(1000)
            .reservedForReply(200)
            .tokenCounter(tokenCounter)
            .summarizeThreshold(10)
            .summarizeTokenRatio(0.7)
            .build();

        // 验证不需要压缩
        assertFalse(summarizationStrategy.needsCompression(context));
    }

    @Test
    @DisplayName("摘要策略 - 无摘要模型时返回失败")
    void testSummarization_NoSummarizer() {
        List<ChatMessage> messages = createTestMessages(1, 20);

        CompressionContext context = CompressionContext.builder()
            .messages(messages)
            .currentTokens(tokenCounter.countMessages(messages))
            .maxTokens(1000)
            .reservedForReply(200)
            .tokenCounter(tokenCounter)
            .summarizer(null)  // 无摘要模型
            .summarizeThreshold(10)
            .summarizeTokenRatio(0.7)
            .build();

        // 验证不需要压缩（无摘要模型）
        assertFalse(summarizationStrategy.needsCompression(context));

        // 执行压缩应返回失败
        CompressionResult result = summarizationStrategy.compress(context);
        assertFalse(result.isSuccess());
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("边界检查 - reservedForReply超过maxTokens时有效上限为1")
    void testBoundary_EffectiveMaxTokensMinimum() {
        CompressionContext context = CompressionContext.builder()
            .maxTokens(100)
            .reservedForReply(200)  // 超过maxTokens
            .build();

        // 有效上限应为1（最小值）
        assertEquals(1, context.getEffectiveMaxTokens());
        // 使用率应为1.0（已满）
        assertEquals(1.0, context.getUsageRatio());
    }

    @Test
    @DisplayName("边界检查 - 空消息列表处理")
    void testBoundary_EmptyMessages() {
        List<ChatMessage> emptyMessages = new ArrayList<>();

        CompressionContext context = CompressionContext.builder()
            .messages(emptyMessages)
            .currentTokens(0)
            .maxTokens(10000)
            .reservedForReply(2000)
            .tokenCounter(tokenCounter)
            .build();

        // 所有策略对空消息都应正常处理
        CompressionResult slidingResult = slidingWindowStrategy.compress(context);
        assertTrue(slidingResult.isSuccess());
        assertEquals(0, slidingResult.getCompressedMessageCount());

        CompressionResult truncResult = truncationStrategy.compress(context);
        assertTrue(truncResult.isSuccess());
        assertEquals(0, truncResult.getCompressedMessageCount());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试消息
     * @param systemCount 系统消息数量
     * @param regularPairs 普通消息对数（每对包含用户消息和AI回复）
     */
    private List<ChatMessage> createTestMessages(int systemCount, int regularPairs) {
        List<ChatMessage> messages = new ArrayList<>();

        for (int i = 0; i < systemCount; i++) {
            messages.add(SystemMessage.from("系统提示" + i));
        }

        for (int i = 0; i < regularPairs; i++) {
            messages.add(UserMessage.from("用户消息内容" + i));
            messages.add(AiMessage.from("AI回复内容" + i));
        }

        return messages;
    }

    /**
     * 创建大量消息（用于Token超限测试）
     */
    private List<ChatMessage> createLargeMessages(int pairs) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("系统提示"));

        for (int i = 0; i < pairs; i++) {
            // 每条消息较长，增加Token数
            messages.add(UserMessage.from("这是第" + i + "条用户消息，内容比较长，用于测试Token限制".repeat(5)));
            messages.add(AiMessage.from("这是第" + i + "条AI回复，内容也比较长，用于测试Token限制".repeat(5)));
        }

        return messages;
    }

    /**
     * 检查消息列表是否包含系统消息
     */
    private boolean hasSystemMessage(List<ChatMessage> messages) {
        return messages.stream().anyMatch(m -> m instanceof SystemMessage);
    }

    /**
     * 统计系统消息数量
     */
    private int countSystemMessages(List<ChatMessage> messages) {
        return (int) messages.stream().filter(m -> m instanceof SystemMessage).count();
    }

    /**
     * 检查是否包含摘要消息
     */
    private boolean hasSummaryMessage(List<ChatMessage> messages) {
        return messages.stream()
            .filter(m -> m instanceof SystemMessage)
            .anyMatch(m -> ((SystemMessage) m).text().contains("历史对话摘要"));
    }
}