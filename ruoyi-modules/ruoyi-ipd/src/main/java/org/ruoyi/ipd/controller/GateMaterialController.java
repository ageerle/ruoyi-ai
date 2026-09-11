package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.GateMaterialChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Gate 材料齐套性视图端点（[CONSISTENCY-15] + [SEC-FIX] 2026-09-06）。
 *
 * <p>授权：复用 {@code ipd:gate-review:list} 权限码（与 GateReviewController 同源），
 * 同时需满足 {@code projectId} 与 {@code gateId} 关联校验（防越权——避免构造
 * 合法 gateId + 别人 projectId 拉别人材料统计）。
 *
 * <p>GET /api/v1/gates/{gateId}/materials?projectId= → {total, uploaded, missing, isReady, items[]}
 */
@RestController
@RequestMapping("/api/v1/gates/{gateId}/materials")
@RequiredArgsConstructor
public class GateMaterialController {

    private final GateMaterialChecker gateMaterialChecker;
    private final GateMapper gateMapper;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_GATE_REVIEW, type = "ipd")
    @GetMapping
    public ApiV1Response<Map<String, Object>> materials(
            @PathVariable("gateId") Long gateId,
            @RequestParam("projectId") Long projectId) {
        // 关联校验：URL 提供的 gateId 必须属于请求中的 projectId——防越权 IDOR
        Gate gate = gateMapper.selectById(gateId);
        if (gate == null || !projectId.equals(gate.getProjectId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "Gate 不存在或不属于该项目");
        }
        return ApiV1Response.ok(gateMaterialChecker.listMaterialStatus(gateId, projectId));
    }
}
