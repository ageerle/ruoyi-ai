package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.security.IpdActor;
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
 * Gate 评审要素管理接口 /api/v1/gate-elements（超管后台；P1-6 补口）。
 * SEC-API-01：写操作仅 SUPER_ADMIN，operator 从会话推导。
 */
@RestController
@RequestMapping("/api/v1/gate-elements")
@RequiredArgsConstructor
public class GateElementController {

    private final GateElementService gateElementService;
    private final IpdPermission ipdPermission;

    /**
     * 查询 Gate 要素列表（可按 gate 过滤），需 ipd:gate-element:list 权限。
     *
     * @param gate 可选 Gate 码，如 G1
     * @return 要素列表
     */
    @SaCheckPermission("ipd:gate-element:list")
    @GetMapping
    public ApiV1Response<List<GateElement>> list(@RequestParam(required = false) String gate) {
        return ApiV1Response.ok(gateElementService.listByGate(gate));
    }

    /**
     * 新建 Gate 要素，需 ipd:gate-element:add 权限。
     *
     * @param req 白名单创建 DTO
     * @return 新建要素
     */
    @SaCheckPermission("ipd:gate-element:add")
    @PostMapping
    public ApiV1Response<GateElement> create(@RequestBody org.ruoyi.ipd.dto.GateElementCreateReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.create(req.toEntity(), String.valueOf(actor.id())));
    }

    /**
     * 更新 Gate 要素，需 ipd:gate-element:edit 权限。
     *
     * @param id  要素 ID
     * @param req 白名单更新 DTO
     * @return 更新后要素
     */
    @SaCheckPermission("ipd:gate-element:edit")
    @PostMapping("/{id}/update")
    public ApiV1Response<GateElement> update(@PathVariable Long id,
                                             @RequestBody org.ruoyi.ipd.dto.GateElementUpdateReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.update(req.toPatch(id), String.valueOf(actor.id())));
    }

    /**
     * 停用 Gate 要素（禁删：在途判定引用证据链），需 ipd:gate-element:remove 权限。
     *
     * @param id 要素 ID
     * @return 停用后要素
     */
    @SaCheckPermission("ipd:gate-element:remove")
    @PostMapping("/{id}/disable")
    public ApiV1Response<GateElement> disable(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(gateElementService.disable(id, String.valueOf(actor.id())));
    }
}
