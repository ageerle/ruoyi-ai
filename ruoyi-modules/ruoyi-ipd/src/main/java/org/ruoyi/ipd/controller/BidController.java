package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.BidInvitationService;
import org.ruoyi.ipd.service.BidResponseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 招标组队 API（P2-3.1 / P2-3.2）
 *
 * <p>端点：
 * <ul>
 *   <li>POST   /api/v1/bid-invitations          创建招标单（市场PM）</li>
 *   <li>GET    /api/v1/bid-invitations           分页查询招标单</li>
 *   <li>GET    /api/v1/bid-invitations/{id}      招标单详情</li>
 *   <li>PUT    /api/v1/bid-invitations/{id}/publish   发布</li>
 *   <li>PUT    /api/v1/bid-invitations/{id}/select    遴选应标</li>
 *   <li>PUT    /api/v1/bid-invitations/{id}/withdraw  撤回（24h内）</li>
 *   <li>PUT    /api/v1/bid-invitations/{id}/close     关闭</li>
 *   <li>GET    /api/v1/bid-invitations/{id}/responses 应标列表</li>
 *   <li>POST   /api/v1/bid-responses             提交应标（研发PM）</li>
 *   <li>PUT    /api/v1/bid-responses/{id}/withdraw  撤回应标</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BidController {

    private final BidInvitationService bidInvitationService;
    private final BidResponseService bidResponseService;
    private final IpdPermission ipdPermission;
    private final IpdAuthSession session;

    // ==================== 招标单 ====================

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/bid-invitations")
    public ApiV1Response<BidInvitation> createInvitation(@RequestBody BidInvitation invitation) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        // 发起人身份服务端权威：供 listResponses 隐私过滤与审计使用（通用填充器取不到 IPD 独立会话）
        invitation.setCreateBy(person.getId());
        return ApiV1Response.ok(bidInvitationService.create(invitation));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/bid-invitations")
    public ApiV1Response<IPage<BidInvitation>> listInvitations(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bidInvitationService.page(pageNo, pageSize, projectId, status));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/bid-invitations/{id}")
    public ApiV1Response<BidInvitation> getInvitation(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bidInvitationService.getById(id));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/publish")
    public ApiV1Response<BidInvitation> publishInvitation(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bidInvitationService.publish(id));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/select")
    public ApiV1Response<BidInvitation> selectResponse(
            @PathVariable Long id,
            @RequestParam Long responseId) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        return ApiV1Response.ok(bidInvitationService.selectResponse(id, responseId, person.getId()));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/withdraw")
    public ApiV1Response<BidInvitation> withdrawInvitation(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bidInvitationService.withdraw(id));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/close")
    public ApiV1Response<BidInvitation> closeInvitation(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(bidInvitationService.close(id));
    }

    /**
     * P2-3.3 AC-TEAM-13：市场PM（招标单发起人）在有效期内修改招标条件
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/modify")
    public ApiV1Response<BidInvitation> modifyInvitation(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") java.util.Date expireAt) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        return ApiV1Response.ok(bidInvitationService.modifyInvitation(id, title, content, expireAt, person.getId()));
    }

    /**
     * P2-3.3 AC-TEAM-09：超管对挂起超 30 日的招标单直接指派
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-invitations/{id}/admin-assign")
    public ApiV1Response<BidInvitation> adminAssign(
            @PathVariable Long id,
            @RequestParam Long targetPersonId) {
        ipdPermission.requireAdmin();
        Person person = session.currentPerson();
        return ApiV1Response.ok(bidInvitationService.adminAssign(id, targetPersonId, person.getId()));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/bid-invitations/{id}/responses")
    public ApiV1Response<List<BidResponse>> listResponses(@PathVariable Long id) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        return ApiV1Response.ok(bidInvitationService.listResponses(id, person.getId()));
    }

    // ==================== 应标 ====================

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/bid-responses")
    public ApiV1Response<BidResponse> submitResponse(@RequestBody BidResponse response) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        // decision=reject 不留痕：BR-TEAM-03 以 code=0 + data=null 表达 204 语义
        return ApiV1Response.ok(bidResponseService.submit(response, person.getId()));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/bid-responses/{id}/withdraw")
    public ApiV1Response<BidResponse> withdrawResponse(@PathVariable Long id) {
        ipdPermission.requireInternal();
        Person person = session.currentPerson();
        return ApiV1Response.ok(bidResponseService.withdraw(id, person.getId()));
    }
}
