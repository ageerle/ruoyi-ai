package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectScoreTask;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreTaskMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 「我的在途评分待办」验收（GET /api/v1/project-score-tasks/my；2026-09-08
 * 前端契约对照轮补交）：自评任务按 personId 归我、组长任务按组归属、PENDING 过滤。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectScoreMyTasksTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper groupMapper;
    @Mock private ProjectScoreTaskMapper taskMapper;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission permission;

    private ProjectScoreScheduleService service;

    private static final Long ME = 3001L;
    private static final Long MEMBER_A = 4001L;
    private static final Long PROJECT_1 = 100L;
    private static final Long PROJECT_2 = 200L;

    @BeforeEach
    void setUp() {
        service = new ProjectScoreScheduleService(projectMapper, memberMapper, personMapper,
            groupMapper, taskMapper, notificationService, auditLogService, permission);
        // 通用：我不领导任何组（部分用例覆盖）
        lenient().when(groupMapper.selectList(any())).thenReturn(List.of());
        // 通用：项目/人员字典
        lenient().when(projectMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
            Project.builder().id(PROJECT_1).code("PRJ-001").name("阿尔法").build(),
            Project.builder().id(PROJECT_2).code("PRJ-002").name("贝塔").build()));
        lenient().when(personMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
            Person.builder().id(ME).name("我本人").build(),
            Person.builder().id(MEMBER_A).name("成员A").build()));
    }

    private IpdActor me() {
        return new IpdActor(ME, "我本人", "MARKET_PM", 10L);
    }

    private ProjectScoreTask task(Long projectId, Long personId, String targetType, Date due) {
        return ProjectScoreTask.builder()
            .projectId(projectId).personId(personId).targetType(targetType)
            .dueAt(due).status("PENDING").actionUrl("/project-scores/self")
            .launchDateSnapshot(new Date()).build();
    }

    @Test
    @DisplayName("我的自评 PENDING 任务：返回且带项目编码/我的姓名；dueAt 升序")
    void myTasks_returnsSelfPending() {
        Date earlier = new Date(System.currentTimeMillis() - 86_400_000L);
        Date later = new Date(System.currentTimeMillis() + 86_400_000L * 10);
        when(taskMapper.selectList(any())).thenReturn(List.of(
            task(PROJECT_2, ME, "SELF_SCORING", later),
            task(PROJECT_1, ME, "SELF_SCORING", earlier)));

        List<ProjectScoreScheduleService.MyScoreTaskView> views = service.myTasks(me());

        assertThat(views).hasSize(2);
        // dueAt 升序：earlier 在前
        assertThat(views.get(0).projectId()).isEqualTo(PROJECT_1);
        assertThat(views.get(0).projectCode()).isEqualTo("PRJ-001");
        assertThat(views.get(0).personName()).isEqualTo("我本人");
        assertThat(views.get(0).targetType()).isEqualTo("SELF_SCORING");
        assertThat(views.get(1).projectId()).isEqualTo(PROJECT_2);
    }

    @Test
    @DisplayName("组长视角：我领导的产品组成员的 LEADER_REVIEW 待办计入我的任务")
    void myTasks_includesLeaderReviewForLedGroupMembers() {
        when(taskMapper.selectList(any())).thenReturn(
            List.of(task(PROJECT_1, ME, "SELF_SCORING",
                new Date(System.currentTimeMillis() + 86_400_000L * 5))))
            .thenReturn(List.of(task(PROJECT_2, MEMBER_A, "LEADER_REVIEW",
                new Date(System.currentTimeMillis() + 86_400_000L))));
        when(groupMapper.selectList(any())).thenReturn(List.of(
            ProductGroup.builder().id(10L).leaderPersonId(ME).build()));
        when(personMapper.selectList(any())).thenReturn(List.of(
            Person.builder().id(MEMBER_A).name("成员A").groupId(10L).delFlag("0").build()));

        List<ProjectScoreScheduleService.MyScoreTaskView> views = service.myTasks(me());

        assertThat(views).hasSize(2);
        assertThat(views).anySatisfy(v -> {
            assertThat(v.targetType()).isEqualTo("LEADER_REVIEW");
            assertThat(v.personId()).isEqualTo(MEMBER_A);
            assertThat(v.personName()).isEqualTo("成员A");
            assertThat(v.projectCode()).isEqualTo("PRJ-002");
        });
    }

    @Test
    @DisplayName("无在途任务：返回空列表（不返回 null）")
    void myTasks_empty() {
        when(taskMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.myTasks(me())).isEmpty();
    }
}
