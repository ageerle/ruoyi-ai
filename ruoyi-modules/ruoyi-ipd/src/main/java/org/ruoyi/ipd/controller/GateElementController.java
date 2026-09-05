package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.GateElement;
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

    /** ?gate=G1 可选过滤（缺省全量，按 Gate+排序） */
    @GetMapping
    public ApiV1Response<List<GateElement>> list(@RequestParam(required = false) String gate) {
        return ApiV1Response.ok(gateElementService.listByGate(gate));
    }

    @PostMapping
    public ApiV1Response<GateElement> create(@RequestBody GateElement element, @RequestParam Long operatorId) {
        return ApiV1Response.ok(gateElementService.create(element, String.valueOf(operatorId)));
    }

    @PostMapping("/{id}/update")
    public ApiV1Response<GateElement> update(@PathVariable Long id, @RequestBody GateElement patch,
                                             @RequestParam Long operatorId) {
        patch.setId(id);
        return ApiV1Response.ok(gateElementService.update(patch, String.valueOf(operatorId)));
    }

    /** 停用（禁删：在途判定引用证据链） */
    @PostMapping("/{id}/disable")
    public ApiV1Response<GateElement> disable(@PathVariable Long id, @RequestParam Long operatorId) {
        return ApiV1Response.ok(gateElementService.disable(id, String.valueOf(operatorId)));
    }
}