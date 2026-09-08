package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
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
 * KPI 月度填报任务投递（taskType=kpi_fill，P1 方向 B 设计 §3 表 #8）：
 * actor 是被考核人（personId），period=当月且状态 EDITING（编辑中未提交），每行 1 卡。
 * 设计文档 §3 原写 status='DRAFT'——实体实际值域 EDITING/PENDING_REVIEW/APPROVED/...（无 DRAFT），以代码为准。
 * 期限：kpi_records 无 dueAt 字段（月度截止日由 kpi.monthlyDeadlineDay 配置控制，聚合器不读配置）→ dueDate=null 恒 normal。
 */
@Component
@Order(9)
@RequiredArgsConstructor
public class KpiFillAggregator implements WorkbenchAggregator {

    /** 与 KpiRecordService.ST_EDITING 同值（彼处 public 但为避免 service↔workbench 包互引，自带同值，勿漂移）。 */
    static final String KF_EDITING = "EDITING";
    private static final String KPI_TYPE_SHARED = "SHARED";

    private final KpiRecordMapper kpiRecordMapper;

    @Override
    public String taskType() {
        return "kpi_fill";
    }

    @Override
    public List<Map<String, Object>> collect(IpdActor actor, Map<Long, Project> visibleProjects, Date now) {
        if (visibleProjects.isEmpty()) {
            return List.of();
        }
        String currentPeriod = new SimpleDateFormat("yyyy-MM").format(now);
        List<KpiRecord> records = kpiRecordMapper.selectList(new LambdaQueryWrapper<KpiRecord>()
            .in(KpiRecord::getProjectId, visibleProjects.keySet())
            .eq(KpiRecord::getPersonId, actor.id())
            .eq(KpiRecord::getPeriod, currentPeriod)
            .eq(KpiRecord::getStatus, KF_EDITING));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (KpiRecord record : records) {
            if (!actor.id().equals(record.getPersonId())) {
                continue; // 防御式双保险：非被考核人不投
            }
            if (!currentPeriod.equals(record.getPeriod()) || !KF_EDITING.equals(record.getStatus())) {
                continue; // 防御式双保险：非当月或非编辑中不投（防查询条件漂移/mock 差异）
            }
            tasks.add(toTask(record, visibleProjects.get(record.getProjectId())));
        }
        return tasks;
    }

    private Map<String, Object> toTask(KpiRecord record, Project project) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", "KF-" + record.getId());
        task.put("projectId", record.getProjectId());
        task.put("projectName", project != null ? project.getName() : null);
        task.put("projectCode", project != null ? project.getCode() : null);
        task.put("actionCode", "KPI-FILL-" + record.getId());
        task.put("title", "KPI 月度填报：" + record.getPeriod()
            + ("SHARED".equals(record.getKpiType()) ? "（共担归集）" : "（功能）"));
        task.put("taskType", taskType());
        task.put("status", record.getStatus());
        task.put("priority", "normal"); // 无期限字段，恒 normal
        task.put("ownerRole", null); // 填报是被考核人具体义务，非角色
        task.put("dueDate", null);
        task.put("isBlocking", "1"); // 未填报阻断月度评分归集，值域 '1'/'N'
        task.put("deepLink", KPI_TYPE_SHARED.equals(record.getKpiType())
            ? "/ipd/kpi/shared"
            : "/ipd/kpi/functional");
        return task;
    }
}
