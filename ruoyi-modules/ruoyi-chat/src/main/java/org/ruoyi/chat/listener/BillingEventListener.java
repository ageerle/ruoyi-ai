package org.ruoyi.chat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.event.ChatMessageCreatedEvent;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final IChatCostService chatCostService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageCreated(ChatMessageCreatedEvent event) {
        try {
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setUserId(event.getUserId());
            chatRequest.setSessionId(event.getSessionId());
            chatRequest.setModel(event.getModelName());
            chatRequest.setRole(event.getRole());
            chatRequest.setPrompt(event.getContent());
            // 异步执行计费累计与扣费
            chatCostService.deductToken(chatRequest);
        } catch (Exception ex) {
            log.error("BillingEventListener onChatMessageCreated error", ex);
        }
    }
}

