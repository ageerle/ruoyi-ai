package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段签署任务投递（taskType=stage_sign）：ipd_stage_actions 按项目范围 + 责任角色过滤。
 * 责任匹配与 OPEN 态口径从 WorkbenchService 原地迁入（P0 行为不变量，P1.1 仅搬家不改语义）。
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class StageSignAggregator implements WorkbenchAggregator {

    private static final String ST_DONE = "DONE";
    private static final String ST_NA = "NA";

    private final StageActionMapper stageActionMapper;

    @Override
    public String taskType() {
        return "stage_sign";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        if (visibleProjects.isEmpty()) {
            return tasks;
        }
        for (StageAction action : loadAll(visibleProjects)) {
            if (!ST_DONE.equals(action.getStatus()) && !ST_NA.equals(action.getStatus())
                && WorkbenchPolicy.OPEN_STATUSES.contains(action.getStatus())
                && WorkbenchPolicy.isMine(action.getOwnerRole(), actor.role())) {
                tasks.add(toTask(action, visibleProjects.get(action.getProjectId()), now));
            }
        }
        return tasks;
    }

    @Override
    public int completedCount(IpdActor actor, Map<Long, Project> visibleProjects) {
        if (visibleProjects.isEmpty()) {
            return 0;
        }
        return (int) loadAll(visibleProjects).stream()
            .filter(a -> ST_DONE.equals(a.getStatus()))
            .count();
    }

    private List<StageAction> loadAll(Map<Long, Project> visibleProjects) {
        return stageActionMapper.selectList(
            new LambdaQueryWrapper<StageAction>().in(StageAction::getProjectId, visibleProjects.keySet()));
    }

    private Map<String, Object> toTask(StageAction action, Project project, Date now) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", action.getId());
        task.put("projectId", action.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", action.getActionCode());
        task.put("title", action.getActionName());
        task.put("taskType", taskType());
        task.put("status", action.getStatus());
        task.put("priority", action.getDueDate() != null && action.getDueDate().before(now) ? "high" : "normal");
        task.put("ownerRole", action.getOwnerRole());
        task.put("dueDate", action.getDueDate());
        task.put("isBlocking", action.getIsBlocking());
        task.put("deepLink", "/ipd/projects/" + action.getProjectId() + "/actions/" + action.getId());
        return task;
    }
}
