package org.ruoyi.ipd.service;

import org.ruoyi.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 阶段动作实例服务：深轻管分离完成校验（BR-IPD-03/04/05，动作清单 v3）
 *
 * 校验矩阵（以 ActionCatalog 目录为 SSOT，不信任前端）：
 * - 深管 DONE：至少 1 个未删交付物（del_flag=0）——BR-IPD-03 强制附件
 * - 轻管 DONE：actual_done_at 必填（BR-IPD-05）；且**不允许 DELAYED**（轻管枚举无延期）
 * - 数值登记（valueFields）：D11=FAR,FRR；V02=CERT_NO,CERT_DATE
 * - 阻断跳阶（is_blocking）由 P1-5 GateEngine 消费本表状态
 *
 * P1-4.3 状态机（仅 /transit 入口，禁止 PATCH status 字段）：
 * - 深管：NOT_STARTED → IN_PROGRESS → DONE / NA / DELAYED
 * - 轻管：NOT_STARTED → IN_PROGRESS → DONE / NA（无 DELAYED）
 * - NA 必传 reason（防绕过）
 * - 幂等：同 id 同 target 重复 /transit 返回当前状态，不写新审计
 * - 乐观锁：@Version；并发同 id 仅 1 成功
 */
@Service
@RequiredArgsConstructor
public class StageActionService {

