package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.ProjectScoreSubmitReq;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.ProjectScoreArchiveService;
import org.ruoyi.ipd.vo.ProjectScoreView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 项目绩效 20/40/40 独立提交、归档与结算接口（P3-2.2）。 */
@RestController
@RequestMapping("/api/v1/project-scores")
@RequiredArgsConstructor
public class ProjectScoreController {

    private final ProjectScoreArchiveService archiveService;
    private final org.ruoyi.ipd.security.IpdPermission permission;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<ProjectScoreView> submit(@Valid @RequestBody ProjectScoreSubmitReq request) {
        permission.requireInternal();
        return ApiV1Response.ok(archiveService.submit(request));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}/{personId}")
    public ApiV1Response<ProjectScoreView> view(
        @PathVariable Long projectId, @PathVariable Long personId) {
        permission.requireInternal();
        return ApiV1Response.ok(archiveService.view(projectId, personId));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}/{personId}/settle")
    public ApiV1Response<ProjectScoreView> settle(
        @PathVariable Long projectId, @PathVariable Long personId) {
        permission.requireInternal();
        return ApiV1Response.ok(archiveService.settleVersion(projectId, personId));
    }
}
