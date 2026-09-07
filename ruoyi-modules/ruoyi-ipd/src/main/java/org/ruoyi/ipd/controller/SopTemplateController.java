package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.SopTemplate;
import org.ruoyi.ipd.domain.SopTemplateInstance;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
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
 * P1-3.3 SOP 模板接口（/api/v1/sop-templates）——版本管理 + 实例化快照。
 * <ul>
 *   <li>读：list / get / active —— 内部角色</li>
 *   <li>写：publish —— 仅超管（ipd:sop-template:edit）</li>
 *   <li>实例化：instantiate / listInstances —— MARKET_PM/RD_PM/GROUP_LEADER</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sop-templates")
@RequiredArgsConstructor
public class SopTemplateController {

    private final SopTemplateService sopTemplateService;
    private final IpdPermission ipdPermission;

    /** 列表（按 category/status 过滤） */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<SopTemplate>> list(@RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String status) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.listTemplates(category, status));
    }

    /** 当前生效模板（PUBLISHED + effectiveTo IS NULL） */
    @GetMapping("/active")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplate> active(@RequestParam String templateCode) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.getActiveTemplate(templateCode));
    }

    /** 模板详情（任意状态） */
    @GetMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplate> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.getById(id));
    }

    /** 发布新版本（仅超管）——同 templateCode 旧 PUBLISHED 自动 ARCHIVED */
    @PostMapping("/publish")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Long> publish(@RequestBody SopTemplate template) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(sopTemplateService.publishTemplate(template, actor));
    }

    /** 实例化模板 → 生成 SopTemplateInstance 快照（MARKET_PM/RD_PM/GROUP_LEADER） */
    @PostMapping("/{templateId}/instantiate")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<SopTemplateInstance> instantiate(@PathVariable Long templateId,
                                                          @RequestParam Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.instantiate(templateId, projectId, actor));
    }

    /** 按项目列出实例快照 */
    @GetMapping("/instances")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SOP_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<SopTemplateInstance>> listInstances(@RequestParam Long projectId) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(sopTemplateService.listInstancesByProject(projectId, actor));
    }
}