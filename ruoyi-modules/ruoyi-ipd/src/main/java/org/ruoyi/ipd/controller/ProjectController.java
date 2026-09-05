package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Project;
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
 * 项目接口 /api/v1/projects（TS-09 统一响应 code=0）
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiV1Response<List<Project>> list(@RequestParam(required = false) String keyword) {
        return ApiV1Response.ok(projectService.list(keyword));
    }

    @GetMapping("/{id}")
    public ApiV1Response<Project> get(@PathVariable Long id) {
        return ApiV1Response.ok(projectService.getById(id));
    }

    @PostMapping
    public ApiV1Response<Project> create(@RequestBody Project project, @RequestParam Long operatorId) {
        return ApiV1Response.ok(projectService.create(project, operatorId));
    }

    @PostMapping("/{id}/status")
    public ApiV1Response<Project> changeStatus(@PathVariable Long id, @RequestParam String target, @RequestParam Long operatorId) {
        return ApiV1Response.ok(projectService.changeStatus(id, target, operatorId));
    }

    @PostMapping("/{id}/advance-stage")
    public ApiV1Response<Project> advanceStage(@PathVariable Long id, @RequestParam Long operatorId) {
        return ApiV1Response.ok(projectService.advanceStage(id, operatorId));
    }
}