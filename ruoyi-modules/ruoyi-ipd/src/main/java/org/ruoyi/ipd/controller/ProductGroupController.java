package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProductGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品组管理接口 /api/v1/product-groups（P2-1；组长随 API 同步 A3）
 *
 * <p>组织架构主数据；删除必须走 DeletionRequest 审核（P0-6.2）。
 */
@RestController
@RequestMapping("/api/v1/product-groups")
@RequiredArgsConstructor
public class ProductGroupController {

    private final ProductGroupService productGroupService;
    private final IpdPermission ipdPermission;

    /** 查询产品组列表，需 ipd:product:list 权限（内部角色均可读） */
    @GetMapping
    @SaCheckPermission(value = "ipd:product:list", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<ProductGroup>> list() {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(productGroupService.listAll());
    }

    /** 查询单个产品组，需 ipd:product:list 权限 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = "ipd:product:list", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProductGroup> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(productGroupService.getById(id));
    }

    /** 新建产品组，需 ipd:product:add 权限 */
    @PostMapping
    @SaCheckPermission(value = "ipd:product:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProductGroup> create(@RequestBody ProductGroup group) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(productGroupService.create(group, actor.id()));
    }

    /** 替换组长（HR 同步场景），需 ipd:product:edit 权限 */
    @PostMapping("/{id}/leader")
    @SaCheckPermission(value = "ipd:product:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProductGroup> updateLeader(@PathVariable Long id, @RequestBody Long newLeaderPersonId) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(productGroupService.updateLeader(id, newLeaderPersonId, actor.id()));
    }

    /**
     * 删除入口已关闭：须走删除审核（P0-6.2）。
     */
    @PostMapping("/{id}/remove")
    @SaCheckPermission(value = "ipd:product:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Void> remove(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        productGroupService.remove(id, actor.id());
        return ApiV1Response.ok(null);
    }
}
