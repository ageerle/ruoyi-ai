package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.SharedKpiCollectReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.KpiSharedCollectionService;
import org.ruoyi.ipd.vo.SharedKpiCollectView;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** K01-K04 共担 KPI 归集入口（P3-1.2）。 */
@RestController
@RequestMapping("/api/v1/kpi/shared")
@RequiredArgsConstructor
@Validated
public class SharedKpiController {

    private final IpdPermission permission;
    private final KpiSharedCollectionService service;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<SharedKpiCollectView> collect(@Valid @RequestBody SharedKpiCollectReq request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(service.collectSharedKpi(actor, request));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/deadline-scan")
    public ApiV1Response<KpiSharedCollectionService.DeadlineScanResult> scanDeadlines(
        @RequestParam
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "period 必须为 YYYY-MM")
        String period) {
        permission.requireAdmin();
        return ApiV1Response.ok(service.scanMonthlyDeadlines(
            java.time.LocalDate.now(), java.time.YearMonth.parse(period)));
    }
}
