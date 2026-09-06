package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.StageActionFieldsReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.StageActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final IpdPermission ipdPermission;

    /** 查询项目阶段动作列表，需 ipd:stage-action:list 权限 */
    @GetMapping
    @SaCheckPermission(value = "ipd:stage-action:list", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<StageAction>> list(@RequestParam Long projectId) {
        ipdPermission.requireInternal();
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
    @SaCheckPermission(value = "ipd:stage-action:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<StageAction> transit(@PathVariable Long id,
                                              @RequestParam String target,
                                              @RequestParam(required = false) String reason) {
        IpdActor actor = ipdPermission.requireActionWriter(() -> stageActionService.getById(id));
        return ApiV1Response.ok(stageActionService.transit(id, target, reason, String.valueOf(actor.id())));
    }

    /**
     * P1-4.1：录入轻管完成日 / BioCV FAR·FRR / 证书字段（不改 status）。
     * 轻管完成路径：先本接口写 actualDoneAt，再 /transit?target=DONE。
     */
    @PostMapping("/{id}/fields")
    @SaCheckPermission(value = "ipd:stage-action:edit", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<StageAction> recordFields(@PathVariable Long id,
                                                   @RequestBody StageActionFieldsReq req) {
        IpdActor actor = ipdPermission.requireActionWriter(() -> stageActionService.getById(id));
        StageActionFieldsReq body = req == null
            ? new StageActionFieldsReq(null, null, null, null, null, null) : req;
        return ApiV1Response.ok(stageActionService.recordFields(
            id, body.actualDoneAt(), body.farValue(), body.frrValue(),
            body.certNo(), body.certPassedAt(), body.algoType(), String.valueOf(actor.id())));
    }

    /** 深管交付物登记（BR-IPD-03 完成前置），需 ipd:stage-action:add 权限 */
    @PostMapping("/{id}/deliverables")
    @SaCheckPermission(value = "ipd:stage-action:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Deliverable> addDeliverable(@PathVariable Long id,
                                                     @RequestParam String fileName,
                                                     @RequestParam(required = false) Long ossId) {
        IpdActor actor = ipdPermission.requireActionWriter(() -> stageActionService.getById(id));
        return ApiV1Response.ok(stageActionService.addDeliverable(id, fileName, ossId, String.valueOf(actor.id())));
    }

    /** 从目录实例化某阶段动作（幂等），返回新建数量，需 ipd:stage-action:add 权限 */
    @PostMapping("/instantiate")
    @SaCheckPermission(value = "ipd:stage-action:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Integer> instantiate(@RequestParam Long projectId,
                                              @RequestParam Long stageId,
                                              @RequestParam String stage) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(stageActionService.instantiate(projectId, stageId, stage));
    }

    /**
     * P1-8.1：涉生物合规 C12 补挂（幂等）。
     * 已有 is_bio_feature=1 且缺 C12 → 挂到 CONCEPT；已有/无涉生物 → 0。
     */
    @PostMapping("/ensure-bio-compliance")
    @SaCheckPermission(value = "ipd:stage-action:add", type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Integer> ensureBioCompliance(@RequestParam Long projectId) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(stageActionService.ensureBioComplianceMount(projectId));
    }
}
