package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.CoefficientChangeRequest;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.CoefficientChangeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * S/B 级系数定值 API（AC-INC-15c）。
 */
@RestController
@RequestMapping("/api/v1/coefficient-change-requests")
@RequiredArgsConstructor
public class CoefficientChangeController {

    private final CoefficientChangeService coefficientChangeService;
    private final IpdPermission ipdPermission;

    /**
     * 双PM 联合提议。
     *
     * @param body 项目/系数/理由/双PM
     * @return 待组长确认申请
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COEFFICIENT_PROPOSE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<CoefficientChangeRequest> propose(@Valid @RequestBody ProposeReq body) {
        // IPD 会话 loginType=ipd，禁止 LoginHelper（基线 StpUtil）取 userId
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(coefficientChangeService.propose(
            body.projectId(), body.proposedCoefficient(), body.reason(),
            body.marketPmId(), body.rdPmId(), actor.id(), body.leaderId()));
    }

    /**
     * 产品组长确认或驳回。
     *
     * @param id      申请 ID
     * @param approve true=写入项目档案
     * @param opinion 意见
     * @return 终态申请
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COEFFICIENT_CONFIRM, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/leader-decision")
    public ApiV1Response<CoefficientChangeRequest> leaderDecision(@PathVariable Long id,
                                                                  @RequestParam boolean approve,
                                                                  @RequestParam(required = false) String opinion) {
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        return ApiV1Response.ok(coefficientChangeService.leaderDecision(
            id, actor.id(), approve, opinion));
    }

    /** 联合提议入参。R11 / A2 修复:leaderId 字段（提议时前端选定组长）。 */
    public record ProposeReq(
        @NotNull Long projectId,
        @NotNull BigDecimal proposedCoefficient,
        @NotBlank @Size(max = 500) String reason,
        @NotNull Long marketPmId,
        @NotNull Long rdPmId,
        @NotNull Long leaderId) {
    }
}
