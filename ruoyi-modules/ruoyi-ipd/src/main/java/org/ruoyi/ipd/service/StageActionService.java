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
 * - 深管 DONE：至少 1 个交付物（deliverables 表未删记录）——BR-IPD-03 强制附件
 * - 轻管 DONE：actual_done_at 必填（三字段登记 BR-IPD-05）；且**不允许 DELAYED**（轻管枚举无延期）
 * - 数值登记（valueFields）：D11=FAR,FRR；V02=CERT_NO,CERT_DATE；L08=项目上市日期前置校验（P1-1 已有 advanceStage 前置，此处不重复）
 * - 阻断跳阶（is_blocking）由 P1-5 GateEngine 消费本表状态，此处只保证状态真实
 */
@Service
@RequiredArgsConstructor
public class StageActionService {

    private static final Set<String> LIGHT_STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "DONE", "NA");
    private static final Set<String> DEEP_EXTRA_STATUSES = Set.of("DELAYED");

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

    /** 状态流转：NOT_STARTED -> IN_PROGRESS -> DONE/NA（深管另有 DELAYED）；DONE 触发深度+数值双重校验 */
    @Transactional
    public StageAction transit(Long id, String target, String operator) {
        StageAction a = getById(id);
        ActionDef def = ActionCatalog.byCode(a.getActionCode());
        boolean deep = "DEEP".equals(a.getDepth());
        if (!deep && DEEP_EXTRA_STATUSES.contains(target)) {
            throw new ServiceException("轻管动作不支持延期状态（BR-IPD-05 三字段登记）: " + def.code());
        }
        Set<String> allowed = deep ? Set.of("NOT_STARTED", "IN_PROGRESS", "DONE", "NA", "DELAYED") : LIGHT_STATUSES;
        if (!allowed.contains(target)) {
            throw new ServiceException("非法目标状态: " + target);
        }
        if ("DONE".equals(target)) {
            validateCompletion(a, def, deep);
        }
        a.setStatus(target);
        if ("DONE".equals(target)) {
            if (a.getActualDoneAt() == null) {
                a.setActualDoneAt(new Date());
            }
        }
        int n = stageActionMapper.updateById(a);
        if (n != 1) {
            throw new ServiceException("动作状态更新失败: " + def.code());
        }
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("PM")
            .action("UPDATE").entityType("STAGE_ACTION").entityId(a.getId())
            .afterData("status=" + target).reason("BR-IPD-03/04/05")
            .build());
        return a;
    }

    /** 完成前校验：深度规则 + 目录 valueFields 数值登记 */
    private void validateCompletion(StageAction a, ActionDef def, boolean deep) {
        if (deep) {
            Long cnt = deliverableMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Deliverable>()
                    .eq(Deliverable::getActionId, a.getId()));
            if (cnt == null || cnt == 0) {
                throw new ServiceException("深管动作完成前必须上传至少 1 个交付物（BR-IPD-03）: " + def.code());
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

    /** 交付物登记（深管附件上传的最小真实落点；OSS 集成后补 fileUrl） */
    @Transactional
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
            .afterData("action=" + a.getActionCode() + ";file=" + fileName)
            .build());
        return d;
    }

    /** 实例化：按项目 + 阶段从目录批量生成动作实例（幂等：同项目同编码跳过） */
    @Transactional
    public int instantiate(Long projectId, Long stageId, String stage) {
        int created = 0;
        for (ActionDef def : ActionCatalog.byStage(stage)) {
            Long exists = stageActionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageAction>()
                    .eq(StageAction::getProjectId, projectId)
                    .eq(StageAction::getActionCode, def.code()));
            if (exists != null && exists > 0) {
                continue;
            }
            StageAction a = StageAction.builder()
                .projectId(projectId)
                .stageId(stageId)
                .actionCode(def.code())
                .actionName(def.name())
                .ownerRole(def.ownerRole())
                .depth(def.depth())
                .status("NOT_STARTED")
                .isBlocking(def.blocking() ? "1" : "0")
                .isBioFeature(def.bioFeature() ? "1" : "0")
                .build();
            stageActionMapper.insert(a);
            created++;
        }
        return created;
    }
}