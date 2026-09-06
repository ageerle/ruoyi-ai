package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateReviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P2-5.4 签署超时与期限提醒扫描（独立于 /gates/{gateId} 前缀的全局端点）。
 *
 * <ul>
 *   <li>POST /api/v1/gates/sign/scan-timeout —— 双签 Gate 超期未签方自动弃权，
 *       按主导方意见执行并审计（AC-GATE-08；BR-GATE-04 D17）</li>
 *   <li>POST /api/v1/gates/sign/scan-remind —— 签署期限前 1 天提醒未签方
 *       （AC-GATE-09；GATE_SIGN_SOON，publishDaily 每日去重）</li>
 * </ul>
 *
 * <p>超管手工触发与 ops 定时任务（OPS-04 scheduler 接线后）共用同一 service 入口。
 */
@RestController
@RequiredArgsConstructor
public class GateSignScanController {

    private final GateReviewService service;
    private final IpdPermission permission;

    @PostMapping("/api/v1/gates/sign/scan-timeout")
    public ApiV1Response<Map<String, Object>> scanTimeout() {
        IpdActor actor = permission.requireAdmin();
        return ApiV1Response.ok(Map.of("abstainCount", service.scanTimeout(actor)));
    }

    @PostMapping("/api/v1/gates/sign/scan-remind")
    public ApiV1Response<Map<String, Object>> scanRemind() {
        IpdActor actor = permission.requireAdmin();
        return ApiV1Response.ok(Map.of("remindCount", service.scanRemind(actor)));
    }
}
