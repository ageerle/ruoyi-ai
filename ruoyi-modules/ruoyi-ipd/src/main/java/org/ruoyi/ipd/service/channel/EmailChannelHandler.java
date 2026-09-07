package org.ruoyi.ipd.service.channel;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationChannelHandler;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.stereotype.Component;

/**
 * 邮件（EMAIL）通道处理器 stub（ROOT-R2-P0-2；防范围爆炸仅占位）。
 *
 * <p>本期仅落日志 + 显式抛 {@link UnsupportedOperationException}，由 dispatcher
 * 退避重试直至 DEAD；真实 SMTP 接入由后续卡（邮件平台集成）替换。
 */
@Slf4j
@Component
public class EmailChannelHandler implements NotificationChannelHandler {

    public static final String STUB_SENTINEL = "email-stub-not-implemented";

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void deliver(NotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            throw new NotificationDispatchException("EMAIL 事件缺 receiverId", new IllegalArgumentException("receiverId required"));
        }
        log.warn("[EMAIL-STUB] 未接入邮件推送，仅占位；eventId={} receiver={} type={}",
            event.getId(), event.getReceiverId(), event.getEventType());
        // 显式标记未实现：dispatcher 应在重试上限后转 DEAD
        throw new UnsupportedOperationException(STUB_SENTINEL);
    }
}
