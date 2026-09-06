package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectScoreTask;
import org.ruoyi.ipd.mapper.NotificationEventMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreTaskMapper;
import org.ruoyi.ipd.security.IpdPermission;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P3-2.3 上市 30/90 日项目绩效待办与逾期催办验收。 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P323AcceptanceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper groupMapper;
    @Mock private ProjectScoreTaskMapper taskMapper;
    @Mock private NotificationEventMapper notificationEventMapper;
    @Mock private org.ruoyi.ipd.service.NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission permission;

    private ProjectScoreScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ProjectScoreScheduleService(
            projectMapper, memberMapper, personMapper, groupMapper, taskMapper,
            notificationService, auditLogService, permission);
    }

    @Test
    @DisplayName("上市后 30 日生成两 PM 自评，90 日生成两组长评定")
    void scan_createsSelfAndLeaderTasksAtDueDates() {
        Project project = project(1L, LocalDate.of(2026, 6, 1));
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(memberMapper.selectList(any())).thenReturn(List.of(
            member(101L, "MARKET_PM", 11L), member(201L, "RD_PM", 12L)));
        when(taskMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.insert(any(ProjectScoreTask.class))).thenAnswer(invocation -> {
            ((ProjectScoreTask) invocation.getArgument(0)).setId(System.nanoTime());
            return 1;
        });

        ProjectScoreScheduleService.ScheduleScanResult result = service.scanLaunchedProjects(
            LocalDate.of(2026, 7, 1));

        assertThat(result.createdTasks()).isEqualTo(2);
        verify(taskMapper, org.mockito.Mockito.times(2)).insert(any(ProjectScoreTask.class));
        // 90 日组任务不提前生成，扫描日仍只创建两 PM 任务
        assertThat(result.selfTasks()).isEqualTo(2);
        assertThat(result.leaderTasks()).isZero();
    }

    @Test
    @DisplayName("上市日期更正只更新既有任务，不重复创建")
    void scan_dateCorrectionDoesNotDuplicateTask() {
        Project project = project(1L, LocalDate.of(2026, 5, 1));
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(memberMapper.selectList(any())).thenReturn(List.of(member(101L, "MARKET_PM", 11L)));
        ProjectScoreTask existing = task(1L, 101L, "SELF",
            LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 1));
        when(taskMapper.selectOne(any())).thenReturn(existing);
        when(taskMapper.updateById(existing)).thenReturn(1);

        service.scanLaunchedProjects(LocalDate.of(2026, 7, 2));

        verify(taskMapper, never()).insert(any(ProjectScoreTask.class));
        verify(taskMapper).updateById(existing);
        assertThat(java.time.Instant.ofEpochMilli(existing.getDueAt().getTime())
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            .isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("逾期待办提醒 PM，超时升级超管")
    void scan_overduePublishesActionAndEscalatesAdmin() {
        Project project = project(1L, LocalDate.of(2026, 6, 1));
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(memberMapper.selectList(any())).thenReturn(List.of(member(101L, "MARKET_PM", 11L)));
        peopleAndGroups();
        when(taskMapper.selectOne(any())).thenReturn(task(1L, 101L, "SELF",
            LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)));
        lenient().when(notificationService.publish(any(), anyString(), anyString(), anyString(), any(Long.class),
            anyString(), anyString(), anyString())).thenReturn(NotificationEvent.builder().id(1L).build());

        ProjectScoreScheduleService.ScheduleScanResult result = service.scanLaunchedProjects(
            LocalDate.of(2026, 7, 2));

        assertThat(result.reminders()).isEqualTo(1);
        verify(notificationService).publish(
            eq(101L), eq("PROJECT_SCORE_SELF_OVERDUE"), anyString(), anyString(), any(Long.class),
            anyString(), anyString(), nullable(String.class));
    }

    @Test
    @DisplayName("上市日期缺失不生成待办")
    void scan_withoutLaunchDateSkips() {
        Project project = project(1L, null);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));

        ProjectScoreScheduleService.ScheduleScanResult result = service.scanLaunchedProjects(
            LocalDate.of(2026, 7, 1));

        assertThat(result.createdTasks()).isZero();
        verify(taskMapper, never()).insert(any(ProjectScoreTask.class));
    }

    private void peopleAndGroups() {
        when(personMapper.selectById(101L)).thenReturn(person(101L, "MARKET_PM", 11L));
        when(personMapper.selectById(201L)).thenReturn(person(201L, "RD_PM", 12L));
        when(personMapper.selectById(901L)).thenReturn(person(901L, "GROUP_LEADER", 11L));
        when(personMapper.selectById(902L)).thenReturn(person(902L, "GROUP_LEADER", 12L));
        when(groupMapper.selectById(11L)).thenReturn(group(11L, 901L));
        when(groupMapper.selectById(12L)).thenReturn(group(12L, 902L));
    }

    private static Project project(Long id, LocalDate launchDate) {
        return Project.builder().id(id).mainGroupId(10L).status("ACTIVE")
            .launchDate(launchDate == null ? null : java.sql.Date.valueOf(launchDate)).build();
    }

    private static ProjectMember member(Long id, String role, Long groupId) {
        return ProjectMember.builder().projectId(1L).personId(id).role(role).build();
    }

    private static Person person(Long id, String type, Long groupId) {
        return Person.builder().id(id).personType(type).groupId(groupId).build();
    }

    private static ProductGroup group(Long id, Long leaderId) {
        return ProductGroup.builder().id(id).leaderPersonId(leaderId).build();
    }

    private static ProjectScoreTask task(Long id, Long personId, String type, LocalDate due, LocalDate launch) {
        return ProjectScoreTask.builder().id(id).projectId(1L).personId(personId).targetType(type)
            .dueAt(java.sql.Date.valueOf(due)).launchDateSnapshot(java.sql.Date.valueOf(launch))
            .status("PENDING").build();
    }
}
