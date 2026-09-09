package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 贡献度确认任务投递（taskType=contribution_confirm，P1 方向 B 设计 §3 表 #6）：
 * actor 是评定组长（leaderId）且评定已提交待确认（SUBMITTED），每行 1 卡。
 * 期限：无字段 → dueDate=null 恒 normal；组长是具体人（leaderId 匹配），非按角色泛配。
 */
@Component
@Order(6)
@RequiredArgsConstructor
public class ContributionConfirmAggregator implements WorkbenchAggregator {

    private final ContributionMapper contributionMapper;

    @Override
    public String taskType() {
        return "contribution_confirm";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<Contribution> pending = contributionMapper.selectList(new LambdaQueryWrapper<Contribution>()
            .in(Contribution::getProjectId, visibleProjects.keySet())
            .eq(Contribution::getStatus, Contribution.ST_SUBMITTED));
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Contribution contribution : pending) {
            if (!actor.id().equals(contribution.getLeaderId())) {
                continue; // 防御式双保险：非该评定组长不投
            }
            if (!Contribution.ST_SUBMITTED.equals(contribution.getStatus())) {
                continue; // 防御式双保险：非 SUBMITTED（已确认等）不投（防查询条件漂移/mock 差异）
            }
            tasks.add(toTask(contribution, visibleProjects.get(contribution.getProjectId())));
        }
        return tasks;
    }

    private Map<String, Object> toTask(Contribution contribution, Project project) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "CT-" + contribution.getId());
        task.put("projectId", contribution.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "CONTRIBUTION-" + contribution.getId());
        task.put("title", "贡献度确认（双 PM 自评待组长评定）");
        task.put("taskType", taskType());
        task.put("status", contribution.getStatus());
        task.put("priority", "normal"); // 无期限字段，恒 normal
        task.put("ownerRole", null); // 组长确认是具体人职责，非角色
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 未确认则 tierCoefficient 未落定，阻断奖金池计算，值域 '1'/'N'
        task.put("deepLink", "/ipd/incentive/contribution");
        return task;
    }
}
