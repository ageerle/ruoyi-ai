package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gate 冲突仲裁任务投递（taskType=key_gate_arbitration，P1 方向 B 设计 §3 表 #4）：
 * actor 是仲裁人（arbitratorId）且未裁（decision IS NULL，开仲裁时预落待裁行），
 * gate 已被驳回（REJECTED——仲裁仅存在于被驳回的 Gate，见
 * GateReviewService.requireArbitratable；2026-09-08 修正：旧代码照抄签署态 PENDING 是错的）。
 * 仲裁表无期限字段（gate_arbitrations.dueAt 不存在）→ dueDate=null，不计 overdue。
 *
 * <p>可见性锚点（2026-09-08 真活验证修正）：仲裁是「指派给组长的个人任务」，不是「我的项目」任务——
 * 组长通常不是 project_members 成员，若按 visibleProjects（成员关系）过滤，仲裁卡对组长永不出现
 * （gate 999101 真活链路实证：通知/预落行都落库，卡却不投递）。故以 arbitratorId 直查仲裁行为锚，
 * 项目信息仅用于卡面展示（visibleProjects 命中优先复用，miss 时补查 ACTIVE 项目）。
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class KeyGateArbitrationAggregator implements WorkbenchAggregator {

    /** 与 GateReviewService.STATUS_REJECTED 同值（包私有不可直引，自带防漂移，见 KpiFillAggregator.KF_EDITING 先例）。 */
    static final String GATE_REJECTED = "REJECTED";

    /** 与 WorkbenchService.ST_ACTIVE_PROJECT 同值（包私有不可直引，自带防漂移）。 */
    static final String ST_ACTIVE_PROJECT = "ACTIVE";

    private final GateMapper gateMapper;
    private final GateArbitrationMapper gateArbitrationMapper;
    private final ProjectMapper projectMapper;

    @Override
    public String taskType() {
        return "key_gate_arbitration";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        List<GateArbitration> undecided = gateArbitrationMapper.selectList(
            new LambdaQueryWrapper<GateArbitration>()
                .eq(GateArbitration::getArbitratorId, actor.id())
                .isNull(GateArbitration::getDecision));
        if (undecided == null || undecided.isEmpty()) {
            return List.of();
        }
        List<Gate> gates = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .in(Gate::getId, undecided.stream().map(GateArbitration::getGateId).distinct().toList())
            .eq(Gate::getStatus, GATE_REJECTED));
        if (gates == null || gates.isEmpty()) {
            return List.of();
        }
        Map<Long, Gate> gateById = new LinkedHashMap<>();
        gates.forEach(g -> gateById.put(g.getId(), g));
        // 卡面项目信息：visibleProjects 命中优先复用；miss（组长非成员项目）补查 ACTIVE 项目
        Map<Long, Project> projectById = new LinkedHashMap<>(visibleProjects);
        List<Long> missing = gates.stream().map(Gate::getProjectId).distinct()
            .filter(pid -> !projectById.containsKey(pid)).toList();
        if (!missing.isEmpty()) {
            List<Project> extra = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .in(Project::getId, missing)
                .eq(Project::getStatus, ST_ACTIVE_PROJECT));
            if (extra != null) {
                extra.forEach(p -> projectById.put(p.getId(), p));
            }
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (GateArbitration arbitration : undecided) {
            // 防御式双保险：SQL 已过滤 decision IS NULL，Java 侧再验（防查询条件漂移/mock 差异）
            if (arbitration.getDecision() != null) {
                continue;
            }
            Gate gate = gateById.get(arbitration.getGateId());
            if (gate == null) {
                continue;
            }
            // 防御式双保险：SQL 已过滤 REJECTED，Java 侧再验（防查询条件漂移/mock 差异）——
            // 签署中（PENDING）的 gate 不存在仲裁，投了就是查询语义错配
            if (!GATE_REJECTED.equals(gate.getStatus())) {
                continue;
            }
            // 防御式双保险：旧轮残留的未裁行不算当前待办（openArbitration 每轮预落新行，
            // round 应等于 gate 当前轮；任一侧 round 为空的存量数据不拦）
            if (gate.getCurrentRound() != null && arbitration.getRound() != null
                && !gate.getCurrentRound().equals(arbitration.getRound())) {
                continue;
            }
            // 项目非 ACTIVE（归档/终止）不投，与工作台「只展示进行中项目」口径对齐
            Project project = projectById.get(gate.getProjectId());
            if (project == null) {
                continue;
            }
            tasks.add(toTask(arbitration, gate, project));
        }
        return tasks;
    }

    private Map<String, Object> toTask(GateArbitration arbitration, Gate gate, Project project) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "GA-" + arbitration.getId());
        task.put("projectId", gate.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "GATE-ARB-" + arbitration.getId());
        task.put("title", "Gate 仲裁：" + gate.getGateCode()
            + (arbitration.getRound() != null ? "（第 " + arbitration.getRound() + " 轮）" : ""));
        task.put("taskType", taskType());
        task.put("status", KeyGateAggregator.GATE_PENDING);
        task.put("priority", "normal"); // 仲裁行无期限字段，恒 normal
        task.put("ownerRole", arbitration.getArbitratorType());
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 仲裁未决阻断 Gate 放行，值域 '1'/'N'
        task.put("deepLink", "/ipd/projects/" + gate.getProjectId() + "/gates");
        return task;
    }
}
