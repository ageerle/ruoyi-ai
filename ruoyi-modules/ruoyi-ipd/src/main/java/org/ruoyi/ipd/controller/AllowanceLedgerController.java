package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.AllowanceLedgerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 月度津贴台账查询 Controller（W4-D，前端 allowance.ts 配套）
 *
 * <p>3 端点（补 AllowanceLedgerService.list/pendingStop/autoScan 后端零 Controller 缺失）：
 * <ul>
 *   <li>{@code GET  /api/v1/allowance/ledger} — 月度津贴快照列表，权限 ipd:kpi:query</li>
 *   <li>{@code GET  /api/v1/allowance/pending-stop} — 待停发津贴列表，权限 ipd:kpi:query</li>
 *   <li>{@code POST /api/v1/allowance/auto-scan} — 月度自动扫描（仅超管，service.requireAdmin 兜底）</li>
 * </ul>
 *
 * <p>W5-E-2.3 P0 #3 IDOR 修复：3 端点全部捕获 {@code requireInternal()/requireAdmin()} 返回的
 * {@link IpdActor} 传入 service；AllowanceLedgerService.list/pendingStop/autoScan 自此有 service 层
 * actor 守卫（UNAUTHORIZED / SUPER_ADMIN），不再仅依赖 Controller 决定。
 *
 * <p>权限梯度：
 * <ul>
 *   <li>list / pending-stop → {@code ipd:kpi:query}（MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 可见，
 *       与 KpiRecordController 同款，沿用 IpdPermissionCode 现成常量避免新增未登记的权限码）</li>
 *   <li>auto-scan → 仅 SUPER_ADMIN（{@code ipdPermission.requireAdmin()}，不挂注解防 OPS-09 漂移）</li>
 * </ul>
 *
 * <p>DTO 字段对齐遗留：当前 Controller 返回 {@link AllowanceLedger} 原样；前端 {@code allowance.ts} type 的
 * period / level / amount / capReached / status 与后端 domain 字段名差异留作 W4-D' 单独对齐任务。
 *
 * <p>W3-A3/A8 探针实证：前端 allowance.ts L27/L32 {@code ipdGet('/allowance/ledger', ...)} +
 * {@code ipdGet('/allowance/pending-stop', ...)} 调用必 404——本 Controller 修复此 404。
 */
@RestController
@RequestMapping("/api/v1/allowance")
@RequiredArgsConstructor
@Validated
public class AllowanceLedgerController {

    private final IpdPermission ipdPermission;
    private final AllowanceLedgerService service;

    /**
     * W4-D §1：月度津贴快照列表（按 period 必填 + 可选 personId 过滤）。
     *
     * @param period  YYYY-MM（必填，违反格式抛 IpdBusinessException）
     * @param personId 可选；为 null 时返回该月全员记录
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/ledger")
    public ApiV1Response<List<AllowanceLedger>> listLedger(
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period,
        @RequestParam(required = false) Long personId) {
        // 兜底二次校验：注解限四角色，service 兜底与 requireInternal 同严
        // W5-E-2.3：捕获 actor 传入 service，service 层再做 UNAUTHORIZED 入口校验（IDOR 修复）
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(service.list(actor, period, personId));
    }

    /**
     * W4-D §2：待停发津贴列表（按 period 过滤；stopReason 非空）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_KPI_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/pending-stop")
    public ApiV1Response<List<AllowanceLedger>> listPendingStop(
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period) {
        // W5-E-2.3：捕获 actor 传入 service（IDOR 修复）
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(service.pendingStop(actor, period));
    }

    /**
     * W4-D §3：月度自动扫描（仅 SUPER_ADMIN）。
     *
     * <p>返回当月 AllowanceLedger 行数；后续扫描逻辑（绩效分 <60 / 60 天无产出判定）由 AllowanceService.determineStopReasonP332 接入。
     */
    @PostMapping("/auto-scan")
    public ApiV1Response<Integer> autoScan(
        @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "period 必须为 YYYY-MM") String period) {
        // 兜底与注解同严：仅 SUPER_ADMIN 可触发扫描（W5-E-2.3：actor 传入 service，service 层同严兜底）
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(service.autoScan(actor, period));
    }
}