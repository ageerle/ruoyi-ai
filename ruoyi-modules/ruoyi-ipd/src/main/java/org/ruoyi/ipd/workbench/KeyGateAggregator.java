package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键评审签署任务投递（taskType=key_gate，P1 方向 B 设计 §3 表 #3）：
 * gate_reviews JOIN gates——actor 是签署人且未签（decision IS NULL），gate 在可见项目内且待决（PENDING），
 * 每行 1 张卡。期限口径：review.signDueAt（BR-GATE-04 签署期限）优先，回退 gate.signDueAt。
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class KeyGateAggregator implements WorkbenchAggregator {

    /** 与 GateReviewService.STATUS_PENDING 同值（彼处包私有不可跨包引用，勿漂移）。 */
    static final String GATE_PENDING = "PENDING";

    private final GateMapper gateMapper;
    private final GateReviewMapper gateReviewMapper;

    @Override
    public String taskType() {
        return "key_gate";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<Gate> gates = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .in(Gate::getProjectId, visibleProjects.keySet())
            .eq(Gate::getStatus, GATE_PENDING));
        if (gates == null || gates.isEmpty()) {
            return List.of();
        }
        Map<Long, Gate> gateById = new LinkedHashMap<>();
        gates.forEach(g -> gateById.put(g.getId(), g));
        List<GateReview> unsigned = gateReviewMapper.selectList(new LambdaQueryWrapper<GateReview>()
            .in(GateReview::getGateId, gateById.keySet())
            .eq(GateReview::getReviewerId, actor.id())
            .isNull(GateReview::getDecision));
        if (unsigned == null || unsigned.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (GateReview review : unsigned) {
            // 防御式双保险：SQL 已过滤 decision IS NULL，Java 侧再验（防查询条件漂移/mock 差异）
            if (review.getDecision() != null) {
                continue;
            }
            Gate gate = gateById.get(review.getGateId());
            if (gate == null) {
                continue;
            }
            tasks.add(toTask(review, gate, visibleProjects.get(gate.getProjectId()), now));
        }
        return tasks;
    }

    private Map<String, Object> toTask(GateReview review, Gate gate, Project project, Date now) {
        // 签署期限：评审行自身优先，回退 gate 实例（BR-GATE-04 submit/reopen 起算）
        Date due = review.getSignDueAt() != null ? review.getSignDueAt() : gate.getSignDueAt();
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "GR-" + review.getId());
        task.put("projectId", gate.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "GATE-SIGN-" + review.getId());
        task.put("title", "Gate 签署：" + gate.getGateCode()
            + (review.getRound() != null ? "（第 " + review.getRound() + " 轮）" : ""));
        task.put("taskType", taskType());
        task.put("status", GATE_PENDING);
        task.put("priority", due != null && due.before(now) ? "high" : "normal");
        task.put("ownerRole", review.getReviewerType());
        task.put("dueDate", due);
        task.put("isBlocking", "1"); // Gate 未放行阻断阶段流转，值域 '1'/'N'
        task.put("deepLink", "/ipd/projects/" + gate.getProjectId() + "/gates");
        return task;
    }
}
