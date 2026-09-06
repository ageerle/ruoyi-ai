package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.springframework.stereotype.Component;

/**
 * 一期 Mock 投递渠道（OPS-05）：只写应用日志，不发生任何外部调用。
 * 事件行 channel 列落 "MOCK"，前端/对账侧不得把 SENT+MOCK 解释为"用户已在站外收到"。
 * 真实渠道接入（邮件/企微）在后续卡新增 NotificationChannel 实现并替换注入。
 */
@Slf4j
@Component
public class MockNotificationChannel implements NotificationChannel {

    public static final String CODE = "MOCK";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public void send(NotificationEvent event) {
        log.info("[{}] receiver={} kind={} type={} source={}:{} dedup={} title={}",
            CODE, event.getReceiverId(), event.getKind(), event.getEventType(),
            event.getSourceType(), event.getSourceId(), event.getDedupKey(), event.getTitle());
    }
}
