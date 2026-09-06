package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.service.GateMaterialChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Gate 材料齐套性视图端点（[CONSISTENCY-15] 2026-09-06）。
 * GET /api/v1/gates/{gateId}/materials?projectId= → {total, uploaded, missing, isReady, items[]}
 */
@RestController
@RequestMapping("/api/v1/gates/{gateId}/materials")
@RequiredArgsConstructor
public class GateMaterialController {

    private final GateMaterialChecker gateMaterialChecker;

    @GetMapping
    public ApiV1Response<Map<String, Object>> materials(
            @PathVariable("gateId") Long gateId,
            @RequestParam("projectId") Long projectId) {
        return ApiV1Response.ok(gateMaterialChecker.listMaterialStatus(gateId, projectId));
    }
}
