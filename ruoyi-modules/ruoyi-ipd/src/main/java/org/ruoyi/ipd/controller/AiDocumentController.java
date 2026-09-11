package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AiDocument;
import org.ruoyi.ipd.dto.AiGenerateReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.AiDocumentService;
import org.ruoyi.ipd.service.AiGenerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 文档版本链 API /api/v1/ai-documents（P1-10.1；页33 AI 文档）。
 * 只做版本链存储（版本号+人工审核+sha256 摘要）；AI 生成/模型配置/预算属 P4-2，
 * 届时由生成侧调用 {@link AiDocumentService#createGenerated} 登记首环。
 * 历史版本只读：内容与摘要无任何 HTTP 更新通道，修正=产生新版本。
 */
@RestController
@RequestMapping("/api/v1/ai-documents")
@RequiredArgsConstructor
public class AiDocumentController {

    private final AiDocumentService aiDocumentService;
    private final AiGenerationService aiGenerationService;
    private final IpdPermission ipdPermission;

    /**
     * 登记 AI 原始输出 v1（版本链首环；生成入口 P4-2 接管）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<AiDocument> create(@RequestBody CreateReq body) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.createGenerated(
            body.projectId(), body.docType(), body.title(), body.content(),
            body.model(), body.tokenPrompt(), body.tokenCompletion(), actor.id()));
    }

    /**
     * P4-2.2：AI 生成（AC-AI-02：PM 录入原始资料 → 模型润色/补齐/标准化 → 登记 v1 待审核）。
     * 权限同登记（ipd:ai-document:add，PM 与组长对等，AC-AI-10）；超时/限流/预算
     * 走生效模型配置（BR-AI-01）；输出透传不过滤（BR-AI-04），UI 层须有风险提示。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/generate")
    public ApiV1Response<AiDocument> generate(@RequestBody AiGenerateReq body) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiGenerationService.generate(actor, body));
    }

    /**
     * 人工改版：基于 baseVersionId 追加 v(n+1)；基准非当前最新版 → 409 明确冲突
     * （AC-AI-04：审核通过后修改 ⇒ 生成 v2，v1 保留）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_REVISE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/revise")
    public ApiV1Response<AiDocument> revise(@PathVariable Long id, @RequestBody ReviseReq body) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.revise(
            id, body.baseVersionId(), body.content(), body.title(), actor.id()));
    }

    /**
     * 人工审核通过（BR-AI-03：AI 输出未经审核不生效）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_REVIEW, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/versions/{versionId}/review")
    public ApiV1Response<AiDocument> review(@PathVariable Long id, @PathVariable Long versionId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.review(versionId, actor.id()));
    }

    /**
     * 完整版本链 v1..vN（AC-AI-06：无一缺失；链断裂按 409 报出）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{id}/versions")
    public ApiV1Response<List<AiDocument>> versions(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.history(id));
    }

    /**
     * AC-AI-05：版本链回溯视图 v1..vN（与 /versions 同源；独立 URL 便于前端按场景切换）。
     * 输出格式：版本号、作者（createBy）、创建时间、当前状态四元组，便于历史侧栏渲染。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{id}/history")
    public ApiV1Response<List<HistoryItem>> history(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.history(id).stream()
            .map(d -> new HistoryItem(d.getId(), d.getVersionNo(), d.getCreateBy(),
                d.getCreateTime(), d.getStatus(), d.getReviewedBy(), d.getArchivedAt()))
            .toList());
    }

    /**
     * AC-AI-03 / BR-AI-02：未审核拒绝归档——仅 REVIEWED 行可归档。
     * 权限复用 OPERATION_AI_DOCUMENT_REVIEW（与审核同义角色集——内部四角色）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_REVIEW, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/versions/{versionId}/archive")
    public ApiV1Response<AiDocument> archive(@PathVariable Long id, @PathVariable Long versionId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.archive(versionId, actor.id()));
    }

    /**
     * BR-AI-03 兜底：审核拒绝——REVIEWED → REJECTED（必须重新走审核才能归档）。
     * 权限复用 OPERATION_AI_DOCUMENT_REVIEW（与 review 同行使）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT_REVIEW, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/versions/{versionId}/reject")
    public ApiV1Response<AiDocument> reject(@PathVariable Long id,
                                             @PathVariable Long versionId,
                                             @RequestBody RejectReq body) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.reject(
            versionId, actor.id(), body != null ? body.comment() : null));
    }

    /**
     * AC-AI-05：任意两版本字段级 diff。from/to 为版本行 ID（同链上才有意义；跨链由调用方负责）。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_DOCUMENT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{id}/diff")
    public ApiV1Response<AiDocumentService.DiffReport> diff(@PathVariable Long id,
                                                           @RequestParam("from") Long fromVersionId,
                                                           @RequestParam("to") Long toVersionId) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(aiDocumentService.diff(fromVersionId, toVersionId));
    }

    /** 回溯视图单行：版本行 ID + 版本号 + 创建者 + 创建时间 + 当前状态 + 审核人 + 归档时间 */
    public record HistoryItem(Long versionId, Integer versionNo, Long author,
                              java.util.Date createdAt, String status,
                              Long reviewedBy, java.util.Date archivedAt) {}

    /** 拒绝请求体（comment 必填，落 review_comment 审计完整性） */
    public record RejectReq(@NotBlank @Size(max = 1000) String comment) {}

    /** 登记 AI 原始输出请求体 */
    public record CreateReq(@NotNull Long projectId,
                            @Size(max = 32) String docType,
                            @NotBlank @Size(max = 200) String title,
                            @NotBlank String content,
                            @Size(max = 64) String model,
                            Integer tokenPrompt,
                            Integer tokenCompletion) {
    }

    /** 人工改版请求体（baseVersionId=乐观锁基准，非最新版即 409） */
    public record ReviseReq(@NotNull Long baseVersionId,
                            @NotBlank String content,
                            @Size(max = 200) String title) {
    }
}
