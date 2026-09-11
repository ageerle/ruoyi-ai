package org.ruoyi.ipd.service;

import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;

/**
 * 单通道投递处理器（ROOT-R2-P0-2：与 {@code NotificationChannelType} 一一对应）。
 *
 * <p>与既有 {@link NotificationChannel}（一期 MOCK 投递处理器接口）的关系：
 * <ul>
 *   <li>本接口是"按目标通道路由到具体投递器"——支持 INBOX/EMAIL/WEBSOCKET 三类路由</li>
 *   <li>{@link NotificationChannel} 是"具体投递实现"——本卡复用既有 MOCK 作为 EMAIL stub 的占位</li>
 * </ul>
 *
 * <p>一个 channel type 对应一个 handler（注册到 {@code AsyncNotificationDispatcher}）。
 * 抛任何 RuntimeException 视为本次失败，dispatcher 进入退避重试。
 */
public interface NotificationChannelHandler {

    /**
     * 本处理器支持的通道类型。
     */
    NotificationChannelType channelType();

    /**
     * 执行投递（幂等性由调用方控制：dispatcher 以条件 UPDATE 守卫）。
     *
     * <p>INBOX 实现：no-op（事件已落库即视为"已送达"——用户主动收件箱读取）。
     * EMAIL 实现：调用 SMTP / 邮件平台（本期 stub：仅日志）。
     * WEBSOCKET 实现：通过 ruoyi-common-websocket 推送到用户 session（本期 stub：仅日志）。
     */
    void deliver(NotificationEvent event);
}
