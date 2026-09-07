package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.AsyncNotificationDispatcher;
import org.ruoyi.ipd.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 站内通知与可执行待办 API /api/v1/notifications（OPS-05；页03 站内信）。
 * receiver 恒从会话推导（SEC-API-01：不接受请求体透传接收者）；
 * 发布无 HTTP 入口——事件由各业务服务事务内 publish，防越权伪造通知。
 * dispatch-pending 为消费端运维触发（正常轮询待 OPS-04 scheduler 合入后接线）。
 * ROOT-R2-P0-2 新增 async-dispatch：异步分发器手动触发（Redisson 延迟队列消费）。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AsyncNotificationDispatcher asyncDispatcher;
    private final IpdPermission ipdPermission;

    /**
     * 本人收件箱。
     *
     * @param unreadOnly true=仅未读
     * @return 事件列表（新→旧）
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<NotificationEvent>> inbox(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(notificationService.inbox(actor.id(), unreadOnly));
    }

    /** 未读计数（红点）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/unread-count")
    public ApiV1Response<Map<String, Long>> unreadCount() {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(Map.of("count", notificationService.unreadCount(actor.id())));
    }

    /**
     * 标记单条已读（仅本人事件；他人 ID 按 NOT_FOUND 拒绝，杜绝探测）。
     *
     * @param id 事件 ID
     * @return 更新后事件
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_READ, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/read")
    public ApiV1Response<NotificationEvent> markRead(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(notificationService.markRead(id, actor.id()));
    }

    /** 全部已读。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_READ, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/read-all")
    public ApiV1Response<Map<String, Integer>> markAllRead() {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(Map.of("updated", notificationService.markAllRead(actor.id())));
    }

    /**
     * 手动触发 outbox 消费（仅超管，运维观察用）。
     *
     * @param limit 单轮上限（默认 50，最大 200）
     * @return sent/failed/dead/skipped 计数
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_DISPATCH, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/dispatch-pending")
    public ApiV1Response<Map<String, Integer>> dispatchPending(@RequestParam(defaultValue = "50") int limit) {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(notificationService.dispatchPending(limit));
    }

    /**
     * ROOT-R2-P0-2：手动触发异步分发器消费（仅超管；正常轮询待 scheduler 合入）。
     * 消费 Redisson 延迟队列事件，按 targetChannel 路由 handler；失败 3 次转 DEAD。
     *
     * @param maxProcess 单轮上限（默认 50，最大 200）
     * @return sent/failed/dead/skipped/aggregated 计数
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_NOTIFICATION_DISPATCH, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/async-dispatch")
    public ApiV1Response<Map<String, Integer>> asyncDispatch(@RequestParam(defaultValue = "50") int maxProcess) {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(asyncDispatcher.consumeOnce(maxProcess));
    }
}
