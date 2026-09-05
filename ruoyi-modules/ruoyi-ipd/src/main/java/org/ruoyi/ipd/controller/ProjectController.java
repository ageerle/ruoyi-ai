package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目接口 /api/v1/projects（TS-09 统一响应 code=0）。
 * SEC-API-01：写操作 operator 仅从会话推导，禁止客户端传 operatorId。
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final IpdPermission ipdPermission;

    /**
     * 查询项目列表（带可选关键字过滤）。
     *
     * @param keyword 可选关键字
     * @return 项目列表
     */
    @GetMapping
    @SaCheckPermission(IpdPermissionCode.OPERATION_MODULE_PROJECT)
    public ApiV1Response<List<Project>> list(@RequestParam(required = false) String keyword) {
        return ApiV1Response.ok(projectService.list(keyword));
    }

    /**
     * 查询项目详情。
     *
     * @param id 项目 ID
     * @return 项目实体
     */
    @GetMapping("/{id}")
    @SaCheckPermission(IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY)
    public ApiV1Response<Project> get(@PathVariable Long id) {
        return ApiV1Response.ok(projectService.getById(id));
    }

    /**
     * 创建项目。CODE-01：白名单 DTO，code/currentStage/status/source 由服务端定。
     *
     * @param req 创建请求
     * @return 新建项目
     */
    @PostMapping
    @SaCheckPermission(IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)
    public ApiV1Response<Project> create(@RequestBody org.ruoyi.ipd.dto.ProjectCreateReq req) {
        IpdActor actor = ipdPermission.requireProjectCreator();
        return ApiV1Response.ok(projectService.create(req.toEntity(), actor.id()));
    }

    /**
     * 变更项目状态。
     *
     * @param id     项目 ID
     * @param target 目标状态
     * @return 更新后项目
     */
    @PostMapping("/{id}/status")
    @SaCheckPermission(IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE)
    public ApiV1Response<Project> changeStatus(@PathVariable Long id, @RequestParam String target) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.changeStatus(id, target, actor.id()));
    }

    /**
     * 推进项目阶段。
     *
     * @param id 项目 ID
     * @return 更新后项目
     */
    @PostMapping("/{id}/advance-stage")
    @SaCheckPermission(IpdPermissionCode.OPERATION_MODULE_PROJECT_ADVANCE_STAGE)
    public ApiV1Response<Project> advanceStage(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.advanceStage(id, actor.id()));
    }
}
