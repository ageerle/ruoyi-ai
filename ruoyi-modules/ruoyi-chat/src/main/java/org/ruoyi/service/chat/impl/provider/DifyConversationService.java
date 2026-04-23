package org.ruoyi.service.chat.impl.provider;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Dify 会话映射管理
 * <p>
 * 维护 ruoyi sessionId 与 Dify conversation_id 的映射关系，
 * 确保多轮对话上下文连续。
 *
 * @author better
 */
@Service
public class DifyConversationService {

    private final ConcurrentHashMap<Long, String> conversationMap = new ConcurrentHashMap<>();

    public String getConversationId(Long sessionId) {
        return conversationMap.get(sessionId);
    }

    public void saveMapping(Long sessionId, String difyConversationId) {
        if (sessionId != null && difyConversationId != null) {
            conversationMap.put(sessionId, difyConversationId);
        }
    }

    public void clearMapping(Long sessionId) {
        if (sessionId != null) {
            conversationMap.remove(sessionId);
        }
    }
}
