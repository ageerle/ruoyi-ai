package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectScoreTask;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreTaskMapper;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 上市 30 日双 PM 自评、90 日两组长评定的待办调度器（P3-2.3）。
 * 上市日期变化通过更新既有任务重排，任务表不新增第二行。
 */
@Service
public class ProjectScoreScheduleService {

    private static final String TYPE_SELF = "SELF_SCORING";
    private static final String TYPE_LEADER = "LEADER_REVIEW";
    private static final String STATUS_PENDING = "PENDING";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final ProductGroupMapper groupMapper;
    private final ProjectScoreTaskMapper taskMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final IpdPermission permission;

    public ProjectScoreScheduleService(ProjectMapper projectMapper,
                                       ProjectMemberMapper memberMapper,
                                       PersonMapper personMapper,
                                       ProductGroupMapper groupMapper,
                                       ProjectScoreTaskMapper taskMapper,
                                       NotificationService notificationService,
                                       AuditLogService auditLogService,
                                       IpdPermission permission) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.personMapper = personMapper;
        this.groupMapper = groupMapper;
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.permission = permission;
    }

    /** 扫描所有已上市项目；不自动关闭 PENDING。 */
    @Transactional(rollbackFor = Exception.class)
    public ScheduleScanResult scanLaunchedProjects(LocalDate scanDate) {
        if (scanDate == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "扫描日期不能为空");
        }
        List<Project> projects = projectMapper.selectList(
            Wrappers.<Project>lambdaQuery()
                .isNotNull(Project::getLaunchDate)
                .ne(Project::getDelFlag, "1"));
        int created = 0;
        int self = 0;
        int leader = 0;
        int reminders = 0;
        int escalations = 0;
        int updated = 0;
        for (Project project : projects) {
            LocalDate launch = localDate(project.getLaunchDate());
            if (launch == null || scanDate.isBefore(launch.plusDays(30))) {
                continue;
            }
            List<ProjectMember> members = activeMembers(project.getId());
            for (ProjectMember member : members) {
                boolean selfCreated = ensureTask(project, member, TYPE_SELF,
                    launch.plusDays(30), scanDate, "/project-scores/self");
                if (selfCreated) {
                    self++;
                }
                if (selfCreated) {
                    created++;
                }
            }
            if (!scanDate.isBefore(launch.plusDays(90))) {
                for (ProjectMember member : members) {
                    boolean leaderCreated = ensureTask(project, member, TYPE_LEADER,
                        launch.plusDays(90), scanDate, "/project-scores/review");
                    if (leaderCreated) {
                        leader++;
                        created++;
                    }
                }
            }
            for (ProjectMember member : members) {
                ProjectScoreTask task = findTask(project.getId(), member.getPersonId(), TYPE_SELF);
                if (task != null && isOverdue(task, scanDate)) {
                    publishTaskReminder(task, "PROJECT_SCORE_SELF_OVERDUE",
                        "项目绩效自评待办已逾期，请双 PM 完成自评", scanDate);
                    reminders++;
                }
            }
            if (!scanDate.isBefore(launch.plusDays(90))) {
                for (ProjectMember member : members) {
                    ProjectScoreTask task = findTask(project.getId(), member.getPersonId(), TYPE_LEADER);
                    if (task != null && isOverdue(task, scanDate)) {
                        Person leaderPerson = groupLeader(member.getPersonId());
                        if (leaderPerson != null) {
                            publishTaskReminderFor(task, leaderPerson.getId(),
                                "PROJECT_SCORE_LEADER_REVIEW_OVERDUE",
                                "项目绩效组长评定待办已逾期", scanDate);
                            reminders++;
                        }
                        if (ChronoUnit.DAYS.between(localDate(task.getDueAt()), scanDate) >= 3) {
                            for (Person admin : superAdmins()) {
                                publishEscalation(task, admin.getId(), scanDate);
                                escalations++;
                            }
                        }
                    }
                }
            }
        }
        return new ScheduleScanResult(created, self, leader, reminders, escalations, updated);
    }

    private boolean ensureTask(Project project, ProjectMember member, String targetType, LocalDate due,
                               LocalDate scanDate, String actionUrl) {
        ProjectScoreTask existing = findTask(project.getId(), member.getPersonId(), targetType);
        Date dueDate = date(due);
        if (existing == null) {
            ProjectScoreTask row = ProjectScoreTask.builder()
                .projectId(project.getId()).personId(member.getPersonId()).targetType(targetType)
                .dueAt(dueDate).launchDateSnapshot(project.getLaunchDate())
                .status(STATUS_PENDING).actionUrl(actionUrl).build();
            try {
                taskMapper.insert(row);
                auditAction("PROJECT_SCORE_TASK_CREATE", project.getId(), row, "created");
                return true;
            } catch (DuplicateKeyException duplicate) {
                existing = findTask(project.getId(), member.getPersonId(), targetType);
                if (existing == null) {
                    throw duplicate;
                }
            }
        }
        if (!localDate(existing.getDueAt()).equals(due)
            || !existing.getLaunchDateSnapshot().equals(project.getLaunchDate())) {
            existing.setDueAt(dueDate).setLaunchDateSnapshot(project.getLaunchDate());
            existing.setStatus(STATUS_PENDING);
            taskMapper.updateById(existing);
            auditAction("PROJECT_SCORE_TASK_RESCHEDULE", project.getId(), existing, "rescheduled");
        }
        return false;
    }

    private ProjectScoreTask findTask(Long projectId, Long personId, String targetType) {
        return taskMapper.selectOne(
            Wrappers.<ProjectScoreTask>lambdaQuery()
                .eq(ProjectScoreTask::getProjectId, projectId)
                .eq(ProjectScoreTask::getPersonId, personId)
                .eq(ProjectScoreTask::getTargetType, targetType)
                .eq(ProjectScoreTask::getStatus, STATUS_PENDING)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private List<ProjectMember> activeMembers(Long projectId) {
        return memberMapper.selectList(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .isNull(ProjectMember::getExitDate)
                .in(ProjectMember::getRole, List.of("MARKET_PM", "RD_PM"))
                .orderByAsc(ProjectMember::getId));
    }

    private Person groupLeader(Long personId) {
        Person member = personId == null ? null : personMapper.selectById(personId);
        if (member == null || member.getGroupId() == null) {
            return null;
        }
        ProductGroup group = groupMapper.selectById(member.getGroupId());
        if (group == null || group.getLeaderPersonId() == null) {
            return null;
        }
        return personMapper.selectById(group.getLeaderPersonId());
    }

    private List<Person> superAdmins() {
        return personMapper.selectList(
            Wrappers.<Person>lambdaQuery()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .ne(Person::getDelFlag, "1"));
    }

    private boolean isOverdue(ProjectScoreTask task, LocalDate scanDate) {
        Date due = task == null ? null : task.getDueAt();
        return due != null && scanDate.isAfter(localDate(due));
    }

    private void publishTaskReminder(ProjectScoreTask task, String eventType,
                                      String content, LocalDate scanDate) {
        if (task.getPersonId() == null) {
            return;
        }
        publishTaskReminderFor(task, task.getPersonId(), eventType, content, scanDate);
    }

    private void publishTaskReminderFor(ProjectScoreTask task, Long receiverId, String eventType,
                                         String content, LocalDate scanDate) {
        if (receiverId == null || notificationService == null) {
            return;
        }
        notificationService.publish(receiverId, eventType, NotificationService.KIND_ACTION,
            "PROJECT_SCORE_TASK", task.getId(), "项目绩效待办逾期", content, task.getActionUrl());
        auditAction("PROJECT_SCORE_TASK_REMIND", task.getProjectId(), task, eventType);
    }

    private void publishEscalation(ProjectScoreTask task, Long adminId, LocalDate scanDate) {
        if (adminId == null) {
            return;
        }
        notificationService.publish(adminId, "PROJECT_SCORE_TASK_ESCALATION", NotificationService.KIND_ACTION,
            "PROJECT_SCORE_TASK", task.getId(), "项目绩效待办升级", "上市后 90 日未完成评定，请超管介入",
            task.getActionUrl());
        auditAction("PROJECT_SCORE_TASK_ESCALATE", task.getProjectId(), task, "day3");
    }

    private void auditAction(String action, Long projectId, ProjectScoreTask task, String reason) {
        auditLogService.append(AuditLog.builder()
            .operatorId(0L).operatorName("PROJECT_SCORE_SCANNER")
            .action(action).entityType("project_score_tasks").entityId(task.getId())
            .reason(reason).afterData(AuditEventData.json(
                "projectId", projectId, "personId", task.getPersonId(), "targetType", task.getTargetType(),
                "dueAt", task.getDueAt(), "launchDateSnapshot", task.getLaunchDateSnapshot()))
            .createTime(new Date())
            .build());
    }

    private static LocalDate localDate(Date date) {
        return date == null ? null
            : java.time.Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date date(LocalDate date) {
        return java.sql.Date.valueOf(date);
    }

    public record ScheduleScanResult(
        int createdTasks,
        int selfTasks,
        int leaderTasks,
        int reminders,
        int escalations,
        int updatedTasks
    ) { }
}
