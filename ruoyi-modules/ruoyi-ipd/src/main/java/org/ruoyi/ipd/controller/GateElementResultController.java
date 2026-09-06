package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateElementResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2-5.1 Gate 逐项判定与提交（/api/v1/gates/{gateId}/...）。
 *
 * <ul>
 *   <li>GET  /elements         —— 33 要素按 Gate 展示（含否决标记与当前判定）</li>
 *   <li>POST /element-results  —— 逐项判定（附件/说明校验 + G1-1 阈值裁决；CONDITIONAL 必填责任人+期限）</li>
 *   <li>POST /submit           —— 提交评审（全要素已判 + 否决阻断 + 冻结版本 + 前序遗留阻断）</li>
 *   <li>GET  /legacy           —— 条件遗留清单（P2-5.3，含已关/未关/逾期标记）</li>
 *   <li>POST /element-results/{resultId}/close —— 关闭遗留（责任人/超管，凭证必填）</li>
 *   <li>POST /api/v1/gates/legacy/scan-overdue（LegacyScanController）—— 逾期扫描发提醒</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/gates/{gateId}")
@RequiredArgsConstructor
public class GateElementResultController {

    private final GateElementResultService service;
    private final IpdPermission permission;

    public record JudgeRequest(@NotNull Long elementId,
                               @NotBlank String result,
                               @Size(max = 500) String conditionNote,
                               @Size(max = 500) String evidenceRef,
                               Integer verifications,
                               Integer writtenIntents,
                               Long responsiblePersonId,
                               String closeDeadline) { }

    public record CloseRequest(@NotBlank @Size(max = 500) String evidence) { }

    public record ResultView(String id, String gateId, String elementId, String result,
                             String conditionNote, String evidenceRef,
                             String leftoverStatus, String responsiblePersonId,
                             String leftoverDueAt, String closedEvidence) {
        public static ResultView from(GateElementResult r) {
            return new ResultView(String.valueOf(r.getId()), String.valueOf(r.getGateId()),
                String.valueOf(r.getElementId()), r.getResult(), r.getConditionNote(), r.getEvidenceRef(),
                r.getLeftoverStatus(), r.getResponsiblePersonId() == null ? null : String.valueOf(r.getResponsiblePersonId()),
                r.getLeftoverDueAt() == null ? null : r.getLeftoverDueAt().toString(), r.getClosedEvidence());
        }
    }

    public record GateView(String id, String projectId, String gateCode, String status,
                           String startedAt, boolean snapshotFrozen) {
        public static GateView from(Gate g) {
            return new GateView(String.valueOf(g.getId()), String.valueOf(g.getProjectId()), g.getGateCode(),
                g.getStatus(), g.getStartedAt() == null ? null : g.getStartedAt().toString(),
                g.getElementSnapshot() != null);
        }
    }

    /** 要素清单（含当前判定，未判定 result=null 供前端高亮缺失项）。 */
    @GetMapping("/elements")
    public ApiV1Response<List<Map<String, Object>>> elements(@PathVariable Long gateId) {
        permission.requireInternal();
        return ApiV1Response.ok(service.checklist(gateId));
    }

    /** 逐项判定：CONDITIONAL 必填说明+责任人+关闭期限（AC-GATE-16）、FAIL 必填证据附件、G1-1 通过须达阈值（AC-GATE-1a~1d）。 */
    @PostMapping("/element-results")
    public ApiV1Response<ResultView> judge(@PathVariable Long gateId,
                                           @Valid @RequestBody JudgeRequest request) {
        IpdActor actor = permission.requireInternal();
        GateElementResult row = service.judge(gateId, request.elementId(), request.result(),
            request.conditionNote(), request.evidenceRef(), request.verifications(), request.writtenIntents(),
            request.responsiblePersonId(), parseDate(request.closeDeadline()), actor);
        return ApiV1Response.ok(ResultView.from(row));
    }

    /** 条件遗留清单（AC-GATE-17 防线：要素停用/删除不消除遗留）。 */
    @GetMapping("/legacy")
    public ApiV1Response<List<Map<String, Object>>> legacy(@PathVariable Long gateId) {
        permission.requireInternal();
        return ApiV1Response.ok(service.legacyList(gateId));
    }

    /** 关闭遗留：仅责任人/超管，凭证必填。 */
    @PostMapping("/element-results/{resultId}/close")
    public ApiV1Response<ResultView> close(@PathVariable Long gateId, @PathVariable Long resultId,
                                           @Valid @RequestBody CloseRequest request) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(ResultView.from(service.close(gateId, resultId, request.evidence(), actor)));
    }

    /** ISO 格式日期解析（yyyy-MM-dd 或 yyyy-MM-dd'T'HH:mm:ss），非法值拒绝。 */
    private static java.util.Date parseDate(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String s = v.trim();
        if (s.length() == 10) {
            s = s + "T23:59:59";
        }
        try {
            return java.util.Date.from(java.time.LocalDateTime.parse(s)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            throw new org.ruoyi.common.core.exception.ServiceException("关闭期限格式非法（期望 yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss）: " + v);
        }
    }

    /**
     * 提交评审：[SEC-FIX-HIGH-1.1] 强制输出物守卫——
     * 全要素已判 + 否决项阻断（AC-GATE-15/19/20）+ 要素定义快照冻结 + 评审材料 + 会议纪要。
     * body 必填 materialsUrl + meetingMinutesUrl（否则 40001 PARAM_INVALID）。
     */
    @PostMapping("/submit")
    public ApiV1Response<GateView> submit(@PathVariable Long gateId,
                                          @Valid @RequestBody MandatoryOutputsReq req) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(GateView.from(
            service.submit(gateId, req.materialsUrl(), req.meetingMinutesUrl(), actor)));
    }

    /** 强制输出物请求体（[SEC-FIX-HIGH-1.1]）。 */
    public record MandatoryOutputsReq(
        @jakarta.validation.constraints.NotBlank(message = "materialsUrl 不能为空")
        @jakarta.validation.constraints.Size(max = 500)
        String materialsUrl,
        @jakarta.validation.constraints.NotBlank(message = "meetingMinutesUrl 不能为空")
        @jakarta.validation.constraints.Size(max = 500)
        String meetingMinutesUrl
    ) {}
}
