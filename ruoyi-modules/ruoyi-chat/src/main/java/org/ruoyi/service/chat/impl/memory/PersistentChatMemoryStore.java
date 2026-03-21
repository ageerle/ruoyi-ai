package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.IChatMessageService;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化聊天内存存储 - 将消息存储到数据库
 * 支持每个用户/会话的独立消息历史
 *
 * @author ageerle@163.com
 * @date 2025/01/10
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final IChatMessageService chatMessageService;


    /**
     * 根据会话ID获取历史消息
     * 转换为LangChain4j的ChatMessage格式
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            if (memoryId == null) {
                return new ArrayList<>();
            }

            Long sessionId = Long.parseLong(memoryId.toString());
            // 从数据库获取该会话的所有消息
            return chatMessageService.getMessagesBySessionId(sessionId);
        } catch (Exception e) {
            log.error("获取会话 {} 的消息失败: {}", memoryId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新会话的消息历史
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            if (memoryId == null || messages == null || messages.isEmpty()) {
                return;
            }
            Long sessionId = Long.parseLong(memoryId.toString());
            log.debug("成功更新会话 {} 的消息，共 {} 条", sessionId, messages.size());
        } catch (Exception e) {
            log.error("更新会话 {} 的消息失败: {}", memoryId, e.getMessage(), e);
        }
    }

    /**
     * 删除会话的所有消息
     */
    @Override
    public void deleteMessages(Object memoryId) {
        try {
            if (memoryId == null) {
                return;
            }

            Long sessionId = Long.parseLong(memoryId.toString());
            chatMessageService.deleteBySessionId(sessionId);

            log.info("成功删除会话 {} 的所有消息", sessionId);
        } catch (Exception e) {
            log.error("删除会话 {} 的消息失败: {}", memoryId, e.getMessage(), e);
        }
    }


}
