package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.GateReviewObserver;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2-5.2/P2-5.4 Gate 双签与轮次接口（/api/v1/gates/{gateId}/...）。
 *
 * <ul>
 *   <li>POST /sign  —— 签署（G1/G5 双 PM 盲签；G2/3/4 领域主导方签署）</li>
 *   <li>GET  /review —— 双签视图（在途互不可见仅"对方已提交"；终态/超管全揭示）</li>
 *   <li>POST /reopen —— 否决后重新发起：round+1，第3轮组长列席/第5轮超管介入（AC-GATE-06/07/07b）</li>
 *   <li>POST /extend-deadline —— 超管延长签署期限，最多 3 次（AC-GATE-21）</li>
 *   <li>POST /arbitrate —— 组长仲裁意见（AC-GATE-10 中段）</li>
 *   <li>POST /final-ruling —— 超管终裁（AC-GATE-10 尾段）</li>
 *   <li>POST /observers/invite —— MEDIUM-1.3 列席人员邀请（销售/供应/售后/品质/合规）</li>
 *   <li>POST /observers/{observerId}/opinion —— MEDIUM-1.3 列席人提交意见</li>
 *   <li>GET  /observers —— MEDIUM-1.3 列席人员 + 意见查询（仅组长/超管）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/gates/{gateId}")
@RequiredArgsConstructor
public class GateReviewController {

    private final GateReviewService service;
    private final IpdPermission permission;

    public record SignRequest(@NotBlank String decision,
                              @Size(max = 1000) String opinion) { }

    /** AC-GATE-21：延长天数（1-30） */
    public record ExtendRequest(@NotNull Integer days) { }

    /** AC-GATE-10：仲裁/终裁意见 */
    public record ArbitrateRequest(@NotBlank String decision,
                                   @Size(max = 1000) String opinion) { }

    /** MEDIUM-1.3：列席人员邀请请求 */
    public record InviteObserversRequest(@NotNull List<Long> observerIds,
                                         @NotBlank String role) { }

    /** MEDIUM-1.3：列席人提交意见请求 */
    public record ObserverOpinionRequest(@NotBlank @Size(max = 2000) String opinion) { }

    public record SignView(String id, String gateId, String reviewerType, String decision,
                           String opinion, String signedAt, String dueAt, Integer round) {
        public static SignView from(GateReview r) {
            return new SignView(String.valueOf(r.getId()), String.valueOf(r.getGateId()),
                r.getReviewerType(), r.getDecision(), r.getOpinion(),
                r.getSignedAt() == null ? null : r.getSignedAt().toString(),
                r.getDueAt() == null ? null : r.getDueAt().toString(), r.getRound());
        }
    }

    /** 签署：APPROVE|REJECT；每方每轮一条（重复拒）；任一 REJECT ⇒ Gate REJECTED（AC-GATE-05）。 */
    @PostMapping("/sign")
    public ApiV1Response<SignView> sign(@PathVariable Long gateId,
                                        @Valid @RequestBody SignRequest request) {
        IpdActor actor = permission.requireInternal();
        GateReview row = service.sign(gateId, request.decision(), request.opinion(), actor);
        return ApiV1Response.ok(SignView.from(row));
    }

