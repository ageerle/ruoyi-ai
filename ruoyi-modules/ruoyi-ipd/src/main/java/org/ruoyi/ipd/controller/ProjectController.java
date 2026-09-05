package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目接口 /api/v1/projects（TS-09 统一响应 code=0）
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final IpdPermission ipdPermission;

    /** 查询项目列表，需 ipd:project:list 权限 */
    @GetMapping
    @SaCheckPermission(value = "ipd:project:list", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<Project>> list(@RequestParam(required = false) String keyword) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.list(keyword));
    }

    /** 查询项目详情，需 ipd:project:query 权限 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = "ipd:project:query", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.getById(id));
    }

    /** 创建项目，需 ipd:project:add 权限 */
    @PostMapping
    @SaCheckPermission(value = "ipd:project:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> create(@RequestBody org.ruoyi.ipd.dto.ProjectCreateReq req) {
        // CODE-01：白名单 DTO，code/currentStage/status/source 由服务端定，客户端不可注入
        ipdPermission.requireProjectCreator();
        return ApiV1Response.ok(projectService.create(req.toEntity(), LoginHelper.getUserId()));
    }

    /** 变更项目状态，需 ipd:project:edit 权限 */
    @PostMapping("/{id}/status")
    @SaCheckPermission(value = "ipd:project:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> changeStatus(@PathVariable Long id, @RequestParam String target) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.changeStatus(id, target, LoginHelper.getUserId()));
    }

    /** 推进项目阶段，需 ipd:project:edit 权限 */
    @PostMapping("/{id}/advance-stage")
    @SaCheckPermission(value = "ipd:project:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> advanceStage(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.advanceStage(id, LoginHelper.getUserId()));
    }
}
