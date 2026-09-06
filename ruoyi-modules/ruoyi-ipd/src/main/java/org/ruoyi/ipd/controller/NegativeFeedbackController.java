package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.NegativeFeedbackCreateReq;
import org.ruoyi.ipd.dto.NegativeFeedbackDecisionReq;
import org.ruoyi.ipd.dto.NegativeFeedbackView;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.NegativeFeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P3-8.2 负反馈执行 API（BR-INC-10；AC-INC-36b/37/38/39/40）
 *
 * <p>端点：
 * <ul>
 *   <li>POST   /api/v1/negative-feedbacks                          DRAFT 录入</li>
 *   <li>PUT    /api/v1/negative-feedbacks/{id}/submit              提交认定 DRAFT → PENDING_DECISION</li>
 *   <li>PUT    /api/v1/negative-feedbacks/{id}/decide              组长认定 PENDING_DECISION → EXECUTED/REJECTED</li>
 *   <li>PUT    /api/v1/negative-feedbacks/{id}/lift                解除 EXECUTED → LIFTED</li>
 *   <li>GET    /api/v1/negative-feedbacks/{id}                     详情</li>
 *   <li>GET    /api/v1/negative-feedbacks                          项目下状态过滤</li>
 *   <li>GET    /api/v1/negative-feedbacks/by-project/{projectId}/effective  当前生效</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/negative-feedbacks")
@RequiredArgsConstructor
public class NegativeFeedbackController {

    private final NegativeFeedbackService negativeFeedbackService;
    private final IpdPermission ipdPermission;

    @SaCheckPermission(value = "ipd:negative-feedback:create", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<NegativeFeedbackView> create(@RequestBody @jakarta.validation.Valid NegativeFeedbackCreateReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(NegativeFeedbackService.toView(negativeFeedbackService.create(req, actor)));
    }

    @SaCheckPermission(value = "ipd:negative-feedback:create", type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{id}/submit")
    public ApiV1Response<NegativeFeedbackView> submit(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(NegativeFeedbackService.toView(negativeFeedbackService.submit(id, actor)));
    }

    @SaCheckPermission(value = "ipd:negative-feedback:decide", type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{id}/decide")
    public ApiV1Response<NegativeFeedbackView> decide(@PathVariable Long id,
                                                     @RequestBody NegativeFeedbackDecisionReq req) {
        // 二次校验：必须 GROUP_LEADER / SUPER_ADMIN
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        return ApiV1Response.ok(NegativeFeedbackService.toView(negativeFeedbackService.decide(id, req, actor)));
    }

    @SaCheckPermission(value = "ipd:negative-feedback:decide", type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{id}/lift")
    public ApiV1Response<NegativeFeedbackView> lift(@PathVariable Long id,
                                                    @RequestBody(required = false) NegativeFeedbackDecisionReq req) {
        IpdActor actor = ipdPermission.requireLeaderOrAdmin();
        NegativeFeedbackDecisionReq body = req != null ? req : new NegativeFeedbackDecisionReq("LIFT", null);
        return ApiV1Response.ok(NegativeFeedbackService.toView(negativeFeedbackService.lift(id, body, actor)));
    }

    @SaCheckPermission(value = "ipd:negative-feedback:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{id}")
    public ApiV1Response<NegativeFeedbackView> detail(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(NegativeFeedbackService.toView(negativeFeedbackService.getById(id, actor)));
    }

    @SaCheckPermission(value = "ipd:negative-feedback:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<NegativeFeedbackView>> list(@RequestParam Long projectId,
                                                          @RequestParam(required = false) String status) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(negativeFeedbackService.listByProject(projectId, actor, status).stream()
            .map(NegativeFeedbackService::toView).toList());
    }

    @SaCheckPermission(value = "ipd:negative-feedback:query", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/by-project/{projectId}/effective")
    public ApiV1Response<List<NegativeFeedbackView>> effectiveByProject(@PathVariable Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(negativeFeedbackService.effectiveByProject(projectId, actor).stream()
            .map(NegativeFeedbackService::toView).toList());
    }
}