package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.CoefficientChangeRequest;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.CoefficientChangeRequestMapper;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 战略变更任务投递（taskType=strategic_change，P1 方向 B 设计 §3 表 #7）：
 * 双表合并——上市日期变更双签（confirmer 待确认，PENDING_SECOND）∪ 差异化系数定值（组长待确认，PENDING_LEADER）。
 * 双表均无期限字段 → dueDate=null 恒 normal；deepLink 指向项目详情变更页。
 */
@Component
@Order(8)
@RequiredArgsConstructor
public class StrategicChangeAggregator implements WorkbenchAggregator {

    private static final String LD_PENDING = LaunchDateChangeRequest.ST_PENDING_SECOND;
    private static final String CC_PENDING = CoefficientChangeRequest.ST_PENDING_LEADER;

    private final LaunchDateChangeRequestMapper launchDateChangeMapper;
    private final CoefficientChangeRequestMapper coefficientChangeMapper;

    @Override
    public String taskType() {
        return "strategic_change";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 上市日期变更：待第二签人确认
        List<LaunchDateChangeRequest> launches = launchDateChangeMapper.selectList(
            new LambdaQueryWrapper<LaunchDateChangeRequest>()
                .in(LaunchDateChangeRequest::getProjectId, visibleProjects.keySet())
                .eq(LaunchDateChangeRequest::getStatus, LD_PENDING));
        if (launches != null) {
            for (LaunchDateChangeRequest request : launches) {
                if (!actor.id().equals(request.getConfirmerId())) {
                    continue; // 防御式双保险：非第二签人不投
                }
                if (!LD_PENDING.equals(request.getStatus())) {
                    continue; // 防御式双保险：非待确认（已双签/拒绝）不投（防查询条件漂移/mock 差异）
                }
                tasks.add(launchTask(request, visibleProjects.get(request.getProjectId())));
            }
        }

        // 差异化系数变更：待产品组长确认
        List<CoefficientChangeRequest> coefficients = coefficientChangeMapper.selectList(
            new LambdaQueryWrapper<CoefficientChangeRequest>()
                .in(CoefficientChangeRequest::getProjectId, visibleProjects.keySet())
                .eq(CoefficientChangeRequest::getStatus, CC_PENDING));
        if (coefficients != null) {
            for (CoefficientChangeRequest request : coefficients) {
                if (!actor.id().equals(request.getLeaderId())) {
                    continue; // 防御式双保险：非该组组长不投
                }
                if (!CC_PENDING.equals(request.getStatus())) {
                    continue; // 防御式双保险：非待确认（已定值/拒绝）不投（防查询条件漂移/mock 差异）
                }
                tasks.add(coefficientTask(request, visibleProjects.get(request.getProjectId())));
            }
        }
        return tasks;
    }

    private Map<String, Object> launchTask(LaunchDateChangeRequest request, Project project) {
        String targetDate = request.getProposedLaunchDate() != null
            ? new SimpleDateFormat("yyyy-MM-dd").format(request.getProposedLaunchDate())
            : "待定";
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "LD-" + request.getId());
        task.put("projectId", request.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "LAUNCH-DATE-" + request.getId());
        task.put("title", "上市日期变更待确认 → " + targetDate);
        task.put("taskType", taskType());
        task.put("status", request.getStatus());
        task.put("priority", "normal"); // 双表无期限字段，恒 normal
        task.put("ownerRole", request.getConfirmerRole());
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 未双签则 projects.launch_date 未落定，值域 '1'/'N'
        task.put("deepLink", "/ipd/projects/" + request.getProjectId() + "/changes");
        return task;
    }

    private Map<String, Object> coefficientTask(CoefficientChangeRequest request, Project project) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "CC-" + request.getId());
        task.put("projectId", request.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "COEFFICIENT-" + request.getId());
        task.put("title", "差异化系数变更待组长确认 → " + request.getProposedCoefficient());
        task.put("taskType", taskType());
        task.put("status", request.getStatus());
        task.put("priority", "normal"); // 双表无期限字段，恒 normal
        task.put("ownerRole", null); // 组长确认是具体人职责（leader_id），非角色
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 未定值则 projects.level_coefficient 未落定，值域 '1'/'N'
        task.put("deepLink", "/ipd/projects/" + request.getProjectId() + "/changes");
        return task;
    }
}
