package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.dto.CertTemplateCreateReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.CertTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 国别认证模板接口 /api/v1/cert-templates（M1：选目标市场自动带出）。
 * SEC-API-01：写操作仅 SUPER_ADMIN，operator 从会话推导。
 */
@RestController
@RequestMapping("/api/v1/cert-templates")
@RequiredArgsConstructor
public class CertTemplateController {

    private final CertTemplateService certTemplateService;
    private final IpdPermission ipdPermission;

    /**
     * 按目标市场解析认证清单，需 ipd:cert-template:list 权限。
     *
     * @param markets 逗号分隔市场码，如 SA,AE
     * @return 匹配的认证模板
     */
    @SaCheckPermission(value = "ipd:cert-template:list", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/resolve")
    public ApiV1Response<List<CertTemplate>> resolve(@RequestParam String markets) {
        return ApiV1Response.ok(certTemplateService.resolve(markets.split(",")));
    }

    /**
     * 查询全部认证模板，需 ipd:cert-template:list 权限。
     *
     * @return 模板列表
     */
    @SaCheckPermission(value = "ipd:cert-template:list", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<CertTemplate>> list() {
        return ApiV1Response.ok(certTemplateService.listAll());
    }

    /**
     * 按国家统计模板数量，需 ipd:cert-template:list 权限。
     *
     * @return 国家码 → 数量
     */
    @SaCheckPermission(value = "ipd:cert-template:list", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/country-counts")
    public ApiV1Response<Map<String, Long>> countryCounts() {
        return ApiV1Response.ok(certTemplateService.countByCountry());
    }

    /**
     * 新建认证模板，需 ipd:cert-template:add 权限。
     * 入参为白名单 DTO（CertTemplateCreateReq），id/tenantId/delFlag 等服务端权威字段不可注入。
     *
     * @param req 新建模板入参
     * @return 新建模板
     */
    @SaCheckPermission(value = "ipd:cert-template:add", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<CertTemplate> create(@RequestBody CertTemplateCreateReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(certTemplateService.create(req.toEntity(), actor.id()));
    }

    /**
     * 移除认证模板，需 ipd:cert-template:remove 权限。
     *
     * @param id 模板 ID
     * @return 空成功体
     */
    @SaCheckPermission(value = "ipd:cert-template:remove", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/remove")
    public ApiV1Response<Void> remove(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        certTemplateService.remove(id, actor.id());
        return ApiV1Response.ok(null);
    }
}
