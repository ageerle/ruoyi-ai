package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.WorkbenchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 我的工作台聚合（P4-3.1 前置真实现；对齐 ZK-IPD 原型 /api/workflow/tasks + workspace 概览）。
 * 页03：metric 四卡 / 责任任务队列 / 我的当前推进 / 删除审批待办数的唯一数据源。
 */
@RestController
@RequestMapping("/api/v1/workbench")
@RequiredArgsConstructor
public class WorkbenchController {

    private final IpdPermission ipdPermission;
    private final WorkbenchService workbenchService;

    /**
     * 工作台总览（真实聚合，无 mock）。
     *
     * @param projectId 可选当前项目（顶栏切换后续传；空 = 第一个可见项目）
     * @return stats(pending/overdue/unread/completed) + tasks + deletionPending + currentAdvance
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/summary")
    public ApiV1Response<Map<String, Object>> summary(@RequestParam(required = false) Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(workbenchService.summary(actor, projectId));
    }
}
