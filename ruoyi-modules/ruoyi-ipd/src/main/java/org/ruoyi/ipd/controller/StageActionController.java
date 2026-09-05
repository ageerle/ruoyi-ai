package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.StageActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 阶段动作实例接口 /api/v1/stage-actions（深轻管分离 BR-IPD-03/04/05）。
 * SEC-API-01：写操作 operator 仅从会话推导，禁止客户端传 operatorId。
 */
@RestController
@RequestMapping("/api/v1/stage-actions")
@RequiredArgsConstructor
public class StageActionController {

    private final StageActionService stageActionService;
    private final IpdPermission ipdPermission;

    /**
     * 按项目查询阶段动作列表，需 ipd:stage-action:list 权限。
     *
     * @param projectId 项目 ID
     * @return 阶段动作列表
     */
    @SaCheckPermission("ipd:stage-action:list")
    @GetMapping
    public ApiV1Response<List<StageAction>> list(@RequestParam Long projectId) {
        return ApiV1Response.ok(stageActionService.listByProject(projectId));
    }

    /**
     * 阶段动作状态流转（P1-4.3 唯一入口），需 ipd:stage-action:edit 权限。
     *
     * @param id     动作实例 ID
     * @param target 目标状态
     * @param reason NA 等原因说明
     * @return 更新后的动作
     */
    @SaCheckPermission("ipd:stage-action:edit")
    @PostMapping("/{id}/transit")
    public ApiV1Response<StageAction> transit(@PathVariable Long id,
                                              @RequestParam String target,
                                              @RequestParam(required = false) String reason) {
        IpdActor actor = ipdPermission.requireActionWriter(() -> stageActionService.getById(id));
        return ApiV1Response.ok(
            stageActionService.transit(id, target, reason, String.valueOf(actor.id())));
    }

    /**
     * 深管交付物登记（BR-IPD-03 完成前置），需 ipd:stage-action:add 权限。
     *
     * @param id       动作实例 ID
     * @param fileName 文件名
     * @param ossId    可选 OSS 文件 ID
     * @return 新建交付物
     */
    @SaCheckPermission("ipd:stage-action:add")
    @PostMapping("/{id}/deliverables")
    public ApiV1Response<Deliverable> addDeliverable(@PathVariable Long id,
                                                     @RequestParam String fileName,
                                                     @RequestParam(required = false) Long ossId) {
        IpdActor actor = ipdPermission.requireActionWriter(() -> stageActionService.getById(id));
        return ApiV1Response.ok(
            stageActionService.addDeliverable(id, fileName, ossId, String.valueOf(actor.id())));
    }

    /**
     * 从目录实例化某阶段动作（幂等），需 ipd:stage-action:add 权限。
     *
     * @param projectId 项目 ID
     * @param stageId   阶段 ID
     * @param stage     阶段编码
     * @return 新建数量
     */
    @SaCheckPermission("ipd:stage-action:add")
    @PostMapping("/instantiate")
    public ApiV1Response<Integer> instantiate(@RequestParam Long projectId,
                                              @RequestParam Long stageId,
                                              @RequestParam String stage) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(stageActionService.instantiate(projectId, stageId, stage));
    }
}
