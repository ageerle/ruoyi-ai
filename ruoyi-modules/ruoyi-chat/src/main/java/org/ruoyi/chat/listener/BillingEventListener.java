package org.ruoyi.chat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.event.ChatMessageCreatedEvent;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final IChatCostService chatCostService;

    @Async
    @EventListener
    public void onChatMessageCreated(ChatMessageCreatedEvent event) {
        log.debug("BillingEventListener->接收到计费事件，用户ID: {}，会话ID: {}，模型: {}",
                event.getUserId(), event.getSessionId(), event.getModelName());
        try {
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setUserId(event.getUserId());
            chatRequest.setSessionId(event.getSessionId());
            chatRequest.setModel(event.getModelName());
            chatRequest.setRole(event.getRole());
            chatRequest.setPrompt(event.getContent());
            chatRequest.setMessageId(event.getMessageId()); // 设置消息ID
            // 异步执行计费累计与扣费
            log.debug("BillingEventListener->开始执行计费逻辑");
            chatCostService.deductToken(chatRequest);
            log.debug("BillingEventListener->计费逻辑执行完成");
        } catch (Exception ex) {
            // 由于已有预检查，这里的异常主要是系统异常（数据库连接等）
            // 记录错误但不中断异步线程
            log.error("BillingEventListener->异步计费异常，用户ID: {}，模型: {}，错误: {}",
                    event.getUserId(), event.getModelName(), ex.getMessage(), ex);

            // TODO: 可以考虑加入重试机制或者错误通知机制
            // 例如：发送到死信队列，或者通知运维人员
        }
    }
}

