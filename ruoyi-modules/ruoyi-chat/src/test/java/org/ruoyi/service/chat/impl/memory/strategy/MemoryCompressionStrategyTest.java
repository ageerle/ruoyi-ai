package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.ruoyi.service.chat.impl.memory.ChatMemoryProperties;
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
 * 测试两种策略：Token截断、摘要压缩
 *
 * @author yang
 * @date 2026-05-06
 */
@Tag("dev")
class MemoryCompressionStrategyTest {

    private TokenCounter tokenCounter;
    private TruncationStrategy truncationStrategy;
    private SummarizationStrategy summarizationStrategy;

    @BeforeEach
    void setUp() {
        tokenCounter = new TokenCounter();
        truncationStrategy = new TruncationStrategy();
        summarizationStrategy = new SummarizationStrategy();
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
     * 检查是否包含摘要消息
     */
    private boolean hasSummaryMessage(List<ChatMessage> messages) {
        return messages.stream()
            .filter(m -> m instanceof SystemMessage)
            .anyMatch(m -> ((SystemMessage) m).text().contains("历史对话摘要"));
    }

    // ========== Message 策略测试 ==========

    @Test
    @DisplayName("Message策略 - 默认配置验证")
    void testMessageStrategy_DefaultConfig() {
        ChatMemoryProperties properties = new ChatMemoryProperties();

        // 验证默认策略是 message
        assertEquals("message", properties.getStrategy());
        assertEquals(20, properties.getMaxMessages());

        System.out.println("[Message策略] 默认配置: strategy=" + properties.getStrategy() + ", maxMessages=" + properties.getMaxMessages());
    }

    @Test
    @DisplayName("Message策略 - 滑动窗口自动移除旧消息")
    void testMessageStrategy_SlidingWindow() {
        int maxMessages = 5;

        // 使用 LangChain4j 原生的 MessageWindowChatMemory
        InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .id("test-session")
            .maxMessages(maxMessages)
            .chatMemoryStore(store)
            .build();

        // 添加系统消息
        memory.add(SystemMessage.from("系统提示"));

        // 添加10条普通消息
        for (int i = 0; i < 10; i++) {
            memory.add(UserMessage.from("用户消息" + i));
            memory.add(AiMessage.from("AI回复" + i));
        }

        // 获取消息列表
        List<ChatMessage> messages = memory.messages();

        // 验证只保留最新的 maxMessages 条消息（包括系统消息）
        assertEquals(maxMessages, messages.size());

        // 验证系统消息被保留
        assertTrue(messages.stream().anyMatch(m -> m instanceof SystemMessage));

        // 验证是最新的消息（消息8、9）
        assertTrue(messages.stream()
            .filter(m -> m instanceof UserMessage)
            .anyMatch(m -> ((UserMessage) m).singleText().equals("用户消息9")));

        System.out.println("[Message策略] 滑动窗口: 添加21条消息 → 保留" + messages.size() + "条消息 (maxMessages=" + maxMessages + ")");
    }

    @Test
    @DisplayName("Message策略 - 不涉及Token管理")
    void testMessageStrategy_NoTokenManagement() {
        // 创建大量消息
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .id("test-session")
            .maxMessages(10)
            .chatMemoryStore(new InMemoryChatMemoryStore())
            .build();

        // 添加超长消息（模拟大量Token）
        String longContent = "这是一条非常长的消息内容".repeat(1000);
        for (int i = 0; i < 10; i++) {
            memory.add(UserMessage.from(longContent));
        }

        // Message策略不考虑Token，只按消息数量截断
        List<ChatMessage> messages = memory.messages();
        assertEquals(10, messages.size());

        // 验证所有消息都是完整的长消息
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage) {
                assertTrue(((UserMessage) msg).singleText().length() > 10000);
            }
        }

        System.out.println("[Message策略] 不涉及Token管理: 10条超长消息完整保留，每条长度>10000字符");
    }
}
