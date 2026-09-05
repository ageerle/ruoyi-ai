package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.DeletionArchiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P0-6.4 归档区控制器（仅超管）。
 * SEC-API-01：入口再调 requireAdmin，与 Service 双保险；审计 operator 来自 IPD 会话。
 */
@RestController
@RequestMapping("/api/v1/deletion-requests")
@RequiredArgsConstructor
public class DeletionRequestController {

    private final DeletionArchiveService archiveService;
    private final IpdPermission ipdPermission;

    /**
     * 归档区列表，需 ipd:deletion-request:archive 权限。
     *
     * @return 已删除且未清除的删除请求
     */
    @SaCheckPermission(value = "ipd:deletion-request:archive", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/archive")
    public ApiV1Response<List<DeletionRequest>> archive() {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(archiveService.listArchive());
    }

    /**
     * 超管二次确认清除，需 ipd:deletion-request:purge 权限。
     *
     * @param id 删除请求 ID
     * @return 清除后的请求记录
     */
    @SaCheckPermission(value = "ipd:deletion-request:purge", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/purge")
    public ApiV1Response<DeletionRequest> purge(@PathVariable Long id) {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(archiveService.purge(id));
    }
}
