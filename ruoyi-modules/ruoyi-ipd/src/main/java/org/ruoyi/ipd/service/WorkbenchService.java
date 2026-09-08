package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.workbench.WorkbenchAggregator;
import org.ruoyi.ipd.workbench.WorkbenchPolicy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 我的工作台聚合（ZK-D2+ 后端真实现，对齐原型 GET /api/workflow/tasks 语义）。
 *
 * 真实数据源（无任何 mock）：
 * - 待我处理 / 临期超期 / 已完成：WorkbenchAggregator 聚合器列表统一投递
 *   （P1 方向 B：taskType 按域拆分到 org.ruoyi.ipd.workbench 各实现，summary() 只做调度；
 *   现有 stage_sign / deletion_review 两类，后续 7 类已就绪域按 P1.2-P1.4 接力）；
 * - 未读通知：notification_events（receiver = 当前人，unread）；
 * - 我的当前推进：当前项目 currentStage 的第一个未完成动作（stageActionMapper 仅剩此职责 + completed 已移入 StageSignAggregator）。
 *
 * 项目范围（原型 access.projectScope 同构）：SUPER_ADMIN=全部；其余=project_members 本人数组。
 * 责任匹配：动作 ownerRole ∈ {MARKET_PM, RD_PM} 时仅该角色可见；其余角色/空值全员可见。
 */
@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private static final String ST_ACTIVE_PROJECT = "ACTIVE";

    /** spec 页03:165 权威 17 类全集（与前端 WORKBENCH_TASK_TYPE_TEXT 同序同值，勿漂移）。 */
    private static final List<String> ALL_TASK_TYPES = List.of(
        "bonus_lock", "capacity_approval", "change_implementation", "change_verify",
        "closeout", "contribution_confirm", "deletion_review", "handover",
        "key_gate", "key_gate_arbitration", "kpi_fill", "rd_replacement",
        "receipt_review", "retirement_review", "stage_sign", "strategic_change", "waiver_review");

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final StageActionMapper stageActionMapper;
    private final NotificationService notificationService;
    /** 按域聚合器列表（Spring 注入全部 WorkbenchAggregator 实现，@Order 决定 stage 先 deletion 后）。 */
    private final List<WorkbenchAggregator> aggregators;

    /**
     * 工作台总览。
     *
     * @param actor     当前登录人（SEC-API-01：仅从会话推导）
     * @param projectId 指定当前项目（顶栏切换）；空 = 第一个进行中项目
     * @return stats + 任务平铺列表（前端按项目分组）+ 当前推进 + 删除待办数
     */
    public Map<String, Object> summary(IpdActor actor, Long projectId) {
        List<Project> scope = visibleProjects(actor);
        Map<Long, Project> byId = new LinkedHashMap<>();
        scope.forEach(p -> byId.put(p.getId(), p));

        Date now = new Date();
        // 统一调度：每类 taskType 由各自 aggregator 投递卡（P1 方向 B）
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (WorkbenchAggregator aggregator : aggregators) {
            tasks.addAll(aggregator.collect(actor, byId, now));
        }
        int completed = aggregators.stream()
            .mapToInt(a -> a.completedCount(actor, byId))
            .sum();

        // overdue 统一按任务卡 dueDate 口径（所有 taskType 同规则）
        long overdue = tasks.stream()
            .filter(t -> t.get("dueDate") instanceof Date d && d.before(now))
            .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        // pending 按主任务总数（B4 拍板③）：全部 taskType 计入
        stats.put("pending", tasks.size());
        // 计数一律 int 装箱：全局 Long→String 序列化会把 Long 计数变字符串，破坏前端 number 契约
        stats.put("overdue", (int) overdue);
        stats.put("unread", (int) notificationService.unreadCount(actor.id()));
        stats.put("completed", (int) completed);
        // 按类型计数（设计 §5）：17 类 key 预置 0（无数据类也返回），供前端按类型过滤/展示
        Map<String, Integer> pendingType = new LinkedHashMap<>();
        for (String taskType : ALL_TASK_TYPES) {
            pendingType.put(taskType, 0);
        }
        for (Map<String, Object> task : tasks) {
            pendingType.merge(String.valueOf(task.get("taskType")), 1, Integer::sum);
        }
        stats.put("pendingType", pendingType);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("tasks", tasks);
        // deletionPending 保留独立字段（前端 WorkbenchSummary 兼容）：按 taskType 从调度结果统计
        result.put("deletionPending", (int) tasks.stream()
            .filter(t -> "deletion_review".equals(t.get("taskType")))
            .count());
        result.put("currentAdvance", currentAdvance(actor, projectId, byId));
        return result;
    }

    /** 项目可见范围：超管全部，其余按成员关系。 */
    private List<Project> visibleProjects(IpdActor actor) {
        if ("SUPER_ADMIN".equals(actor.role())) {
            return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, ST_ACTIVE_PROJECT)
                .orderByAsc(Project::getId));
        }
        List<ProjectMember> memberships = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getPersonId, actor.id()));
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> ids = memberships.stream().map(ProjectMember::getProjectId).distinct().toList();
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
            .in(Project::getId, ids)
            .eq(Project::getStatus, ST_ACTIVE_PROJECT)
            .orderByAsc(Project::getId));
    }

    /** 我的当前推进：指定项目（或第一个可见项目）当前阶段的第一个未完成动作。 */
    private Map<String, Object> currentAdvance(IpdActor actor, Long projectId, Map<Long, Project> byId) {
        Project target = null;
        if (projectId != null && byId.containsKey(projectId)) {
            target = byId.get(projectId);
        } else {
            target = byId.values().stream().findFirst().orElse(null);
        }
        if (target == null) {
            return null;
        }
        List<StageAction> actions = stageActionMapper.selectList(
            new LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, target.getId())
                .orderByAsc(StageAction::getId));
        StageAction next = actions.stream()
            .filter(a -> WorkbenchPolicy.OPEN_STATUSES.contains(a.getStatus())
                && WorkbenchPolicy.isMine(a.getOwnerRole(), actor.role()))
            .min(Comparator.comparing(StageAction::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .orElse(null);
        Map<String, Object> advance = new LinkedHashMap<>();
        advance.put("projectId", target.getId());
        advance.put("projectCode", target.getCode());
        advance.put("projectName", target.getName());
        advance.put("currentStage", target.getCurrentStage());
        advance.put("actionId", next != null ? next.getId() : null);
        advance.put("actionName", next != null ? next.getActionName() : null);
        advance.put("actionStatus", next != null ? next.getStatus() : null);
        advance.put("deepLink", next != null
            ? "/ipd/projects/" + target.getId() + "/actions/" + next.getId()
            : "/ipd/projects/" + target.getId() + "/flow");
        return advance;
    }

    /** 通知收件箱透传（工作台右侧与顶栏红点共用）。 */
    public List<NotificationEvent> inbox(IpdActor actor, boolean unreadOnly) {
        return notificationService.inbox(actor.id(), unreadOnly);
    }
}
