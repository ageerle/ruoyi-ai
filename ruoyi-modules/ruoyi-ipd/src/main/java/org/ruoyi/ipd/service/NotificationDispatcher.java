package org.ruoyi.ipd.service;

import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;

/**
 * 通知分发器接口（ROOT-R2-P0-2：NotificationService 中间件化）。
 *
 * <p>与既有 {@link NotificationService#dispatchPending(int)} 的关系：
 * <ul>
 *   <li>{@code NotificationService.dispatchPending} — 扫表 + 走单一 {@code NotificationChannel} 投递 + 状态翻 SENT/FAILED/DEAD；运维管理端点</li>
 *   <li>{@code NotificationDispatcher.dispatch*} — 单事件立即分发（同步/异步），按 {@link NotificationChannelType} 多通道路由，可聚合降级</li>
 * </ul>
 *
 * <p>典型用法：业务事务内 {@link NotificationService#publish} 落 outbox 后，
 * 由上层组合调用 dispatcher 做即时推送（WebSocket）或邮件二次触达。
 * 失败重试与聚合由默认实现 {@code AsyncNotificationDispatcher} 负责。
 */
public interface NotificationDispatcher {

    /**
     * 同步分发单条事件（按 event.targetChannel 路由；返回是否成功）。
     *
     * <p>同步模式不进入重试队列，失败立即抛 {@code NotificationDispatchException}，
     * 适用"操作内即时反馈"场景（如审批操作内立刻推送 WebSocket）。
     *
     * @param event 通知事件（targetChannel 必填；INBOX 视为落库成功）
     * @return true=发送成功 / false=业务侧接受但通道暂不可达
     * @throws NotificationDispatchException 系统异常时抛出
     */
    boolean dispatch(NotificationEvent event);

    /**
     * 异步分发单条事件（进入 Redisson 延迟队列 + 失败重试 + 聚合）。
     *
     * <p>异步模式失败后由 dispatcher 内部按指数退避重试（默认 3 次），
     * 终态为 DEAD 进入死信（运维可见）。适用"业务事务外触达"场景
     * （如定时提醒、外部邮件）。
     *
     * @param event 通知事件
     * @return true=入队成功（不代表已发送）
     */
    boolean dispatchAsync(NotificationEvent event);

    /**
     * 通道不可达异常（dispatch 同步调用时抛出；dispatchAsync 转为退避重试）。
     */
    class NotificationDispatchException extends RuntimeException {
        public NotificationDispatchException(String message) {
            super(message);
        }

        public NotificationDispatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
