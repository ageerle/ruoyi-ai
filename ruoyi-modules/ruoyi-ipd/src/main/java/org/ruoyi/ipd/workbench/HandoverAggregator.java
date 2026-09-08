package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移交任务投递（taskType=handover，P1 方向 B 设计 §3 表 #5）：
 * actor 是移交人或承接人，且记录未完成（DRAFT 待确认 / CONFIRMED 已确认未完成），每行 1 卡。
 * 期限：无字段 → dueDate=null 恒 normal；projectId 为空的 SUPER_ADMIN/BATCH 移交暂不投递（spec 未定口径）。
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class HandoverAggregator implements WorkbenchAggregator {

    /** 与 HandoverService.ST_DRAFT/ST_CONFIRMED 同值（彼处 private，勿漂移；实体注释 DRAFT|CONFIRMED|COMPLETED）。 */
    static final String HS_DRAFT = "DRAFT";
    static final String HS_CONFIRMED = "CONFIRMED";

    private final HandoverMapper handoverMapper;

    @Override
    public String taskType() {
        return "handover";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        List<HandoverRecord> records = handoverMapper.selectList(new LambdaQueryWrapper<HandoverRecord>()
            .in(HandoverRecord::getProjectId, visibleProjects.keySet())
            .in(HandoverRecord::getStatus, List.of(HS_DRAFT, HS_CONFIRMED)));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (HandoverRecord record : records) {
            boolean isTo = actor.id().equals(record.getToPersonId());
            boolean isFrom = actor.id().equals(record.getFromPersonId());
            if (!isTo && !isFrom) {
                continue; // 防御式双保险：非当事人不投
            }
            if (!HS_DRAFT.equals(record.getStatus()) && !HS_CONFIRMED.equals(record.getStatus())) {
                continue; // 防御式双保险：COMPLETED 已完结不投（防查询条件漂移/mock 差异）
            }
            tasks.add(toTask(record, visibleProjects.get(record.getProjectId()), isTo));
        }
        return tasks;
    }

    private Map<String, Object> toTask(HandoverRecord record, Project project, boolean receiverSide) {
        String roleSuffix = record.getHandoverRole() != null ? "（" + record.getHandoverRole() + "）" : "";
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "HD-" + record.getId());
        task.put("projectId", record.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "HANDOVER-" + record.getId());
        task.put("title", (receiverSide ? "移交确认：" : "移交跟进：")
            + ("PROJECT".equals(record.getHandoverType()) ? "项目" : record.getHandoverType()) + roleSuffix);
        task.put("taskType", taskType());
        task.put("status", record.getStatus());
        task.put("priority", "normal"); // 无期限字段，恒 normal
        task.put("ownerRole", record.getHandoverRole());
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 移交未完成阻断权限/数据切换，值域 '1'/'N'
        task.put("deepLink", "/ipd/handover");
        return task;
    }
}
