package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.AiModelSaveReq;
import org.ruoyi.ipd.dto.AiModelView;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AiModelConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P4-2.1 AI 模型配置接口 /api/v1/ai-models（AC-AI-01）。
 * 读=内部角色（仅脱敏视图）；写/启停/连通测试=仅超管（卡面：只有超管管理）。
 */
@RestController
@RequestMapping("/api/v1/ai-models")
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiModelConfigService service;
    private final IpdPermission ipdPermission;

    /** 配置列表（脱敏，密钥永不回显）。 */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<AiModelView>> list() {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(service.list());
    }

    /** 配置详情（脱敏）。 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<AiModelView> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(service.get(id));
    }

    /** 新建配置（apiKey 明文进密文出），仅超管。 */
    @PostMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<AiModelView> create(@RequestBody AiModelSaveReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(service.create(req, String.valueOf(actor.id())));
    }

    /** 更新（apiKey=null 不改密钥），仅超管。 */
    @PostMapping("/{id}/update")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<AiModelView> update(@PathVariable Long id, @RequestBody AiModelSaveReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(service.update(id, req, String.valueOf(actor.id())));
    }

    /** 启用（全局至多一条生效），仅超管。 */
    @PostMapping("/{id}/enable")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<AiModelView> enable(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(service.enable(id, String.valueOf(actor.id())));
    }

    /** 连接测试（失败消息白名单化，不泄露凭证），仅超管。 */
    @PostMapping("/{id}/test")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_AI_MODEL_EDIT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<AiModelView> test(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(service.testConnect(id, String.valueOf(actor.id())));
    }
}
