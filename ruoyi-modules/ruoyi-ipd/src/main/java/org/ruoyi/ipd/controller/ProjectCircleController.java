package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProjectCircleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 项目协作圈（P0 域#35；对齐 ZK-IPD 原型 /api/project-circle/*）。
 * 页：项目空间-协作圈 Tab；成员/动态/评论/候选人的唯一数据源。
 */
@RestController
@RequestMapping("/api/v1/project-circle")
@RequiredArgsConstructor
public class ProjectCircleController {

    private final IpdPermission ipdPermission;
    private final ProjectCircleService projectCircleService;

    /** 协作圈视图：项目 + 成员 + 动态（含评论）+ canManage + readOnly。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}")
    public ApiV1Response<Map<String, Object>> view(@PathVariable Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectCircleService.view(projectId, actor));
    }

    /** 可加协作人候选（仅管理人）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}/candidates")
    public ApiV1Response<Map<String, Object>> candidates(@PathVariable Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(Map.of("candidates", projectCircleService.candidates(projectId, actor)));
    }

    /** 增加协作人（幂等 upsert 圈角色 + 通知被加人）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/members")
    public ApiV1Response<Map<String, Object>> addMember(@PathVariable Long projectId,
                                                        @RequestBody AddMemberRequest request) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectCircleService.addMember(
            projectId, actor, request.userId(), request.circleRole()));
    }

    /** 发动态（2-3000 字；可关联业务对象）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/posts")
    public ApiV1Response<Map<String, Object>> createPost(@PathVariable Long projectId,
                                                         @RequestBody CreatePostRequest request) {
        IpdActor actor = ipdPermission.requireInternal();
        Long id = projectCircleService.createPost(
            projectId, actor, request.content(), request.objectType(), request.objectId());
        return ApiV1Response.ok(Map.of("id", id));
    }

    /** 评论（2-2000 字；楼中楼 parentId 可选；通知动态作者）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/posts/{id}/comments")
    public ApiV1Response<Map<String, Object>> comment(@PathVariable Long id,
                                                      @RequestBody CommentRequest request) {
        IpdActor actor = ipdPermission.requireInternal();
        Long commentId = projectCircleService.addComment(id, actor, request.content(), request.parentId());
        return ApiV1Response.ok(Map.of("id", commentId));
    }

    /** 候选列表透传（供页面无 canManage 时置空）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}/members")
    public ApiV1Response<List<Map<String, Object>>> members(@PathVariable Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) projectCircleService.view(projectId, actor).get("members");
        return ApiV1Response.ok(members);
    }

    public record AddMemberRequest(@NotNull Long userId, String circleRole) { }

    public record CreatePostRequest(@NotBlank String content, String objectType, Long objectId) { }

    public record CommentRequest(@NotBlank String content, Long parentId) { }
}
