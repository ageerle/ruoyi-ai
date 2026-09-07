package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.DeletionArchiveService;
import org.ruoyi.ipd.service.DeletionRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 删除审核 + 归档区（P0-6.x）。
 * SEC-API-01：写操作 operator 仅从会话推导。
 * P0-6.2：超管通过走 DeleteAuditService 原子软删。
 */
@RestController
@RequestMapping("/api/v1/deletion-requests")
@RequiredArgsConstructor
public class DeletionRequestController {

    private final DeletionArchiveService archiveService;
    private final DeletionRequestService deletionRequestService;
    private final IpdPermission ipdPermission;

    /**
     * 提交删除申请（进组长初审）。
     *
     * @param body entityType/entityId/reason/snapshot
     * @return 新建申请
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<DeletionRequest> submit(@RequestBody SubmitReq body) {
        // W5-E-2.2：actor 传入 service，service 层做 UNAUTHORIZED + 资源归属校验（IDOR 修复）
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(deletionRequestService.submit(
            actor, body.entityType(), body.entityId(), body.snapshot(), body.reason()));
    }

    /**
     * 组长初审。
     *
     * @param id      申请 ID
     * @param approve true=通过进超管；false=驳回
     * @param opinion 意见
     * @return 更新后申请
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_LEADER, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/leader-decision")
    public ApiV1Response<DeletionRequest> leaderDecision(@PathVariable Long id,
                                                         @RequestParam boolean approve,
                                                         @RequestParam(required = false) String opinion) {
        // W5-E-2.2：actor 传入 service，service 层做组长角色 + 目标所属组匹配校验（防冒充组长）
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        return ApiV1Response.ok(deletionRequestService.leaderDecision(actor, id, approve, opinion));
    }

    /**
     * 超管终审；通过时原子软删目标（AC-DEL-02）。
     *
     * @param id      申请 ID
     * @param approve true=通过并软删；false=驳回
     * @param opinion 意见
     * @return 终态申请
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_ADMIN, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/admin-decision")
    public ApiV1Response<DeletionRequest> adminDecision(@PathVariable Long id,
                                                        @RequestParam boolean approve,
                                                        @RequestParam(required = false) String opinion) {
        // W5-E-2.2：actor 传入 service，service 层做 SUPER_ADMIN 硬校验（防冒充超管审批触发软删）
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(deletionRequestService.adminDecision(actor, id, approve, opinion));
    }

    /**
     * 归档区列表，需 archive 权限。
     *
     * @return 已删除且未清除的删除请求
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_ARCHIVE, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/archive")
    public ApiV1Response<List<DeletionRequest>> archive() {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(archiveService.listArchive());
    }

    /**
     * 超管二次确认清除，需 purge 权限。
     *
     * @param id 删除请求 ID
     * @return 清除后的请求记录
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/purge")
    public ApiV1Response<DeletionRequest> purge(@PathVariable Long id) {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(archiveService.purge(id));
    }

    /**
     * 申请人撤回：限申请人本人在 24h 内、未终态；超 24h 不可撤回（AC-DEL-06）。
     * 权限用 SUBMIT 码 + actor.id 必须等于 requesterId（应用层兜底防越权自审）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/withdraw")
    public ApiV1Response<DeletionRequest> withdraw(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(deletionRequestService.withdraw(id, actor.id()));
    }

    /**
     * 组长逾期升级：扫 LEADER_REVIEW 状态 + leaderDueAt 已过 → 转 ADMIN_REVIEW（AC-DEL-07）。
     * 超管专属（影响全库删申请，保守授权）；幂等可重复执行（已升级者不再被扫中）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_ADMIN, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/escalate-overdue")
    public ApiV1Response<java.util.Map<String, Object>> escalateOverdue() {
        ipdPermission.requireAdmin();
        int escalated = deletionRequestService.escalateOverdueLeaderReview();
        return ApiV1Response.ok(java.util.Map.of("escalated", escalated));
    }

    /**
     * 超管逾期清单：ADMIN_REVIEW 状态 + adminDueAt 已过（仅查询，不自动通过；AC-DEL-07）。
     * 超管专属。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_ADMIN, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/overdue-admin-review")
    public ApiV1Response<java.util.List<DeletionRequest>> listOverdueAdminReview() {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(deletionRequestService.listOverdueAdminReview());
    }

    /** 提交删除申请入参。 */
    public record SubmitReq(String entityType, Long entityId, String reason, String snapshot) {
    }
}
