package org.ruoyi.ipd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.ReportExportResult;
import org.ruoyi.ipd.dto.ReportSummaryRow;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.IpdReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P4-4.1 项目组织绩效汇总与导出 API（AC-INC-34 / BR-INC-15）
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET /api/v1/report/project-summary} — 项目绩效汇总列表</li>
 *   <li>{@code GET /api/v1/report/export/allowance} — 津贴台账导出</li>
 *   <li>{@code GET /api/v1/report/export/bonus} — 奖金台账导出</li>
 *   <li>{@code GET /api/v1/report/export/project} — 项目汇总导出</li>
 * </ul>
 *
 * <p>权限梯度：
 * <ul>
 *   <li>列表 / 津贴导出 / 项目汇总导出 → 内部全员（service 二次校验 actor 范围）</li>
 *   <li>奖金台账导出 → GROUP_LEADER + SUPER_ADMIN（资金敏感）</li>
 * </ul>
 *
 * <p>不依赖注解级权限（兼容 OPS-09 兄弟在途工作模式），改走 service.requireInternal + requireLeaderOrAdmin 二段门禁。
 */
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class IpdReportController {

    /** 列表查询内部权限码（仅作 @SaCheckPermission 兜底；service 二次校验 actor 范围） */
    public static final String PERM_QUERY = "ipd:report:query";
    /** 津贴导出内部权限码（service 二次校验 actor 范围） */
    public static final String PERM_EXPORT_ALLOWANCE = "ipd:report:export-allowance";
    /** 奖金导出内部权限码（仅 GROUP_LEADER + SUPER_ADMIN） */
    public static final String PERM_EXPORT_BONUS = "ipd:report:export-bonus";
    /** 项目汇总导出内部权限码 */
    public static final String PERM_EXPORT_PROJECT = "ipd:report:export-project";

    private final IpdPermission ipdPermission;
    private final IpdReportService ipdReportService;

    /**
     * P4-4.1 §1：项目绩效汇总列表（分页）。
     */
    @GetMapping("/project-summary")
    public ApiV1Response<IPage<ReportSummaryRow>> listProjectSummary(
            @RequestParam String month,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(ipdReportService.listProjectSummaries(
            month, productId, keyword, pageNo, pageSize, actor));
    }

    /**
     * P4-4.1 §2.1：津贴台账导出（AC-INC-34）。
     */
    @GetMapping("/export/allowance")
    public ApiV1Response<ReportExportResult> exportAllowance(
            @RequestParam String month,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long personId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(ipdReportService.exportAllowance(month, projectId, personId, actor));
    }

    /**
     * P4-4.1 §2.2：奖金台账导出（AC-INC-34）—— 仅 GROUP_LEADER / SUPER_ADMIN。
     */
    @GetMapping("/export/bonus")
    public ApiV1Response<ReportExportResult> exportBonus(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status) {
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        return ApiV1Response.ok(ipdReportService.exportBonus(projectId, status, actor));
    }

    /**
     * P4-4.1 §2.3：项目汇总导出。
     */
    @GetMapping("/export/project")
    public ApiV1Response<ReportExportResult> exportProjectSummary(
            @RequestParam String month,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(ipdReportService.exportProjectSummary(month, productId, keyword, actor));
    }
}
