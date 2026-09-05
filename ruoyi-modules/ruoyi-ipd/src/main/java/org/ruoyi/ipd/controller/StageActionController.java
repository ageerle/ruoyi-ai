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
 * P1-4.3：状态迁移唯一入口 /transit，禁止 PATCH status 字段
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

    /**
     * 状态流转（P1-4.3 唯一入口）。
     * - 深管：NOT_STARTED/IN_PROGRESS/DONE/NA/DELAYED
     * - 轻管：NOT_STARTED/IN_PROGRESS/DONE/NA（无 DELAYED）
     * - NA 必须传 reason；幂等：同 target 返回当前态不写审计
     * - 乐观锁：并发同 id 重复 /transit 由 MP 仅 1 成功
     */
    @PostMapping("/{id}/transit")
    public ApiV1Response<StageAction> transit(@PathVariable Long id,
                                              @RequestParam String target,
                                              @RequestParam(required = false) String reason,
                                              @RequestParam Long operatorId) {
        return ApiV1Response.ok(stageActionService.transit(id, target, reason, String.valueOf(operatorId)));
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