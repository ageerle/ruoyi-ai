package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.HandoverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P2-7.1 单项目角色移交（/api/v1/handovers；页27）。
 *
 * <p>onBehalf=true 走产品组长/超管代办一键移交（AC-HAND-01c）；普通移交由接手人
 * confirm 完成（{@code POST /{id}/accept}）。
 */
@RestController
@RequestMapping("/api/v1/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;
    private final IpdPermission permission;

    /** 发起请求：approvalRef 仅当接手后达到项目数上限阈值时必填（AC-TEAM-11，服务端强制）。 */
    public record InitiateRequest(@NotNull Long projectId, @NotBlank String role,
                                  @NotNull Long toPersonId, String note,
                                  String approvalRef, Boolean onBehalf) { }

    public record AcceptRequest(String approvalRef) { }

    /** 批量请求：projectIds 为空 ⇒ 原负责人名下该角色全部活跃项目；approvalRef 为本批统一备案号。 */
    public record BatchRequest(@NotNull Long fromPersonId, @NotBlank String role,
                               @NotNull Long toPersonId, String note,
                               String approvalRef, List<Long> projectIds) { }

    public record BatchResultView(Long projectId, String status, String reason) { }

    public record HandoverView(String id, String projectId, String fromPersonId, String toPersonId,
                               String handoverRole, String status, String note,
                               String confirmedAt, String completedAt) {
        public static HandoverView from(HandoverRecord r) {
            return new HandoverView(String.valueOf(r.getId()), String.valueOf(r.getProjectId()),
                String.valueOf(r.getFromPersonId()), String.valueOf(r.getToPersonId()),
                r.getHandoverRole(), r.getStatus(), r.getNote(),
                r.getConfirmedAt() == null ? null : r.getConfirmedAt().toString(),
                r.getCompletedAt() == null ? null : r.getCompletedAt().toString());
        }
    }

    /** 发起移交（onBehalf=true：组长代离职/冻结人员一键移交，发起即完成）。 */
    @PostMapping
    public ApiV1Response<HandoverView> initiate(@Valid @RequestBody InitiateRequest request) {
        boolean onBehalf = Boolean.TRUE.equals(request.onBehalf());
        IpdActor actor = onBehalf ? permission.requireLeaderOrAdmin() : permission.requireInternal();
        HandoverRecord rec = onBehalf
            ? handoverService.initiateOnBehalf(request.projectId(), request.role(),
                request.toPersonId(), request.note(), request.approvalRef(), actor)
            : handoverService.initiate(request.projectId(), request.role(),
                request.toPersonId(), request.note(), actor);
        return ApiV1Response.ok(HandoverView.from(rec));
    }

    /** 批量移交（P2-7.2）：逐项目结果明确；失败保持原归属；重试跳过已成功项；真全清才禁用。 */
    @PostMapping("/batch")
    public ApiV1Response<List<BatchResultView>> batch(@Valid @RequestBody BatchRequest request) {
        IpdActor actor = permission.requireLeaderOrAdmin();
        return ApiV1Response.ok(handoverService.batchHandover(request.fromPersonId(), request.role(),
                request.toPersonId(), request.note(), request.approvalRef(), request.projectIds(), actor)
            .stream().map(r -> new BatchResultView(r.projectId(), r.status(), r.reason())).toList());
    }

    /** 接手人确认接受（DRAFT → COMPLETED，原子转移）。 */
    @PostMapping("/{id}/accept")
    public ApiV1Response<HandoverView> accept(@PathVariable Long id,
                                              @RequestBody(required = false) AcceptRequest request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(HandoverView.from(
            handoverService.accept(id, request == null ? null : request.approvalRef(), actor)));
    }

    /** 收件箱：待我接收 + 我发起的（未完结）。 */
    @GetMapping("/inbox")
    public ApiV1Response<List<HandoverView>> inbox() {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(handoverService.inbox(actor).stream().map(HandoverView::from).toList());
    }

    /** P2-7.3：超管权限移交（AC-HAND-07）。仅超管本人可发起；confirmation 确认短语二次确认（页49 原型：输入「确认移交管理员」）；强制 PERSON_TYPE=SUPER_ADMIN 切换 + 审计 SUPER_ADMIN_TRANSFER；旧会话由 scopeOf→NONE 每请求 401 兕底。 */
    public record SuperAdminTransferRequest(@NotNull Long toPersonId, String note, @NotBlank String confirmation) { }

    @PostMapping("/super-admin")
    public ApiV1Response<Void> transferSuperAdmin(@Valid @RequestBody SuperAdminTransferRequest request) {
        IpdActor actor = permission.requireAdmin();
        handoverService.transferSuperAdmin(request.toPersonId(), request.note(), request.confirmation(), actor);
        return ApiV1Response.ok(null);
    }
}
