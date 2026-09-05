package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Product;
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
 * 产品接口 /api/v1/products（TS-09 统一响应 code=0）
 * 操作人暂取参数 operatorId，P1-2 认证打通后改 LoginHelper 当前人。
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiV1Response<List<Product>> list(@RequestParam(required = false) String keyword) {
        return ApiV1Response.ok(productService.list(keyword));
    }

    @GetMapping("/{id}")
    public ApiV1Response<Product> get(@PathVariable Long id) {
        return ApiV1Response.ok(productService.getById(id));
    }

    @PostMapping
    public ApiV1Response<Product> create(@RequestBody org.ruoyi.ipd.dto.ProductCreateReq req, @RequestParam Long operatorId) {
        // CODE-01：白名单 DTO；source 三路枚举校验（BR-PROD-01），projectId/status 不可注入
        String src = req.source();
        if (src != null && !src.isBlank()
            && !Product.SRC_ADMIN_IMPORT.equals(src) && !Product.SRC_PM_NEW.equals(src)) {
            throw new org.ruoyi.common.core.exception.ServiceException("产品来源非法（允许 ADMIN_IMPORT|PM_NEW）: " + src);
        }
        return ApiV1Response.ok(productService.create(req.toEntity(), operatorId));
    }

    @PostMapping("/{id}/bind-project")
    public ApiV1Response<Void> bindProject(@PathVariable Long id, @RequestParam Long projectId, @RequestParam Long operatorId) {
        productService.bindProject(id, projectId, operatorId);
        return ApiV1Response.ok(null);
    }

    @PostMapping("/{id}/status")
    public ApiV1Response<Void> changeStatus(@PathVariable Long id, @RequestParam String status, @RequestParam Long operatorId) {
        productService.changeStatus(id, status, operatorId);
        return ApiV1Response.ok(null);
    }
}