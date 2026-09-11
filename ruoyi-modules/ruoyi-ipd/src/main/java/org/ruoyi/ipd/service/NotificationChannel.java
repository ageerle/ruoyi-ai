package org.ruoyi.ipd.service;

import org.ruoyi.ipd.domain.NotificationEvent;

/**
 * 站内通知投递渠道（OPS-05）。
 *
 * <p>一期仅有 {@link MockNotificationChannel}（code=MOCK，只写日志不外发）；
 * 接入真实渠道（邮件/企微）时新增实现类并在此登记说明，不改本接口。
 * {@link #send} 抛出任意异常即视为本次投递失败，事件进入退避重试（PENDING→FAILED→…→DEAD）。
 */
public interface NotificationChannel {

    /**
     * 渠道编码；落 {@code notification_events.channel} 列，用于事后对账"当时经哪个渠道投的"。
     *
     * @return 渠道编码（一期 MOCK）
     */
    String code();

    /**
     * 投递一条通知（幂等性由调用方 dispatchPending 的条件 UPDATE 守卫，渠道实现无需自行去重）。
     *
     * @param event 待投递事件（含 receiver/kind/title/content/action_url）
     */
    void send(NotificationEvent event);
}
