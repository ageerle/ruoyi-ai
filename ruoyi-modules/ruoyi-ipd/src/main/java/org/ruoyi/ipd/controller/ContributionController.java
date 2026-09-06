package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.ContributionSaveReq;
import org.ruoyi.ipd.dto.ContributionView;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.service.ContributionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 贡献度评定 Controller（P3-6.2；BR-INC-09 / AC-INC-25~28）。
 *
 * <p>5 端点：
 * <ul>
 *   <li>{@code GET    /api/v1/contributions/{projectId}}                  — 查询最新评定</li>
 *   <li>{@code GET    /api/v1/contributions/{projectId}/preview}           — 公式预览（tier 修正因子）</li>
 *   <li>{@code POST   /api/v1/contributions/{projectId}/save}              — 双 PM 自评保存</li>
 *   <li>{@code POST   /api/v1/contributions/{projectId}/market-share}      — 调整市场 PM 比例（40-65% 联动研发）</li>
 *   <li>{@code POST   /api/v1/contributions/{projectId}/confirm}           — 产品组长确认（APPROVE/REJECT）</li>
 * </ul>
 *
 * <p>权限：
 * <ul>
 *   <li>query / save — {@code ipd:contribution:query/save}（双 PM + 组长 + 超管）</li>
 *   <li>confirm — {@code ipd:contribution:confirm}（仅产品组长 / 超管）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
@Validated
public class ContributionController {

    private final ContributionService contributionService;

    /**
     * 查询项目最新贡献度评定。
     */
    @SaCheckPermission(value = "ipd:contribution:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{projectId}")
    public ApiV1Response<ContributionView> get(@PathVariable Long projectId) {
        return ApiV1Response.ok(contributionService.getByProject(projectId));
    }

    /**
     * 公式预览（不改库；返回 tierCoefficient 与联动比例）。
     */
    @SaCheckPermission(value = "ipd:contribution:query", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/preview")
    public ApiV1Response<ContributionView> preview(@PathVariable Long projectId,
                                                    @RequestBody @Valid ContributionSaveReq req) {
        return ApiV1Response.ok(contributionService.preview(projectId, req));
    }

    /**
     * 双 PM 自评保存（五维度原始分数）。
     */
    @SaCheckPermission(value = "ipd:contribution:save", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/save")
    public ApiV1Response<ContributionView> saveSelf(@PathVariable Long projectId,
                                                    @RequestBody @Valid ContributionSaveReq req) {
        return ApiV1Response.ok(contributionService.saveSelf(projectId, req));
    }

    /**
     * 调整市场 PM 比例（区间 [0.40, 0.65]，研发联动）。
     */
    @SaCheckPermission(value = "ipd:contribution:save", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/market-share")
    public ApiV1Response<ContributionView> adjustMarketShare(
        @PathVariable Long projectId,
        @RequestParam @NotNull
        @DecimalMin(value = "0.40", message = "市场 PM 比例不能低于 0.40")
        @DecimalMax(value = "0.65", message = "市场 PM 比例不能高于 0.65") BigDecimal marketShare) {
        return ApiV1Response.ok(contributionService.adjustMarketShare(projectId, marketShare));
    }

    /**
     * 产品组长确认（APPROVE → CONFIRMED；REJECT → 退回 DRAFT）。
     */
    @SaCheckPermission(value = "ipd:contribution:confirm", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/confirm")
    public ApiV1Response<ContributionView> confirm(@PathVariable Long projectId,
                                                   @RequestParam @NotNull String decision,
                                                   @RequestParam(required = false) String opinion) {
        return ApiV1Response.ok(contributionService.confirm(projectId, decision, opinion));
    }
}