package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.KpiRecordService;
import org.ruoyi.ipd.service.KpiRecordService.KpiSourceItem;
import org.ruoyi.ipd.service.KpiRecordService.TrendPoint;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * KPI 考核 Controller（P3-1.1 / P3-1.2 / P3-1.3）
 *
 * <p>3 端点：
 * <ul>
 *   <li>{@code GET /api/v1/kpi/functional} — P3-1.1 功能 KPI 指标来源与计算（P0-10.29）</li>
 *   <li>{@code GET /api/v1/kpi/performance} — P3-1.2 绩效 KPI 聚合（P0-10.30）</li>
 *   <li>{@code GET /api/v1/kpi/trend} — P3-1.3 历史 KPI 趋势（P0-10.32）</li>
 * </ul>
 *
 * <p>权限：统一 {@code ipd:kpi:query}（MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 可见）。
 */
@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
@Validated
public class KpiRecordController {

    private final IpdPermission ipdPermission;
    private final KpiRecordService kpiRecordService;

    /**
     * P3-1.1 功能 KPI 指标来源（前端 P0-10.29）。
     *
     * @param period YYYY-MM
     */
    @SaCheckPermission(value = "ipd:kpi:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/functional")
    public ApiV1Response<List<KpiSourceItem>> functional(
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period
    ) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(kpiRecordService.calculateFunctionalKpi(actor, period));
    }

    /**
     * P3-1.2 绩效 KPI 聚合（前端 P0-10.30）。
     *
     * @param period YYYY-MM
     */
    @SaCheckPermission(value = "ipd:kpi:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/performance")
    public ApiV1Response<Map<String, Object>> performance(
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period
    ) {
        IpdActor actor = ipdPermission.requireInternal();
        Map<String, java.math.BigDecimal> raw = kpiRecordService.aggregatePerformanceKpi(actor, period);
        // 转 String 防止前端 BigInt 截断（与 ApiV1Response §BigNumberSerializer 一致）
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        raw.forEach((k, v) -> result.put(k, v.toPlainString()));
        return ApiV1Response.ok(result);
    }

    /**
     * P3-1.3 KPI 历史趋势（前端 P0-10.32 项目详情-KPI 考核）。
     *
     * @param periods 回看月数（1~36，缺省 12）
     */
    @SaCheckPermission(value = "ipd:kpi:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/trend")
    public ApiV1Response<List<TrendPoint>> trend(
        @RequestParam(required = false, defaultValue = "12") int periods
    ) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(kpiRecordService.getHistoricalTrend(actor, periods));
    }
}