    private static final Set<String> LIGHT_STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "DONE", "NA");
    private static final Set<String> DEEP_EXTRA_STATUSES = Set.of("DELAYED");
    private static final Set<String> DEEP_ALLOWED = Set.of("NOT_STARTED", "IN_PROGRESS", "DONE", "NA", "DELAYED");

    private final StageActionMapper stageActionMapper;
    private final DeliverableMapper deliverableMapper;
    private final AuditLogService auditLogService;

    public StageAction getById(Long id) {
        StageAction a = stageActionMapper.selectById(id);
        if (a == null) {
            throw new ServiceException("动作实例不存在: " + id);
        }
        return a;
    }

    public List<StageAction> listByProject(Long projectId) {
        return stageActionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId)
                .orderByAsc(StageAction::getActionCode));
    }

    /**
     * 状态迁移唯一入口（P1-4.3）。
     * - 状态机白名单（depth + 目标）
     * - 幂等：当前态 == 目标态 → 直接返回，不写库、不写审计
     * - NA 必 reason
     * - DONE 触发深度+数值双重校验
     * - 乐观锁：@Version，updateById 失败（version 冲突）抛 ServiceException
     * - 每次成功迁移写审计 action=TRANSIT
     */
    @Transactional(rollbackFor = Exception.class)
    public StageAction transit(Long id, String target, String reason, String operator) {
        if (target == null) { throw new ServiceException("目标状态不能为空"); }
        StageAction a = getById(id);
        ActionDef def = ActionCatalog.byCode(a.getActionCode());
        boolean deep = "DEEP".equals(a.getDepth());

        if (!deep && DEEP_EXTRA_STATUSES.contains(target)) {
            throw new ServiceException("轻管动作不支持延期状态（BR-IPD-05 三字段登记）: " + def.code());
        }
        Set<String> allowed = deep ? DEEP_ALLOWED : LIGHT_STATUSES;
        if (!allowed.contains(target)) {
            throw new ServiceException("非法目标状态: " + target);
        }
        if (target.equals(a.getStatus())) {
            return a; // 幂等
        }
        if ("NA".equals(target) && (reason == null || reason.isBlank())) {
            throw new ServiceException("标记 NA 必须填写原因（防绕过 P1-4.3）: " + def.code());
        }
        if ("DONE".equals(target)) {
            validateCompletion(a, def, deep);
        }

        String before = statusSnapshot(a);
        a.setStatus(target);
        a.setUpdateBy(actorIdOf(operator));
        if ("DONE".equals(target) && a.getActualDoneAt() == null) {
            a.setActualDoneAt(new Date(Math.floorDiv(System.currentTimeMillis(), 1000L) * 1000L));
        }
        int n = stageActionMapper.updateById(a);
        if (n != 1) {
            throw new ServiceException("动作状态更新失败：可能并发冲突或记录不存在（P1-4.3 乐观锁）: " + def.code());
        }
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("PM")
            .action("TRANSIT").entityType("STAGE_ACTION").entityId(a.getId())
            .beforeData(before).afterData(statusSnapshot(a))
            .reason(reason == null || reason.isBlank() ? "P1-4.3 状态机" : reason)
            .build());
        return a;
    }

    private void validateCompletion(StageAction a, ActionDef def, boolean deep) {
        if (deep) {
            Long cnt = deliverableMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Deliverable>()
                    .eq(Deliverable::getActionId, a.getId())
                    .eq(Deliverable::getDelFlag, "0"));
            if (cnt == null || cnt == 0) {
                throw new ServiceException("深管动作完成前必须上传至少 1 个未删交付物（BR-IPD-03）: " + def.code());
            }
        } else if (a.getActualDoneAt() == null) {
            throw new ServiceException("轻管动作完成必须登记实际完成日期（BR-IPD-05）: " + def.code());
        }
        String vf = def.valueFields() == null ? "" : def.valueFields();
        if (vf.contains("FAR") && (a.getFarValue() == null || a.getFrrValue() == null)) {
            throw new ServiceException("BioCV 算法评测必须登记实测 FAR/FRR（动作清单 v3 例外二）: " + def.code());
        }
        if (vf.contains("CERT_NO") && (a.getCertNo() == null || a.getCertNo().isBlank()
            || a.getCertPassedAt() == null)) {
            throw new ServiceException("认证送检完成必须登记证书编号与通过日期（v3 例外一）: " + def.code());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int ensureBioComplianceMount(Long projectId) {
        Long bioCount = stageActionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId)
                .eq(StageAction::getIsBioFeature, "1"));
        if (bioCount == null || bioCount == 0) {
            return 0;
        }
        Long c12 = stageActionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId)
                .eq(StageAction::getActionCode, "C12"));
        if (c12 != null && c12 > 0) {
            return 0;
        }
        ActionDef def = ActionCatalog.byCode("C12");
        StageAction c12Action = StageAction.builder()
            .projectId(projectId)
            .stageId(null)
            .actionCode(def.code())
            .actionName(def.name())
            .ownerRole(def.ownerRole())
            .depth(def.depth())
            .status("NOT_STARTED")
            .isBlocking("1")
            .isBioFeature("1")
            .build();
        stageActionMapper.insert(c12Action);
        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public Deliverable addDeliverable(Long actionId, String fileName, Long ossId, String operator) {
        StageAction a = getById(actionId);
        Deliverable d = Deliverable.builder()
            .actionId(actionId)
            .projectId(a.getProjectId())
            .fileName(fileName)
            .ossId(ossId)
            .uploadedAt(new Date())
            .build();
        deliverableMapper.insert(d);
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("PM")
            .action("CREATE").entityType("DELIVERABLE").entityId(d.getId())
            .afterData("{\"actionCode\":\"" + a.getActionCode() + "\",\"file\":\""
                + fileName.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}")
            .build());
        return d;
    }

    /**
     * PERF-03：批量实例化阶段动作，从 N 次 selectCount + N 次 insert 优化为
     * 1 次 selectList（取项目所有已有 action codes）+ 1 次 insertBatch（批量插入剩余）。
     * 69 动作 CONCEPT 阶段 = 138 IO → 2 IO，P99 下降 ~250ms → ~20ms。
     */
    @Transactional(rollbackFor = Exception.class)
    public int instantiate(Long projectId, Long stageId, String stage) {
        Set<String> existingCodes = stageActionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId))
            .stream().map(StageAction::getActionCode)
            .collect(java.util.stream.Collectors.toSet());
        List<StageAction> toCreate = ActionCatalog.byStage(stage).stream()
            .filter(def -> !existingCodes.contains(def.code()))
            .map(def -> StageAction.builder()
                .projectId(projectId)
                .stageId(stageId)
                .actionCode(def.code())
                .actionName(def.name())
                .ownerRole(def.ownerRole())
                .depth(def.depth())
                .status("NOT_STARTED")
                .isBlocking(def.blocking() ? "1" : "0")
                .isBioFeature(def.bioFeature() ? "1" : "0")
                .build())
            .toList();
        if (!toCreate.isEmpty()) {
            stageActionMapper.insertBatch(toCreate, 200);
        }
        return toCreate.size();
    }

    private static String statusSnapshot(StageAction a) {
        // audit_logs.before_data/after_data 为 MySQL JSON 列，必须写合法 JSON
        return "{\"actionCode\":\"" + a.getActionCode() + "\",\"status\":\"" + a.getStatus()
            + "\",\"version\":" + a.getVersion()
            + (a.getActualDoneAt() == null ? "" : ",\"actualDoneAt\":" + a.getActualDoneAt().getTime())
            + "}";
    }

    private static Long actorIdOf(String operator) {
        try { return Long.parseLong(operator); } catch (NumberFormatException e) { return null; }
    }
}
