package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品接口 /api/v1/products（TS-09 统一响应 code=0）。
 * SEC-API-01：写操作 operator 仅从会话推导，禁止客户端传 operatorId。
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final IpdPermission ipdPermission;

    /**
     * 查询产品列表，需 ipd:product:list 权限。
     *
     * @param keyword 可选关键字
     * @return 产品列表
     */
    @SaCheckPermission("ipd:product:list")
    @GetMapping
    public ApiV1Response<List<Product>> list(@RequestParam(required = false) String keyword) {
        return ApiV1Response.ok(productService.list(keyword));
    }

    /**
     * 查询产品详情，需 ipd:product:query 权限。
     *
     * @param id 产品 ID
     * @return 产品详情
     */
    @SaCheckPermission("ipd:product:query")
    @GetMapping("/{id}")
    public ApiV1Response<Product> get(@PathVariable Long id) {
        return ApiV1Response.ok(productService.getById(id));
    }

    /**
     * 新建产品，需 ipd:product:add 权限。
     *
     * @param req 白名单创建 DTO
     * @return 新建后的产品
     */
    @SaCheckPermission("ipd:product:add")
    @PostMapping
    public ApiV1Response<Product> create(@RequestBody org.ruoyi.ipd.dto.ProductCreateReq req) {
        String src = req.source();
        if (src != null && !src.isBlank()
            && !Product.SRC_ADMIN_IMPORT.equals(src) && !Product.SRC_PM_NEW.equals(src)) {
            throw new org.ruoyi.common.core.exception.ServiceException("产品来源非法（允许 ADMIN_IMPORT|PM_NEW）: " + src);
        }
        IpdActor actor = ipdPermission.requireProductCreator(src);
        return ApiV1Response.ok(productService.create(req.toEntity(), actor.id()));
    }

    /**
     * 产品绑定项目，需 ipd:product:edit 权限。
     *
     * @param id        产品 ID
     * @param projectId 项目 ID
     * @return 空成功体
     */
    @SaCheckPermission("ipd:product:edit")
    @PostMapping("/{id}/bind-project")
    public ApiV1Response<Void> bindProject(@PathVariable Long id, @RequestParam Long projectId) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        productService.bindProject(id, projectId, actor.id());
        return ApiV1Response.ok(null);
    }

    /**
     * 变更产品状态，需 ipd:product:edit 权限。
     *
     * @param id     产品 ID
     * @param status 目标状态
     * @return 空成功体
     */
    @SaCheckPermission("ipd:product:edit")
    @PostMapping("/{id}/status")
    public ApiV1Response<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        IpdActor actor = ipdPermission.requireProductWriter(() -> productService.getById(id));
        productService.changeStatus(id, status, actor.id());
        return ApiV1Response.ok(null);
    }
}
