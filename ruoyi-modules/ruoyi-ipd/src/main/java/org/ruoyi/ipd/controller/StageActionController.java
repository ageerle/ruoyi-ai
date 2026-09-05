package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.service.StageActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 阶段动作实例接口 /api/v1/stage-actions（深轻管分离 BR-IPD-03/04/05）
 * operatorId 暂取 @RequestParam（P1-2 认证打通后切 LoginHelper，与既有控制器一致）
 */
@RestController
@RequestMapping("/api/v1/stage-actions")
@RequiredArgsConstructor
public class StageActionController {

    private final StageActionService stageActionService;

    @GetMapping
    public ApiV1Response<List<StageAction>> list(@RequestParam Long projectId) {
        return ApiV1Response.ok(stageActionService.listByProject(projectId));
    }

    /** 状态流转（DONE 时强制深度+数值校验，拒绝仅前端标记） */
    @PostMapping("/{id}/transit")
    public ApiV1Response<StageAction> transit(@PathVariable Long id,
                                              @RequestParam String target,
                                              @RequestParam Long operatorId) {
        return ApiV1Response.ok(stageActionService.transit(id, target, String.valueOf(operatorId)));
    }

    /** 深管交付物登记（BR-IPD-03 完成前置） */
    @PostMapping("/{id}/deliverables")
    public ApiV1Response<Deliverable> addDeliverable(@PathVariable Long id,
                                                     @RequestParam String fileName,
                                                     @RequestParam(required = false) Long ossId,
                                                     @RequestParam Long operatorId) {
        return ApiV1Response.ok(stageActionService.addDeliverable(id, fileName, ossId, String.valueOf(operatorId)));
    }

    /** 从目录实例化某阶段动作（幂等），返回新建数量 */
    @PostMapping("/instantiate")
    public ApiV1Response<Integer> instantiate(@RequestParam Long projectId,
                                              @RequestParam Long stageId,
                                              @RequestParam String stage) {
        return ApiV1Response.ok(stageActionService.instantiate(projectId, stageId, stage));
    }
}