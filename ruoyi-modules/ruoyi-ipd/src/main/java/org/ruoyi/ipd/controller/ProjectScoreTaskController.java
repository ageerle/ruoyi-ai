package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.ProjectScoreScheduleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 项目绩效上市 30/90 日待办扫描入口（P3-2.3）。 */
@RestController
@RequestMapping("/api/v1/project-score-tasks")
@RequiredArgsConstructor
public class ProjectScoreTaskController {

    private final ProjectScoreScheduleService scheduleService;
    private final IpdPermission permission;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/scan")
    public ApiV1Response<ProjectScoreScheduleService.ScheduleScanResult> scan() {
        permission.requireAdmin();
        return ApiV1Response.ok(scheduleService.scanLaunchedProjects(java.time.LocalDate.now()));
    }
}
