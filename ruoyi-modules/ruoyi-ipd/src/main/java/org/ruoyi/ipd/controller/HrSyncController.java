package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.HrSyncService;
import org.ruoyi.ipd.service.PersonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HR 同步事件接入端点（P2-2.2；AC-AUTH-06/AC-HAND-01）。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code POST /api/v1/hr-sync/mark-resigned} HR 系统上报离职 → 联动冻结 + 撤销会话 + 企微解绑 + 通知</li>
 *   <li>{@code POST /api/v1/hr-sync/escalate-stale-resignations} 管理员手动触发 15 日倒计时升级</li>
 *   <li>{@code GET /api/v1/hr-sync/pending-handovers} 列出当前所有 FROZEN_PENDING_HANDOVER 人员</li>
 * </ul>
 *
 * <p>权限：mark-resigned + escalate 限 SUPER_ADMIN；pending-handovers 限 GROUP_LEADER + SUPER_ADMIN。
 */
@RestController
@RequestMapping("/api/v1/hr-sync")
@RequiredArgsConstructor
public class HrSyncController {

    private final HrSyncService hrSyncService;
    private final IpdPermission permission;

    public record MarkResignedRequest(@NotNull Long personId, @NotBlank String reason) { }

    public record ResignView(boolean idempotent, long pendingProjects, String message,
                             boolean wecomUnbound, boolean sessionsRevoked, int notificationsSent) {
        public static ResignView from(PersonService.ResignResult r) {
            return new ResignView(r.idempotent(), r.pendingProjects(), r.message(),
                r.wecomUnbound(), r.sessionsRevoked(), r.notificationsSent());
        }
    }

    public record PendingHandoverView(Long personId, String name, String employeeNo, Long groupId,
                                      String frozenSince, long activeProjects, long ageDays,
                                      boolean escalate) {
        public static PendingHandoverView from(HrSyncService.PendingHandover p) {
            return new PendingHandoverView(p.personId(), p.name(), p.employeeNo(), p.groupId(),
                p.frozenSince() == null ? null : p.frozenSince().toString(),
                p.activeProjects(), p.ageDays(), p.escalate());
        }
    }

    public record EscalateResponse(int escalated, int thresholdDays) { }

    /**
     * HR 系统上报离职 → PersonService.resign 完整联动链路（AC-AUTH-06）。
     * <p>限 SUPER_ADMIN（HR 系统直推，超管收口）；幂等：重复上报返 idempotent=true。
     */
    @PostMapping("/mark-resigned")
    public ApiV1Response<ResignView> markResigned(@Valid @RequestBody MarkResignedRequest req) {
        IpdActor operator = permission.requireAdmin();
        PersonService.ResignResult result = hrSyncService.markResignedByHr(
            req.personId(), req.reason(), operator);
        return ApiV1Response.ok(ResignView.from(result));
    }

    /**
     * 管理员手动触发 15 日倒计时升级（AC-HAND-01 末段）。生产由 cron 每日 09:00 触发。
     */
    @PostMapping("/escalate-stale-resignations")
    public ApiV1Response<EscalateResponse> escalateStaleResignations(
            @RequestParam(value = "thresholdDays", required = false, defaultValue = "15") int thresholdDays) {
        IpdActor operator = permission.requireAdmin();
        int escalated = hrSyncService.escalateStaleResignations(thresholdDays, operator);
        return ApiV1Response.ok(new EscalateResponse(escalated, thresholdDays));
    }

    /**
     * 列出当前所有 FROZEN_PENDING_HANDOVER 人员（前端待移交收件箱 + 管理员视图共用）。
     */
    @GetMapping("/pending-handovers")
    public ApiV1Response<List<PendingHandoverView>> listPendingHandovers(
            @RequestParam(value = "thresholdDays", required = false, defaultValue = "15") int thresholdDays) {
        permission.requireLeaderOrAdmin();
        return ApiV1Response.ok(hrSyncService.listPendingHandoverEscalations(thresholdDays).stream()
            .map(PendingHandoverView::from).toList());
    }
}
