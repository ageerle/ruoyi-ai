package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
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
 * actor 是仲裁人（arbitratorId）且未裁（decision IS NULL），gate 在可见项目内且待决（PENDING）。
 * 仲裁表无期限字段（gate_arbitrations.dueAt 不存在）→ dueDate=null，不计 overdue。
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class KeyGateArbitrationAggregator implements WorkbenchAggregator {

    private final GateMapper gateMapper;
    private final GateArbitrationMapper gateArbitrationMapper;

    @Override
    public String taskType() {
        return "key_gate_arbitration";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<Gate> gates = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .in(Gate::getProjectId, visibleProjects.keySet())
            .eq(Gate::getStatus, KeyGateAggregator.GATE_PENDING));
        if (gates == null || gates.isEmpty()) {
            return List.of();
        }
        Map<Long, Gate> gateById = new LinkedHashMap<>();
        gates.forEach(g -> gateById.put(g.getId(), g));
        List<GateArbitration> undecided = gateArbitrationMapper.selectList(
            new LambdaQueryWrapper<GateArbitration>()
                .in(GateArbitration::getGateId, gateById.keySet())
                .eq(GateArbitration::getArbitratorId, actor.id())
                .isNull(GateArbitration::getDecision));
        if (undecided == null || undecided.isEmpty()) {
            return List.of();
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
            tasks.add(toTask(arbitration, gate, visibleProjects.get(gate.getProjectId())));
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
