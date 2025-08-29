package org.ruoyi.chat.event;

import org.springframework.context.ApplicationEvent;

/**
 * 聊天消息创建事件（用于异步计费/累计等）
 */
public class ChatMessageCreatedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long sessionId;
    private final String modelName;
    private final String role;
    private final String content;
    private final Long messageId;

    public ChatMessageCreatedEvent(Long userId, Long sessionId, String modelName, String role, String content, Long messageId) {
        super(userId);
        this.userId = userId;
        this.sessionId = sessionId;
        this.modelName = modelName;
        this.role = role;
        this.content = content;
        this.messageId = messageId;
    }

    public Long getUserId() { return userId; }
    public Long getSessionId() { return sessionId; }
    public String getModelName() { return modelName; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Long getMessageId() { return messageId; }
}

