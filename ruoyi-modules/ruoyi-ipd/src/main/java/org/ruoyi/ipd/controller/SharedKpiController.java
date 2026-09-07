package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.dto.SharedKpiCollectReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.KpiSharedCollectionService;
import org.ruoyi.ipd.vo.SharedKpiCollectView;
import org.ruoyi.ipd.service.KpiSharedCollectionService.DeadlineConfigView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * K01-K04 共担 KPI 归集入口（P3-1.2）+ 共担归集列表读端点（W4-E）。
 *
 * <p>3 端点：
 * <ul>
 *   <li>{@code POST /api/v1/kpi/shared} — 归集录入（产品组长）</li>
 *   <li>{@code POST /api/v1/kpi/shared/deadline-scan} — 月度逾期扫描（仅超管）</li>
 *   <li>{@code GET  /api/v1/kpi/shared?projectId&period} — 按项目+周期读取共担归集记录列表（W4-E 补交付，前端页 30 共担 KPI 阻塞修复）</li>
 * </ul>
 *
 * <p>权限梯度：
 * <ul>
 *   <li>collect / list → {@code ipd:kpi:query}（MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 可见）</li>
 *   <li>deadline-scan → 仅 SUPER_ADMIN</li>
 * </ul>
 */
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

    /**
     * W4-E §1：按 projectId + period 列出当期全部共担 KPI 归集记录。
     *
     * <p>前端页 30 共担 KPI 阻塞修复——原前端 kpi.ts 仅含 functional/performance/trend 三个无 shared 模块，
     * 后端原本只有 POST 写入与 POST 扫描，无 GET 读端点，前端调必 404。
     * <p>实现：service.listSharedKpis 按 projectId+period+kpiType=SHARED 查询，
     * 返回 List&lt;KpiRecord&gt;（含双 PM 各一条 + 历次 revision），按 revision DESC 排序保证最新版本在前。
     * <p>不写审计、不变更状态，纯查询。
     *
     * @param projectId 项目主键（必填）
     * @param period YYYY-MM（必填）
     * @return KpiRecord 列表（可能为空但不会为 null）
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<KpiRecord>> listShared(
        @RequestParam @NotNull Long projectId,
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period) {
        // 兜底二次校验：注解限四角色，service 兜底与 requireInternal 同严
        // W4-Security IDOR 修复：捕获 actor 传给 service 做 project 级鉴权（件 1.6）
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(service.listSharedKpis(actor, projectId, period));
    }

    /**
     * HIGH-4.1：月度截止日配置视图（前端可读、运维可观察）。
     * <p>返回：{@code dayOfMonth} + {@code cutoffTime} + {@code version} + {@code source} +
     * {@code configuredValue}；source ∈ {FACTORY_DEFAULT, DB_ACTIVE, DB_INACTIVE}。
     * <p>权限梯度：MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 可见（与 listShared 一致）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/deadline-config")
    public ApiV1Response<DeadlineConfigView> getDeadlineConfig() {
        permission.requireInternal();
        return ApiV1Response.ok(service.getDeadlineConfig());
    }
}
