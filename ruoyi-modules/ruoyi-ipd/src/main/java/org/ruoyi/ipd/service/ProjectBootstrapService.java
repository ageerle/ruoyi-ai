package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * P1-3.1：与项目创建同事务初始化六阶段及完整目录。
 * 锁定项目后按当前读检查完整结构；重试不重建，发现部分或错误结构时保留现场并拒绝补写。
 */
@Service
@RequiredArgsConstructor
public class ProjectBootstrapService {
    public static final String[][] STAGES = {
        {"CONCEPT", "概念", "10"}, {"PLAN", "计划", "20"},
        {"DEV", "开发", "30"}, {"VALID", "验证", "40"},
        {"LAUNCH", "发布", "50"}, {"LIFECYCLE", "生命周期", "60"}
    };

    private final ProjectStageMapper projectStageMapper;
    private final StageActionMapper stageActionMapper;

    /** 首次成功返回阶段数6，已有完整结构返回0；调用方必须已开启真实事务。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public int bootstrap(Long projectId, Long operatorId) {
        if (!positive(projectId) || !positive(operatorId)) {
            throw new ServiceException("项目及操作人ID必须为正数", ApiV1ErrorCode.PARAM_INVALID.getCode());
        }
        Project project = projectStageMapper.selectProjectForBootstrap(projectId);
        if (project == null || !Objects.equals(projectId, project.getId())
            || !"000000".equals(project.getTenantId()) || !"0".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在", ApiV1ErrorCode.NOT_FOUND.getCode());
        }
        if (project.getTemplateType() == null
            || !Set.of("HARDWARE", "SOFTWARE", "SOLUTION").contains(project.getTemplateType())) {
            throw new ServiceException("项目模板配置不合法", ApiV1ErrorCode.STATE_CONFLICT.getCode());
        }
        List<Long> allStageIds = projectStageMapper.selectAllStageIdsForBootstrap(projectId);
        List<Long> allActionIds = projectStageMapper.selectAllActionIdsForBootstrap(projectId);
        List<ProjectStage> stages = projectStageMapper.selectLiveByProject(projectId);
        List<StageAction> actions = projectStageMapper.selectActionsForBootstrap(projectId);
        if (allStageIds.size() != stages.size() || allActionIds.size() != actions.size()) throw corruptGraph();
        if (!stages.isEmpty() || !actions.isEmpty()) {
            verifyCompleteGraph(project, stages, actions);
            return 0;
        }
        Set<Long> stageIds = new HashSet<>();
        Set<Long> actionIds = new HashSet<>();
        for (String[] stageDef : STAGES) {
            ProjectStage stage = ProjectStage.builder().projectId(projectId)
                .stageCode(stageDef[0]).stageName(stageDef[1]).sortOrder(Integer.parseInt(stageDef[2]))
                .status("NOT_STARTED").tenantId("000000").delFlag("0").build();
            stage.setCreateTime(new Date());
            stage.setCreateBy(operatorId);
            stage.setUpdateBy(operatorId);
            requireInsert(projectStageMapper.insert(stage));
            requireGeneratedId(stage.getId(), stageIds);
            for (ActionDef def : ActionCatalog.byStage(stageDef[0])) {
                StageAction action = StageAction.builder().projectId(projectId).stageId(stage.getId())
                    .actionCode(def.code()).actionName(def.name()).ownerRole(def.ownerRole())
                    .depth(expectedDepth(def, project.getTemplateType())).status("NOT_STARTED")
                    .isBlocking(def.blocking() ? "1" : "0").isBioFeature(def.bioFeature() ? "1" : "0").build();
                action.setCreateTime(new Date());
                action.setCreateBy(operatorId);
                action.setUpdateBy(operatorId);
                requireInsert(stageActionMapper.insert(action));
                requireGeneratedId(action.getId(), actionIds);
            }
        }
        if (stageIds.size() != STAGES.length || actionIds.size() != ActionCatalog.ALL.size()) throw writeFailure();
        return stageIds.size();
    }

    private void verifyCompleteGraph(Project project, List<ProjectStage> stages, List<StageAction> actions) {
        if (stages.size() != STAGES.length || actions.size() != ActionCatalog.ALL.size()) throw corruptGraph();
        Map<String, ProjectStage> byCode = new HashMap<>();
        Set<Long> stageIds = new HashSet<>();
        for (ProjectStage stage : stages) {
            if (!positive(stage.getId()) || !stageIds.add(stage.getId())
                || !Objects.equals(project.getId(), stage.getProjectId())
                || !"000000".equals(stage.getTenantId()) || !"0".equals(stage.getDelFlag())
                || byCode.put(stage.getStageCode(), stage) != null) throw corruptGraph();
        }
        for (String[] def : STAGES) {
            ProjectStage stage = byCode.get(def[0]);
            if (stage == null || !Objects.equals(stage.getSortOrder(), Integer.valueOf(def[2]))) throw corruptGraph();
        }
        Map<String, ActionDef> catalog = new HashMap<>();
        ActionCatalog.ALL.forEach(def -> catalog.put(def.code(), def));
        Set<String> seenCodes = new HashSet<>();
        Set<Long> actionIds = new HashSet<>();
        for (StageAction action : actions) {
            String code = action.getActionCode() == null ? null : ActionCatalog.resolveCode(action.getActionCode());
            ActionDef def = catalog.get(code);
            if (def == null || !seenCodes.add(code) || !positive(action.getId()) || !actionIds.add(action.getId())
                || !Objects.equals(project.getId(), action.getProjectId())
                || !Objects.equals(byCode.get(def.stage()).getId(), action.getStageId())
                || !def.ownerRole().equals(action.getOwnerRole())
                || !expectedDepth(def, project.getTemplateType()).equals(action.getDepth())
                || !Objects.equals(def.blocking() ? "1" : "0", action.getIsBlocking())
                || !Objects.equals(def.bioFeature() ? "1" : "0", action.getIsBioFeature())) throw corruptGraph();
        }
    }

    private static String expectedDepth(ActionDef def, String templateType) {
        // BR-IPD-05：普通轻管不增加强制附件，仅SOLUTION的V11/Z04按模板转深管。
        return "V11".equals(def.code()) && "SOLUTION".equals(templateType) ? "DEEP" : def.depth();
    }

    private static boolean positive(Long id) { return id != null && id > 0; }
    private static void requireInsert(int count) { if (count != 1) throw writeFailure(); }
    private static void requireGeneratedId(Long id, Set<Long> ids) {
        if (!positive(id) || !ids.add(id)) throw writeFailure();
    }
    private static ServiceException writeFailure() {
        return new ServiceException("项目初始化记录写入或主键回填失败", ApiV1ErrorCode.INTERNAL_ERROR.getCode());
    }
    private static ServiceException corruptGraph() {
        return new ServiceException("项目初始化结构不完整或与目录不一致", ApiV1ErrorCode.STATE_CONFLICT.getCode());
    }
}
