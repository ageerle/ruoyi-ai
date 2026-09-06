package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateElementResultService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P2-5.3 条件遗留逾期扫描（独立于 /gates/{gateId} 前缀的全局端点）。
 *
 * <p>POST /api/v1/gates/legacy/scan-overdue —— 扫全库 OPEN 且期限已过的遗留项，
 * 逐项通知责任人（AC-GATE-17 逾期提醒，publishDaily 每日去重）。超管手工触发与
 * ops 定时任务（OPS 卡）共用同一 service 入口。
 */
@RestController
@RequiredArgsConstructor
public class LegacyScanController {

    private final GateElementResultService service;
    private final IpdPermission permission;

    @PostMapping("/api/v1/gates/legacy/scan-overdue")
    public ApiV1Response<Map<String, Object>> scanOverdue() {
        IpdActor actor = permission.requireAdmin();
        return ApiV1Response.ok(Map.of("overdueCount", service.scanOverdue(actor)));
    }
}
