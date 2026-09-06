package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
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
 * - 待我处理 / 临期超期 / 已完成：ipd_stage_actions 按项目范围 + 责任角色过滤；
 * - 未读通知：notification_events（receiver = 当前人，unread）；
 * - 删除审批待办：ipd_deletion_requests（组长 LEADER_REVIEW / 超管 ADMIN_REVIEW）；
 * - 我的当前推进：当前项目 currentStage 的第一个未完成动作。
 *
 * 项目范围（原型 access.projectScope 同构）：SUPER_ADMIN=全部；其余=project_members 本人数组。
 * 责任匹配：动作 ownerRole ∈ {MARKET_PM, RD_PM} 时仅该角色可见；其余角色/空值全员可见。
 */
@Service
@RequiredArgsConstructor
public class WorkbenchService {

    /** 深管/轻管共用的未完成态（P1-4.3：DELAYED 仅深管存在，统一计入待办）。 */
    private static final List<String> OPEN_STATUSES = List.of("NOT_STARTED", "IN_PROGRESS", "DELAYED");
    private static final String ST_DONE = "DONE";
    private static final String ST_ACTIVE_PROJECT = "ACTIVE";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final StageActionMapper stageActionMapper;
    private final DeletionRequestMapper deletionRequestMapper;
    private final NotificationService notificationService;

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

        List<StageAction> openActions = new ArrayList<>();
        long completed = 0;
        if (!byId.isEmpty()) {
            List<StageAction> all = stageActionMapper.selectList(
                new LambdaQueryWrapper<StageAction>().in(StageAction::getProjectId, byId.keySet()));
            for (StageAction action : all) {
                if (ST_DONE.equals(action.getStatus()) || "NA".equals(action.getStatus())) {
                    if (ST_DONE.equals(action.getStatus())) {
                        completed += 1;
                    }
                } else if (OPEN_STATUSES.contains(action.getStatus()) && isMine(action.getOwnerRole(), actor.role())) {
                    openActions.add(action);
                }
            }
        }
        Date now = new Date();
        long overdue = openActions.stream()
            .filter(a -> a.getDueDate() != null && a.getDueDate().before(now))
            .count();

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (StageAction action : openActions) {
            Project project = byId.get(action.getProjectId());
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", action.getId());
            task.put("projectId", action.getProjectId());
            task.put("projectName", project != null ? project.getName() : null);
            task.put("projectCode", project != null ? project.getCode() : null);
            task.put("actionCode", action.getActionCode());
            task.put("title", action.getActionName());
            task.put("taskType", "stage_action");
            task.put("status", action.getStatus());
            task.put("priority", action.getDueDate() != null && action.getDueDate().before(now) ? "high" : "normal");
            task.put("ownerRole", action.getOwnerRole());
            task.put("dueDate", action.getDueDate());
            task.put("isBlocking", action.getIsBlocking());
            task.put("deepLink", "/ipd/projects/" + action.getProjectId() + "/actions/" + action.getId());
            tasks.add(task);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", openActions.size());
        // 计数一律 int 装箱：全局 Long→String 序列化会把 Long 计数变字符串，破坏前端 number 契约
        stats.put("overdue", (int) overdue);
        stats.put("unread", (int) notificationService.unreadCount(actor.id()));
        stats.put("completed", (int) completed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("tasks", tasks);
        result.put("deletionPending", (int) deletionPendingCount(actor));
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

    /** 删除审批待办：组长=待初审数；超管=待终审数；其余角色 0。 */
    private long deletionPendingCount(IpdActor actor) {
        if ("SUPER_ADMIN".equals(actor.role())) {
            return deletionRequestMapper.selectCount(new LambdaQueryWrapper<DeletionRequest>()
                .eq(DeletionRequest::getStatus, DeletionRequestService.ST_ADMIN_REVIEW));
        }
        if ("GROUP_LEADER".equals(actor.role())) {
            return deletionRequestMapper.selectCount(new LambdaQueryWrapper<DeletionRequest>()
                .eq(DeletionRequest::getStatus, DeletionRequestService.ST_LEADER_REVIEW));
        }
        return 0;
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
            .filter(a -> OPEN_STATUSES.contains(a.getStatus()) && isMine(a.getOwnerRole(), actor.role()))
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

    /** 责任匹配：MARKET_PM/RD_PM 定向；组长/超管/联合（BOTH）/空值全员。owner_role 值域见 DDL：MARKET_PM|RD_PM|BOTH。 */
    private boolean isMine(String ownerRole, String personRole) {
        if (ownerRole == null || ownerRole.isBlank() || "BOTH".equals(ownerRole) || "JOINT".equals(ownerRole)) {
            return true;
        }
        if ("SUPER_ADMIN".equals(personRole) || "GROUP_LEADER".equals(personRole)) {
            return true;
        }
        return Objects.equals(ownerRole, personRole);
    }

    /** 通知收件箱透传（工作台右侧与顶栏红点共用）。 */
    public List<NotificationEvent> inbox(IpdActor actor, boolean unreadOnly) {
        return notificationService.inbox(actor.id(), unreadOnly);
    }
}
