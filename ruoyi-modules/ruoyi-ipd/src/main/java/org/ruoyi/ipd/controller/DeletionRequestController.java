package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.service.DeletionArchiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P0-6.4 归档区控制器（仅超管可访问）。
 * - GET /api/v1/deletion-requests/archive：归档区列表（已 DELETED + 未 PURGED）
 * - POST /api/v1/deletion-requests/{id}/purge：超管二次确认清除
 * 身份校验由 DeletionArchiveService.requireSuperAdmin 通过 Sa-Token + PersonMapper 完成。
 */
@RestController
@RequestMapping("/api/v1/deletion-requests")
@RequiredArgsConstructor
public class DeletionRequestController {

    private final DeletionArchiveService archiveService;

    @GetMapping("/archive")
    public ApiV1Response<List<DeletionRequest>> archive() {
        return ApiV1Response.ok(archiveService.listArchive());
    }

    @PostMapping("/{id}/purge")
    public ApiV1Response<DeletionRequest> purge(@PathVariable Long id) {
        return ApiV1Response.ok(archiveService.purge(id));
    }
}