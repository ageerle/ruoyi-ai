package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产环境通知通道占位实现（OPS-05 后续待办）。
 *
 * <p>仅 prod profile 生效。真实邮件/企微通道尚未接入时，此实现确保 Spring 注入不失败，
 * 同时以 WARN 级别显式标记"通知通道未配置"，避免像 dev Mock 那样静默写 INFO 日志被误判为已发送。
 *
 * <p>与 {@link MockNotificationChannel} 的区别：
 * <ul>
 *   <li>Mock（dev）：code=MOCK，INFO 日志，用于开发调试；</li>
 *   <li>NoOp（prod）：code=NOOP，WARN 日志，明确标记未配置，提醒运维接入真实通道。</li>
 * </ul>
 *
 * <p>真实渠道接入后，新增 {@code @Profile("prod")} 的 EmailChannel / WecomChannel 实现并移除此类。
 */
@Slf4j
@Component
@Profile("prod")
public class NoOpNotificationChannel implements NotificationChannel {

    public static final String CODE = "NOOP";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public void send(NotificationEvent event) {
        log.warn("[NOOP] 通知通道未配置，事件未真实发送——receiver={} kind={} type={} source={}:{} title={}",
            event.getReceiverId(), event.getKind(), event.getEventType(),
            event.getSourceType(), event.getSourceId(), event.getTitle());
    }
}
