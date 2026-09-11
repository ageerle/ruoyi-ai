package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.ProjectScoreScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 我的在途评分待办（2026-09-08 前端契约对照轮补交）：
     * 我的 SELF_SCORING 自评 + 我任组长的成员的 LEADER_REVIEW 评审，均限 PENDING。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/my")
    public ApiV1Response<List<ProjectScoreScheduleService.MyScoreTaskView>> myTasks() {
        return ApiV1Response.ok(scheduleService.myTasks(permission.requireInternal()));
    }
}
