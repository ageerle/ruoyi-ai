package org.ruoyi.service.chat.impl.memory.strategy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.impl.memory.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 摘要策略
 * 当超过 Token 限制时，使用 LLM 对旧消息进行摘要压缩
 *
 * @author yang
 * @date 2026-04-29
 */
@Slf4j
@Component
public class SummarizationStrategy implements MemoryCompressionStrategy {

    @Override
    public String getName() {
        return "summarization";
    }

    @Override
    public int getPriority() {
        return 50; // 高优先级，优先尝试摘要
    }

    @Override
    public boolean isComposable() {
        return true; // 可以与截断策略组合使用
    }

    @Override
    public boolean needsCompression(CompressionContext context) {
        // 条件：1. 有摘要模型  2. 消息数足够  3. Token 使用达到比例阈值
        // 摘要策略提前介入，在还没超限时就开始压缩，保留语义
        return context.getSummarizer() != null
            && context.getMessages().size() > context.getSummarizeThreshold()
            && context.getUsageRatio() >= context.getSummarizeTokenRatio();
    }

    @Override
    public CompressionResult compress(CompressionContext context) {
        List<ChatMessage> messages = context.getMessages();
        ChatModel summarizer = context.getSummarizer();
        TokenCounter tokenCounter = context.getTokenCounter();

        if (summarizer == null) {
            log.warn("[摘要策略] 摘要模型未配置，无法执行摘要");
            return CompressionResult.failure(getName(), "摘要模型未配置");
        }

        if (messages == null || messages.size() < context.getSummarizeThreshold()) {
            log.debug("[摘要策略] 消息数 {} 小于阈值 {}，跳过摘要",
                messages != null ? messages.size() : 0, context.getSummarizeThreshold());
            return CompressionResult.failure(getName(), "消息数不足");
        }

        int originalTokens = context.getCurrentTokens();
        int originalCount = messages.size();

        try {
            // 分离系统消息和普通消息
            List<ChatMessage> systemMessages = new ArrayList<>();
            List<ChatMessage> regularMessages = new ArrayList<>();

            for (ChatMessage msg : messages) {
                if (msg instanceof SystemMessage) {
                    systemMessages.add(msg);
                } else {
                    regularMessages.add(msg);
                }
            }

            // 选择要摘要的消息（前半部分）
            int summarizeCount = regularMessages.size() / 2;
            List<ChatMessage> toSummarize = regularMessages.subList(0, summarizeCount);
            List<ChatMessage> toKeep = regularMessages.subList(summarizeCount, regularMessages.size());

            log.info("[摘要策略] 开始摘要: 原消息数={}, 待摘要={}, 保留={}",
                messages.size(), summarizeCount, toKeep.size());

            // 构建摘要提示
            StringBuilder summaryPrompt = new StringBuilder();
            summaryPrompt.append("请用简洁的语言总结以下对话的关键信息，保留重要的上下文和用户偏好：\n\n");

            for (ChatMessage msg : toSummarize) {
                summaryPrompt.append(extractText(msg)).append("\n");
            }

            // 调用 LLM 生成摘要
            String summary = summarizer.chat(summaryPrompt.toString());
            log.info("[摘要策略] 生成摘要成功: {}...",
                summary.substring(0, Math.min(100, summary.length())));

            // 构建新的消息列表
            List<ChatMessage> result = new ArrayList<>(systemMessages);
            result.add(SystemMessage.from("【历史对话摘要】" + summary));
            result.addAll(toKeep);

            int compressedTokens = tokenCounter.countMessages(result);

            log.info("[摘要策略] 完成: 原消息数={} → 新消息数={}, Token: {} → {}",
                originalCount, result.size(), originalTokens, compressedTokens);

            return CompressionResult.success(
                getName(),
                result,
                originalTokens,
                compressedTokens,
                originalCount,
                result.size()
            );

        } catch (Exception e) {
            log.error("[摘要策略] 执行失败: {}", e.getMessage(), e);
            return CompressionResult.failure(getName(), e.getMessage());
        }
    }

    /**
     * 从消息中提取文本内容
     */
    private String extractText(ChatMessage message) {
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        } else if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        } else if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        return "";
    }
}
