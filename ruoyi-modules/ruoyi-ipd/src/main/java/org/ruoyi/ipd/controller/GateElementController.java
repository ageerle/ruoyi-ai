package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateElementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gate 评审要素管理接口 /api/v1/gate-elements（超管后台；P1-6 补口）
 */
@RestController
@RequestMapping("/api/v1/gate-elements")
@RequiredArgsConstructor
public class GateElementController {

    private final GateElementService gateElementService;
    private final IpdPermission ipdPermission;

    /** 查询Gate评审要素列表（?gate=G1可选过滤），需 ipd:gate-element:list 权限 */
    @GetMapping
    @SaCheckPermission(value = "ipd:gate-element:list", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<GateElement>> list(@RequestParam(required = false) String gate) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(gateElementService.listByGate(gate));
    }

    /** 创建Gate评审要素，需 ipd:gate-element:add 权限 */
    @PostMapping
    @SaCheckPermission(value = "ipd:gate-element:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<GateElement> create(@RequestBody org.ruoyi.ipd.dto.GateElementCreateReq req) {
        // CODE-01：白名单 DTO，id/tenantId/delFlag 不可注入
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.create(req.toEntity(), String.valueOf(actor.id())));
    }

    /** 更新Gate评审要素，需 ipd:gate-element:edit 权限 */
    @PostMapping("/{id}/update")
    @SaCheckPermission(value = "ipd:gate-element:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<GateElement> update(@PathVariable Long id, @RequestBody org.ruoyi.ipd.dto.GateElementUpdateReq req) {
        // CODE-01：白名单 DTO，gateCode/elementCode 编码不可改
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.update(req.toPatch(id), String.valueOf(actor.id())));
    }

    /** 停用Gate评审要素（禁删：在途判定引用证据链），需 ipd:gate-element:remove 权限 */
    @PostMapping("/{id}/disable")
    @SaCheckPermission(value = "ipd:gate-element:remove", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<GateElement> disable(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.disable(id, String.valueOf(actor.id())));
    }
}
