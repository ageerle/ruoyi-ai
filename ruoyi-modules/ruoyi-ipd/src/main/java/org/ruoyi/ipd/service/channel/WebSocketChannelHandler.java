package org.ruoyi.ipd.service.channel;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationChannelHandler;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.stereotype.Component;

/**
 * WebSocket 实时推送通道 stub（ROOT-R2-P0-2；防范围爆炸仅占位）。
 *
 * <p>真实实现应通过 {@code ruoyi-common-websocket} 推送到用户 session
 * （{@code WebSocketSession.sendMessage}），并按 receiverId 路由 session。
 * 本期仅日志占位，由后续卡接入。
 */
@Slf4j
@Component
public class WebSocketChannelHandler implements NotificationChannelHandler {

    public static final String STUB_SENTINEL = "websocket-stub-not-implemented";

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.WEBSOCKET;
    }

    @Override
    public void deliver(NotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            throw new NotificationDispatchException("WEBSOCKET 事件缺 receiverId", new IllegalArgumentException("receiverId required"));
        }
        log.warn("[WEBSOCKET-STUB] 未接入实时推送，仅占位；eventId={} receiver={} type={}",
            event.getId(), event.getReceiverId(), event.getEventType());
        // 显式标记未实现：dispatcher 应在重试上限后转 DEAD
        throw new UnsupportedOperationException(STUB_SENTINEL);
    }
}
