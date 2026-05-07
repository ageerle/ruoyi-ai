package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Token 计数器
 * 提供文本和消息的 Token 估算功能
 *
 * @author yang
 * @date 2026-04-27
 */
@Slf4j
public class TokenCounter {

    /**
     * 每条消息的固定格式开销（OpenAI 格式）
     * <|start|>{role}\n{content}<|end|>\n
     */
    private static final int MESSAGE_FORMAT_OVERHEAD = 4;

    /**
     * 对话开始标记开销
     */
    private static final int CONVERSATION_OVERHEAD = 3;

    /**
     * 中文平均每个字符的 Token 比例（约 2 字符 = 1 token）
     */
    private static final double CHINESE_TOKEN_RATIO = 0.5;

    /**
     * 英文平均每个字符的 Token 比例（约 4 字符 = 1 token）
     */
    private static final double ENGLISH_TOKEN_RATIO = 0.25;

    /**
     * 计算文本的 Token 数量（估算）
     *
     * @param text 文本内容
     * @return Token 数量
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isChineseChar(c)) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        // 混合估算
        return (int) Math.ceil(chineseChars * CHINESE_TOKEN_RATIO + otherChars * ENGLISH_TOKEN_RATIO);
    }

    /**
     * 计算单条消息的 Token 数量
     *
     * @param message 消息
     * @return Token 数量
     */
    public int countMessage(ChatMessage message) {
        int tokens = MESSAGE_FORMAT_OVERHEAD;

        // 角色名称
        tokens += countTokens(getRoleName(message));

        // 消息内容
        tokens += countTokens(extractText(message));

        // 名称字段（如果有）
        if (message instanceof UserMessage userMessage && userMessage.name() != null) {
            tokens += countTokens(userMessage.name());
        }

        return tokens;
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
        } else if (message instanceof ToolExecutionResultMessage toolMessage) {
            return toolMessage.text();
        }
        return "";
    }

    /**
     * 计算消息列表的总 Token 数量
     *
     * @param messages 消息列表
     * @return Token 总数
     */
    public int countMessages(Iterable<ChatMessage> messages) {
        if (messages == null) {
            return CONVERSATION_OVERHEAD;
        }

        int total = CONVERSATION_OVERHEAD;
        int count = 0;

        for (ChatMessage message : messages) {
            total += countMessage(message);
            count++;
        }

        log.debug("Token 计数: {} 条消息, 约 {} tokens", count, total);
        return total;
    }

    /**
     * 估算指定 Token 预算下可容纳的消息数量
     *
     * @param maxTokens Token 预算
     * @param avgTokensPerMessage 每条消息平均 Token 数
     * @return 可容纳的消息数量
     */
    public int estimateMaxMessages(int maxTokens, int avgTokensPerMessage) {
        if (avgTokensPerMessage <= 0) {
            avgTokensPerMessage = 50; // 默认每条消息 50 tokens
        }
        return Math.max(1, (maxTokens - CONVERSATION_OVERHEAD) / (avgTokensPerMessage + MESSAGE_FORMAT_OVERHEAD));
    }

    /**
     * 获取消息的角色名称
     */
    private String getRoleName(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return "system";
        } else if (message instanceof UserMessage) {
            return "user";
        } else if (message instanceof AiMessage) {
            return "assistant";
        } else if (message instanceof ToolExecutionResultMessage) {
            return "tool";
        }
        return "user";
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChineseChar(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN;
    }
}
