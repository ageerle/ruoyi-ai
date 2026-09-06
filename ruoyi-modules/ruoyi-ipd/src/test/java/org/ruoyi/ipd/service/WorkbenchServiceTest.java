package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Workbench 聚合单测（BR-WB-01..07；@Tag("dev") 必须）。
 * 8 用例覆盖 6 维度：正常路径 / 边界 / 权限（角色责任链）/ 删除审批分流。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class WorkbenchServiceTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private StageActionMapper stageActionMapper;
    @Mock
    private DeletionRequestMapper deletionRequestMapper;
    @Mock
    private NotificationService notificationService;

    private WorkbenchService service;

    @BeforeEach
    void setUp() {
        service = new WorkbenchService(
            projectMapper, projectMemberMapper, stageActionMapper,
            deletionRequestMapper, notificationService);
    }

    private Project project(long id, String code, String name, String currentStage) {
        return Project.builder()
            .id(id).code(code).name(name).currentStage(currentStage).status("ACTIVE")
            .build();
    }

    private StageAction action(long id, long projectId, String code, String name,
                               String ownerRole, String status, Date dueDate) {
        return StageAction.builder()
            .id(id).projectId(projectId).actionCode(code).actionName(name)
            .ownerRole(ownerRole).status(status).dueDate(dueDate).isBlocking("N")
            .build();
    }

    private ProjectMember member(long projectId, long personId) {
        return ProjectMember.builder().projectId(projectId).personId(personId).build();
    }

    @Test
    @DisplayName("正常路径：MARKET_PM actor → stats.pending/open 等于其责任动作数")
    void summary_returnsStatsAndTasksForCurrentActor() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "立项申请书", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "可行性研究", "RD_PM", "NOT_STARTED", null),
                action(103L, 10L, "CDP-03", "立项评审", "BOTH", "DONE", null)));
        when(notificationService.unreadCount(1L)).thenReturn(5L);

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("pending")).isEqualTo(1);    // 仅 MARKET_PM 命中（CDP-01）
        assertThat(stats.get("completed")).isEqualTo(1);  // CDP-03 DONE
        assertThat(stats.get("unread")).isEqualTo(5);
        assertThat(((List<?>) result.get("tasks"))).hasSize(1);
    }

    @Test
    @DisplayName("边界：actor 无可见项目 → stats 全 0；tasks=[]；currentAdvance=null")
    void summary_returnsEmptyWhenActorHasNoVisibleProjects() {
        IpdActor actor = new IpdActor(2L, "bob", "RD_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("pending")).isEqualTo(0);
        assertThat(stats.get("overdue")).isEqualTo(0);
        assertThat(stats.get("unread")).isEqualTo(0);
        assertThat(stats.get("completed")).isEqualTo(0);
        assertThat(result.get("tasks")).isEqualTo(List.of());
        assertThat(result.get("currentAdvance")).isNull();
        assertThat(result.get("deletionPending")).isEqualTo(0);
    }

    @Test
    @DisplayName("责任链：MARKET_PM actor 看不到 RD_PM 任务")
    void summary_filtersTasksByOwnerRole_whenMarketPm() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "立项", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "研发", "RD_PM", "IN_PROGRESS", null),
                action(103L, 10L, "CDP-03", "共担", "BOTH", "IN_PROGRESS", null)));
        when(notificationService.unreadCount(any())).thenReturn(0L);

        Map<String, Object> result = service.summary(actor, null);

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        // MARKET_PM 应看到 CDP-01（自己）和 CDP-03（BOTH），看不到 CDP-02（RD_PM）
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(t -> t.get("actionCode"))
            .containsExactlyInAnyOrder("CDP-01", "CDP-03");
    }

    @Test
    @DisplayName("责任链：SUPER_ADMIN 命中全部 ownerRole")
    void summary_adminSeesAllTasks() {
        IpdActor admin = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "立项", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "研发", "RD_PM", "IN_PROGRESS", null)));
        when(notificationService.unreadCount(any())).thenReturn(0L);

        Map<String, Object> result = service.summary(admin, null);

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        assertThat(tasks).hasSize(2);
    }

    @Test
    @DisplayName("逾期：dueDate<now 的 OPEN 动作 → priority=high 且 stats.overdue 计入")
    void summary_marksOverdueActionsAsHighPriority() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date now = new Date();
        Date past = new Date(now.getTime() - 86400000L); // 昨天
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "已逾期", "MARKET_PM", "IN_PROGRESS", past),
                action(102L, 10L, "CDP-02", "未到期", "MARKET_PM", "IN_PROGRESS", null)));
        when(notificationService.unreadCount(any())).thenReturn(0L);

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("overdue")).isEqualTo(1);
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        Map<String, Object> overdue = tasks.stream()
            .filter(t -> "CDP-01".equals(t.get("actionCode"))).findFirst().orElseThrow();
        assertThat(overdue.get("priority")).isEqualTo("high");
    }

    @Test
    @DisplayName("当前推进：取首个 isMine+OPEN 动作；按 projectId 升序首个可见项目")
    void summary_currentAdvance_picksFirstOpenAction() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L), member(20L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                project(10L, "P-001", "项目A", "CDP"),
                project(20L, "P-002", "项目B", "PDCP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(201L, 10L, "CDP-01", "项目A动作", "MARKET_PM", "IN_PROGRESS", null),
                action(301L, 20L, "PDCP-01", "项目B动作", "MARKET_PM", "NOT_STARTED", null)));
        when(notificationService.unreadCount(any())).thenReturn(0L);

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> advance = (Map<String, Object>) result.get("currentAdvance");
        assertThat(advance).isNotNull();
        assertThat(advance.get("projectId")).isEqualTo(10L);
        assertThat(advance.get("actionId")).isEqualTo(201L);
        assertThat(advance.get("currentStage")).isEqualTo("CDP");
    }

    @Test
    @DisplayName("删除审批分流：GROUP_LEADER → LEADER_REVIEW 数；SUPER_ADMIN → ADMIN_REVIEW 数；其他 → 0")
    void summary_deletionPending_roleMapping() {
        // GROUP_LEADER
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(deletionRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);
        Map<String, Object> r1 = service.summary(leader, null);
        assertThat(r1.get("deletionPending")).isEqualTo(7);
        // status 过滤条件是 LEADER_REVIEW——通过 captor 校验
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<DeletionRequest>> captor1 =
            org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(deletionRequestMapper).selectCount(captor1.capture());

        // SUPER_ADMIN：visibleProjects 走 projectMapper，deletionPending 走 deletionRequestMapper。
        // visibleProjects 返回空 List → 短路 stageActionMapper，因此此处不 stub stageAction。
        IpdActor admin = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(deletionRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        Map<String, Object> r2 = service.summary(admin, null);
        assertThat(r2.get("deletionPending")).isEqualTo(2);

        // 其他角色（如 RD_PM 无可见项目时） → 0
        IpdActor rd = new IpdActor(4L, "rd", "RD_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        Map<String, Object> r3 = service.summary(rd, null);
        assertThat(r3.get("deletionPending")).isEqualTo(0);
    }

    @Test
    @DisplayName("责任队列分组：tasks 按 projectId 自然分组（前端按 projectId 分组）")
    void summary_groupsTasksByProject() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L), member(20L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                project(10L, "P-001", "项目A", "CDP"),
                project(20L, "P-002", "项目B", "PDCP")));
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "A-1", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "A-2", "MARKET_PM", "IN_PROGRESS", null),
                action(201L, 20L, "PDCP-01", "B-1", "MARKET_PM", "IN_PROGRESS", null)));
        when(notificationService.unreadCount(any())).thenReturn(0L);

        Map<String, Object> result = service.summary(actor, null);
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        long projectA = tasks.stream().filter(t -> t.get("projectId").equals(10L)).count();
        long projectB = tasks.stream().filter(t -> t.get("projectId").equals(20L)).count();
        assertThat(projectA).isEqualTo(2);
        assertThat(projectB).isEqualTo(1);
    }
}