    /** 双签视图：AC-GATE-03 在途互不可见（otherSubmitted 标志）/ AC-GATE-04 终态同时揭示。 */
    @GetMapping("/review")
    public ApiV1Response<Map<String, Object>> review(@PathVariable Long gateId) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(service.view(gateId, actor));
    }

    /** 重新发起（AC-GATE-06）：round+1；第3轮起组长列席/第5轮起超管介入通知（AC-GATE-07/07b）。 */
    @PostMapping("/reopen")
    public ApiV1Response<Map<String, Object>> reopen(@PathVariable Long gateId) {
        IpdActor actor = permission.requireInternal();
        Gate gate = service.reopen(gateId, actor);
        return ApiV1Response.ok(Map.of(
            "id", String.valueOf(gate.getId()),
            "status", gate.getStatus(),
            "round", gate.getCurrentRound(),
            "signDueAt", gate.getSignDueAt() == null ? "" : gate.getSignDueAt().toString()));
    }

    /** 延长签署期限（AC-GATE-21）：仅超管，最多 3 次，第 4 次拒绝。 */
    @PostMapping("/extend-deadline")
    public ApiV1Response<Map<String, Object>> extendDeadline(@PathVariable Long gateId,
                                                             @Valid @RequestBody ExtendRequest request) {
        IpdActor actor = permission.requireInternal();
        Gate gate = service.extendDeadline(gateId, request.days(), actor);
        return ApiV1Response.ok(Map.of(
            "id", String.valueOf(gate.getId()),
            "status", gate.getStatus(),
            "signDueAt", gate.getSignDueAt().toString(),
            "extensionCount", gate.getSignExtensionCount()));
    }

    /** 组长仲裁意见（AC-GATE-10）：分歧驳回后双方组长提交，两组不一致自动升级超管。 */
    @PostMapping("/arbitrate")
    public ApiV1Response<ArbitrationView> arbitrate(@PathVariable Long gateId,
                                                    @Valid @RequestBody ArbitrateRequest request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(ArbitrationView.from(
            service.arbitrate(gateId, request.decision(), request.opinion(), actor)));
    }

    /** 超管终裁（AC-GATE-10）：终裁结果写入项目审计日志。 */
    @PostMapping("/final-ruling")
    public ApiV1Response<ArbitrationView> finalRuling(@PathVariable Long gateId,
                                                      @Valid @RequestBody ArbitrateRequest request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(ArbitrationView.from(
            service.finalRuling(gateId, request.decision(), request.opinion(), actor)));
    }

    /** MEDIUM-1.3：列席人员邀请。仅超管/组长。邀请后 audit + 知会被邀请人。 */
    @PostMapping("/observers/invite")
    public ApiV1Response<Map<String, Object>> inviteObservers(@PathVariable Long gateId,
                                                              @Valid @RequestBody InviteObserversRequest request) {
        IpdActor actor = permission.requireInternal();
        int count = service.inviteObservers(gateId, request.observerIds(), request.role(), actor);
        return ApiV1Response.ok(Map.of(
            "gateId", String.valueOf(gateId),
            "role", request.role(),
            "invitedCount", count));
    }

    /** MEDIUM-1.3：列席人提交意见（仅本人）。 */
    @PostMapping("/observers/{observerId}/opinion")
    public ApiV1Response<ObserverView> recordObserverOpinion(@PathVariable Long gateId,
                                                             @PathVariable Long observerId,
                                                             @Valid @RequestBody ObserverOpinionRequest request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(ObserverView.from(
            service.recordOpinion(gateId, observerId, request.opinion(), actor)));
    }

    /** MEDIUM-1.3：查 Gate 列席人员 + 意见（仅组长/超管）。 */
    @GetMapping("/observers")
    public ApiV1Response<List<ObserverView>> listObservers(@PathVariable Long gateId) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(service.listObservers(gateId, actor).stream()
            .map(ObserverView::from).toList());
    }

    /** 仲裁/终裁行视图。 */
    public record ArbitrationView(String id, String gateId, Integer round, String arbitratorType,
                                  String arbitratorId, String decision, String opinion) {
        public static ArbitrationView from(GateArbitration r) {
            return new ArbitrationView(String.valueOf(r.getId()), String.valueOf(r.getGateId()),
                r.getRound(), r.getArbitratorType(), String.valueOf(r.getArbitratorId()),
                r.getDecision(), r.getOpinion());
        }
    }

    /** MEDIUM-1.3：列席人视图。 */
    public record ObserverView(String id, String gateId, String observerId, String role,
                               Integer attended, String opinion, String invitedAt) {
        public static ObserverView from(GateReviewObserver o) {
            return new ObserverView(String.valueOf(o.getId()), String.valueOf(o.getGateId()),
                String.valueOf(o.getObserverId()), o.getRole(),
                o.getAttended(), o.getOpinion(),
                o.getInvitedAt() == null ? null : o.getInvitedAt().toString());
        }
    }
}
