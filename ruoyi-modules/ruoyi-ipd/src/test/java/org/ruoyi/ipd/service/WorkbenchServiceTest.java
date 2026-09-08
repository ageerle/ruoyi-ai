package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.workbench.DeletionReviewAggregator;
import org.ruoyi.ipd.workbench.StageSignAggregator;
import org.ruoyi.ipd.workbench.WorkbenchAggregator;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Workbench 聚合单测（BR-WB-01..07；@Tag("dev") 必须）。
 * P1.1 起按聚合器架构分层：本类只测 summary() 的调度/统计/口径组装，
 * 责任匹配与卡字段组装下沉到 StageSignAggregatorTest / DeletionReviewAggregatorTest。
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
    private NotificationService notificationService;
    @Mock
    private StageSignAggregator stageAgg;
    @Mock
    private DeletionReviewAggregator deletionAgg;

    private WorkbenchService service;

    @BeforeEach
    void setUp() {
        List<WorkbenchAggregator> aggregators = List.of(stageAgg, deletionAgg);
        service = new WorkbenchService(
            projectMapper, projectMemberMapper, stageActionMapper,
            notificationService, aggregators);
        // 调度器对每个聚合器都调用 collect + completedCount：默认空投递，个别用例覆盖
        lenient().when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class))).thenReturn(List.of());
        lenient().when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class))).thenReturn(List.of());
        lenient().when(stageAgg.completedCount(any(IpdActor.class), any())).thenReturn(0);
        lenient().when(deletionAgg.completedCount(any(IpdActor.class), any())).thenReturn(0);
        lenient().when(stageAgg.taskType()).thenReturn("stage_sign");
        lenient().when(deletionAgg.taskType()).thenReturn("deletion_review");
    }

    private Project project(long id, String code, String name, String currentStage) {
        return Project.builder()
            .id(id).code(code).name(name).currentStage(currentStage).status("ACTIVE")
            .build();
    }

    private ProjectMember member(long projectId, long personId) {
        return ProjectMember.builder().projectId(projectId).personId(personId).build();
    }

    private Map<String, Object> stageCard(long projectId, String actionCode, Date dueDate) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", 100L + projectId);
        card.put("projectId", projectId);
        card.put("projectName", "项目" + projectId);
        card.put("projectCode", "P-" + projectId);
        card.put("actionCode", actionCode);
        card.put("title", actionCode + " 动作");
        card.put("taskType", "stage_sign");
        card.put("status", "IN_PROGRESS");
        card.put("priority", dueDate != null ? "high" : "normal");
        card.put("ownerRole", "MARKET_PM");
        card.put("dueDate", dueDate);
        card.put("isBlocking", "N");
        card.put("deepLink", "/ipd/projects/" + projectId + "/actions/" + (100L + projectId));
        return card;
    }

    private Map<String, Object> deletionCard(String status, Date dueDate) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", "DEL-501");
        card.put("projectId", null);
        card.put("projectName", "删除审批");
        card.put("projectCode", "project");
        card.put("actionCode", "DEL-REVIEW-501");
        card.put("title", "删除初审：project #10");
        card.put("taskType", "deletion_review");
        card.put("status", status);
        card.put("priority", dueDate != null ? "high" : "normal");
        card.put("ownerRole", null);
        card.put("dueDate", dueDate);
        card.put("isBlocking", "1");
        card.put("deepLink", "/ipd/deletion/review");
        return card;
    }

    @Test
    @DisplayName("正常路径：聚合器投递透传 → stats.pending/completed 组装正确")
    void summary_returnsStatsAndTasksFromAggregators() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(stageCard(10L, "CDP-01", null)));
        when(stageAgg.completedCount(any(IpdActor.class), any())).thenReturn(1);
        when(notificationService.unreadCount(1L)).thenReturn(5L);

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("pending")).isEqualTo(1);
        assertThat(stats.get("completed")).isEqualTo(1);
        assertThat(stats.get("unread")).isEqualTo(5);
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("taskType")).isEqualTo("stage_sign");
    }

    @Test
    @DisplayName("边界：actor 无可见项目 → 聚合器收空 byId → stats 全 0；tasks=[]；currentAdvance=null")
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
    @DisplayName("调度顺序：stage_sign 先、deletion_review 后（@Order 约定），卡合并保序")
    void summary_mergesAggregatorCardsInOrder() {
        IpdActor actor = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(stageCard(10L, "CDP-01", null)));
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(deletionCard("ADMIN_REVIEW", null)));

        Map<String, Object> result = service.summary(actor, null);

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(t -> t.get("taskType"))
            .containsExactly("stage_sign", "deletion_review");
    }

    @Test
    @DisplayName("deletionPending：按 taskType 从调度结果统计（不依赖具体聚合器引用）")
    void summary_countsDeletionPendingByTaskType() {
        IpdActor actor = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(stageCard(10L, "CDP-01", null), stageCard(10L, "CDP-02", null)));
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(deletionCard("ADMIN_REVIEW", null)));

        Map<String, Object> result = service.summary(actor, null);

        assertThat(result.get("deletionPending")).isEqualTo(1);
        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("pending")).isEqualTo(3);   // 主任务总数口径（B4 拍板③）
    }

    @Test
    @DisplayName("逾期：投递卡 dueDate<now → stats.overdue 计入（聚合器投 priority，Service 统一算 overdue）")
    void summary_countsOverdueAcrossAggregators() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date past = new Date(System.currentTimeMillis() - 86400000L); // 昨天
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(stageCard(10L, "CDP-01", past)));
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(deletionCard("LEADER_REVIEW", past)));

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("overdue")).isEqualTo(2);
        assertThat(stats.get("pending")).isEqualTo(2);
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
                StageAction.builder().id(201L).projectId(10L).actionCode("CDP-01").actionName("项目A动作")
                    .ownerRole("MARKET_PM").status("IN_PROGRESS").isBlocking("N").build(),
                StageAction.builder().id(301L).projectId(20L).actionCode("PDCP-01").actionName("项目B动作")
                    .ownerRole("MARKET_PM").status("NOT_STARTED").isBlocking("N").build()));

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> advance = (Map<String, Object>) result.get("currentAdvance");
        assertThat(advance).isNotNull();
        assertThat(advance.get("projectId")).isEqualTo(10L);
        assertThat(advance.get("actionId")).isEqualTo(201L);
        assertThat(advance.get("currentStage")).isEqualTo("CDP");
    }

    @Test
    @DisplayName("删除审批分流：GROUP_LEADER 投 2 卡 → deletionPending=2；RD_PM 空 → 0")
    void summary_deletionReview_dispatchByRole() {
        Date now = new Date();
        Date past = new Date(now.getTime() - 86400000L);
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(
                deletionCard("LEADER_REVIEW", past),
                deletionCard("LEADER_REVIEW", null)));

        Map<String, Object> r1 = service.summary(leader, null);

        assertThat(r1.get("deletionPending")).isEqualTo(2);
        Map<String, Object> stats1 = (Map<String, Object>) r1.get("stats");
        assertThat(stats1.get("pending")).isEqualTo(2);
        assertThat(stats1.get("overdue")).isEqualTo(1);

        // RD_PM：deletionAgg 投空 → deletionPending=0
        IpdActor rd = new IpdActor(4L, "rd", "RD_PM", 10L);
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class))).thenReturn(List.of());
        Map<String, Object> r3 = service.summary(rd, null);
        assertThat(r3.get("deletionPending")).isEqualTo(0);
        assertThat(r3.get("tasks")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("分组：tasks 按 projectId 自然分组（前端按 projectId 分组）")
    void summary_groupsTasksByProject() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(member(10L, 1L), member(20L, 1L)));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                project(10L, "P-001", "项目A", "CDP"),
                project(20L, "P-002", "项目B", "PDCP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(
                stageCard(10L, "CDP-01", null),
                stageCard(10L, "CDP-02", null),
                stageCard(20L, "PDCP-01", null)));

        Map<String, Object> result = service.summary(actor, null);
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        long projectA = tasks.stream().filter(t -> t.get("projectId").equals(10L)).count();
        long projectB = tasks.stream().filter(t -> t.get("projectId").equals(20L)).count();
        assertThat(projectA).isEqualTo(2);
        assertThat(projectB).isEqualTo(1);
    }

    @Test
    @DisplayName("pendingType：17 类 key 预置 0，按投递 taskType 计数（设计 §5）")
    void summary_pendingType_countsByTaskTypeWithFullKeys() {
        IpdActor actor = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(project(10L, "P-001", "项目A", "CDP")));
        when(stageAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(stageCard(10L, "CDP-01", null), stageCard(10L, "CDP-02", null)));
        when(deletionAgg.collect(any(IpdActor.class), any(), any(Date.class)))
            .thenReturn(List.of(deletionCard("ADMIN_REVIEW", null)));

        Map<String, Object> result = service.summary(actor, null);

        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        @SuppressWarnings("unchecked")
        Map<String, Integer> pendingType = (Map<String, Integer>) stats.get("pendingType");
        assertThat(pendingType).hasSize(17);
        assertThat(pendingType.get("stage_sign")).isEqualTo(2);
        assertThat(pendingType.get("deletion_review")).isEqualTo(1);
        assertThat(pendingType.get("handover")).isZero();
        assertThat(pendingType.get("waiver_review")).isZero();
    }
}
