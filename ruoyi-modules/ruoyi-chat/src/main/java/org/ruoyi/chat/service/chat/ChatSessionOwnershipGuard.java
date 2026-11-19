package org.ruoyi.chat.service.chat;

import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.domain.vo.chat.ChatSessionVo;
import org.springframework.stereotype.Component;

/** Fail-closed ownership check shared by HTTP and WebSocket chat entry points. */
@Component
@RequiredArgsConstructor
public class ChatSessionOwnershipGuard {

    private final IChatSessionService chatSessionService;

    public void requireOwned(Long userId, Long sessionId) {
        if (userId == null || userId <= 0 || sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("An authenticated chat session is required");
        }
        ChatSessionVo session = chatSessionService.queryById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            // Do not reveal whether a guessed identifier exists.
            throw new IllegalArgumentException("Chat session is unavailable");
        }
    }
}
