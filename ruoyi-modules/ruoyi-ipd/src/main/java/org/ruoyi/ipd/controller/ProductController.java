package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 产品接口 /api/v1/products（TS-09 统一响应 code=0）
 * 操作人取自 IPD 会话当前登录人（SEC-API-01）。
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final IpdPermission ipdPermission;

    /** 查询产品列表，需 ipd:product:list 权限 */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<Product>> list(@RequestParam(required = false) String keyword) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(productService.list(keyword));
    }

    /** 查询产品详情，需 ipd:product:query 权限 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Product> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(productService.getById(id));
    }

    /** 创建产品，需 ipd:product:add 权限 */
    @PostMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Product> create(@RequestBody org.ruoyi.ipd.dto.ProductCreateReq req) {
        // CODE-01：白名单 DTO；source 三路枚举校验（BR-PROD-01），projectId/status 不可注入
        String src = req.source();
        if (src != null && !src.isBlank()
            && !Product.SRC_ADMIN_IMPORT.equals(src) && !Product.SRC_PM_NEW.equals(src)) {
            throw new org.ruoyi.common.core.exception.ServiceException("产品来源非法（允许 ADMIN_IMPORT|PM_NEW）: " + src);
        }
        IpdActor actor = ipdPermission.requireProductCreator(src);
        return ApiV1Response.ok(productService.create(req.toEntity(), actor.id()));
    }

    /** P1-1.2：编辑产品，需 ipd:product:edit */
    @PutMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_BIND_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Product> update(@PathVariable Long id, @RequestBody org.ruoyi.ipd.dto.ProductUpdateReq req) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        return ApiV1Response.ok(productService.update(id, req.toPatch(), actor.id(), actor.groupId(), actor.role()));
    }

    /**
     * P1-1.2 / AC-PROD-06：超管批量导入在售型号。
     * 普通 PM 无 SUPER_ADMIN 角色将被拒绝。
     */
    @PostMapping("/batch-import")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<Map<String, Object>>> batchImport(
        @RequestBody List<org.ruoyi.ipd.dto.ProductImportItem> items) {
        IpdActor actor = ipdPermission.requireAdmin();
        List<Product> rows = items == null ? List.of()
            : items.stream().map(org.ruoyi.ipd.dto.ProductImportItem::toEntity).toList();
        return ApiV1Response.ok(productService.batchImportOnSale(rows, actor.id()));
    }

    /** 绑定项目，需 ipd:product:edit 权限 */
    @PostMapping("/{id}/bind-project")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_BIND_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Void> bindProject(@PathVariable Long id, @RequestParam Long projectId) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        productService.bindProject(id, projectId, actor.id(), actor.groupId(), actor.role());
        return ApiV1Response.ok(null);
    }

    /** P1-1.1：解绑项目（产品 ↔ 项目双向 1:1），需 ipd:product:edit 权限 */
    @PostMapping("/{id}/unbind-project")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_BIND_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Void> unbindProject(@PathVariable Long id, @RequestParam Long projectId) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        productService.unbindProject(id, projectId, actor.id(), actor.groupId(), actor.role());
        return ApiV1Response.ok(null);
    }

    /** 变更产品状态，需 ipd:product:edit 权限 */
    @PostMapping("/{id}/status")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_GROUP_BIND_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        productService.changeStatus(id, status, actor.id(), actor.groupId(), actor.role());
        return ApiV1Response.ok(null);
    }
}
