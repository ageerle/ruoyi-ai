package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectScoreTask;
import org.ruoyi.ipd.mapper.ProjectScoreTaskMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目收尾绩效待办投递（taskType=closeout，P1 方向 B 设计 §3 表 #9）：
 * actor 是被评定人（personId），待办 PENDING（SELF_SCORING 上市30日 / LEADER_REVIEW 上市90日），每行 1 卡。
 * 期限：task.dueAt（BR 上市日期+30/90）；deepLink 用前端真路由 /ipd/kpi/project-score
 * （task.actionUrl 是 legacy /project-scores/*，非真路由，不采用）。
 */
@Component
@Order(7)
@RequiredArgsConstructor
public class CloseoutAggregator implements WorkbenchAggregator {

    /** 与 ProjectScoreScheduleService.TYPE_SELF/TYPE_LEADER 同值（彼处 private，勿漂移）。 */
    static final String TYPE_SELF = "SELF_SCORING";
    static final String TYPE_LEADER = "LEADER_REVIEW";
    private static final String STATUS_PENDING = "PENDING";

    private final ProjectScoreTaskMapper projectScoreTaskMapper;

    @Override
    public String taskType() {
        return "closeout";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<ProjectScoreTask> pending = projectScoreTaskMapper.selectList(
            new LambdaQueryWrapper<ProjectScoreTask>()
                .in(ProjectScoreTask::getProjectId, visibleProjects.keySet())
                .eq(ProjectScoreTask::getPersonId, actor.id())
                .in(ProjectScoreTask::getTargetType, List.of(TYPE_SELF, TYPE_LEADER))
                .eq(ProjectScoreTask::getStatus, STATUS_PENDING));
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (ProjectScoreTask task : pending) {
            if (!actor.id().equals(task.getPersonId())) {
                continue; // 防御式双保险：非被评定人不投（防查询条件漂移/mock 差异）
            }
            tasks.add(toTask(task, visibleProjects.get(task.getProjectId()), now));
        }
        return tasks;
    }

    private Map<String, Object> toTask(ProjectScoreTask task, Project project, Date now) {
        Date due = task.getDueAt();
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", "PS-" + task.getId());
        card.put("projectId", task.getProjectId());
        card.put("projectName", project != null ? project.getName() : null);
        card.put("projectCode", project != null ? project.getCode() : null);
        card.put("actionCode", "SCORE-TASK-" + task.getId());
        card.put("title", TYPE_LEADER.equals(task.getTargetType()) ? "项目绩效组长评定" : "项目绩效自评");
        card.put("taskType", taskType());
        card.put("status", task.getStatus());
        card.put("priority", due != null && due.before(now) ? "high" : "normal");
        card.put("ownerRole", null);
        card.put("dueDate", due);
        card.put("isBlocking", "1"); // 收尾评定未完成阻断项目关闭，值域 '1'/'N'
        card.put("deepLink", "/ipd/kpi/project-score");
        return card;
    }
}
