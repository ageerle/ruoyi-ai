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
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(deletionRequestService.submit(
            body.entityType(), body.entityId(), body.snapshot(), body.reason(), actor.id()));
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
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        return ApiV1Response.ok(deletionRequestService.leaderDecision(id, actor.id(), approve, opinion));
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
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(deletionRequestService.adminDecision(id, actor.id(), approve, opinion));
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

    /** 提交删除申请入参。 */
    public record SubmitReq(String entityType, Long entityId, String reason, String snapshot) {
    }
}
