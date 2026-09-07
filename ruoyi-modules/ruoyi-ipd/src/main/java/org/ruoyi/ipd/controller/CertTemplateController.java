package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.CertTemplateService;
import org.ruoyi.ipd.vo.CertTemplateVO;
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
 * 国别认证模板接口 /api/v1/cert-templates（M1：选目标市场自动带出）
 */
@RestController
@RequestMapping("/api/v1/cert-templates")
@RequiredArgsConstructor
public class CertTemplateController {

    private final CertTemplateService certTemplateService;
    private final IpdPermission ipdPermission;

    /** 项目选定目标市场后自动带出认证清单，需 ipd:cert-template:list 权限 */
    @GetMapping("/resolve")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_CERT_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<CertTemplateVO>> resolve(@RequestParam String markets) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(certTemplateService.resolve(markets.split(",")).stream().map(CertTemplateVO::from).toList());
    }

    /** 查询认证模板列表，需 ipd:cert-template:list 权限 */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_CERT_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<CertTemplateVO>> list() {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(certTemplateService.listAll().stream().map(CertTemplateVO::from).toList());
    }

    /** 查询各国认证模板数量，需 ipd:cert-template:list 权限 */
    @GetMapping("/country-counts")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_CERT_TEMPLATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Map<String, Long>> countryCounts() {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(certTemplateService.countByCountry());
    }

    /** 创建认证模板，需 ipd:cert-template:add 权限 */
    @PostMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_CERT_TEMPLATE_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<CertTemplateVO> create(@RequestBody CertTemplate template) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(CertTemplateVO.from(certTemplateService.create(template, actor.id())));
    }

    /**
     * 删除认证模板入口已关闭：须走删除审核（P0-6.2），禁止直删旁路。
     *
     * @param id         模板 ID
     * @param id 模板 ID
     * @return 业务失败包装（ServiceException → 全局处理器）
     */
    @PostMapping("/{id}/remove")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_CERT_TEMPLATE_DELETE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Void> remove(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        certTemplateService.remove(id, actor.id());
        return ApiV1Response.ok(null);
    }
}
