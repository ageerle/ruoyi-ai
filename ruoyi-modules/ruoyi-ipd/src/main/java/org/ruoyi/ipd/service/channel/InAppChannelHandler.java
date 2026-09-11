package org.ruoyi.ipd.service.channel;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationChannelHandler;
import org.springframework.stereotype.Component;

/**
 * 站内信（INBOX）真实通道处理器（ROOT-R2-P0-2）。
 *
 * <p>INBOX 的"送达"语义：事件已落 {@code notification_events} 即视为送达——
 * 用户主动从收件箱接口（{@code GET /api/v1/notifications}）拉取。
 * 处理器内仅做一次结构校验 + 成功日志，不做实际推送（避免重复发送）。
 */
@Slf4j
@Component
public class InAppChannelHandler implements NotificationChannelHandler {

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.INBOX;
    }

    @Override
    public void deliver(NotificationEvent event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("INBOX 事件必须有 id（落库后由 publish 生成）");
        }
        if (event.getReceiverId() == null) {
            throw new IllegalArgumentException("INBOX 事件必须有 receiverId");
        }
        log.info("[INBOX] receiver={} eventId={} type={} title={}",
            event.getReceiverId(), event.getId(), event.getEventType(), event.getTitle());
    }
}
