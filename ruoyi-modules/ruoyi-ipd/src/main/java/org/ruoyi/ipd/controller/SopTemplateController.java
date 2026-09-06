package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.SopTemplate;
import org.ruoyi.ipd.dto.SopTemplateListItem;
import org.ruoyi.ipd.dto.SopTemplateSaveReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.SopTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P1-3.3 SOP 模板接口 /api/v1/sop-templates（页46；BR-IPD-07）。
 * <ul>
 *   <li>读（版本列表/当前生效/详情）：内部角色（深管执行动作时按 actionCode 取 SOP）。</li>
 *   <li>写（copy/update/publish/revert）：仅超管（ipd:sop-template:edit）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sop-templates")
@RequiredArgsConstructor
public class SopTemplateController {

    private final SopTemplateService sopTemplateService;
    private final IpdPermission ipdPermission;

    /** 版本列表（?actionCode 必填，version 倒序，不含正文）。 */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<SopTemplateListItem>> list(@RequestParam String actionCode) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.listByAction(actionCode));
    }

    /** 深管取当前生效 SOP（PUBLISHED 全文）。 */
    @GetMapping("/current")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplate> current(@RequestParam String actionCode) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.currentForAction(actionCode));
    }

    /** 版本详情（在研项目按 stage_actions.sop_id 快照取历史版本）。 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplate> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.get(id));
    }

    /** 复制当前/历史版本为 draft（→ draftId），仅超管。 */
    @PostMapping("/{id}/copy")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplateListItem> copy(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(SopTemplateService.toItem(sopTemplateService.copyToDraft(id, String.valueOf(actor.id()))));
    }

    /** 编辑 draft（仅 DRAFT 可改），仅超管。 */
    @PostMapping("/{id}/update")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplateListItem> update(@PathVariable Long id, @RequestBody SopTemplateSaveReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(SopTemplateService.toItem(sopTemplateService.updateDraft(id, req, String.valueOf(actor.id()))));
    }

    /** 发布 draft（旧 PUBLISHED 自动 ARCHIVED；新项目实例化起用新版本），仅超管。 */
    @PostMapping("/{id}/publish")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplateListItem> publish(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(SopTemplateService.toItem(sopTemplateService.publish(id, String.valueOf(actor.id()))));
    }

    /** 历史恢复：指定版本复制为新 draft，仅超管。 */
    @PostMapping("/{id}/revert")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplateListItem> revert(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(SopTemplateService.toItem(sopTemplateService.revert(id, String.valueOf(actor.id()))));
    }
}
