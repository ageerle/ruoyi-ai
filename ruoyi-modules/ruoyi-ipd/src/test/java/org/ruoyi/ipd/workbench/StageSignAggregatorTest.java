package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StageSignAggregator 单测（P1.1 从 WorkbenchServiceTest 下沉的责任链/字段组装行为，@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StageSignAggregatorTest {

    @Mock
    private StageActionMapper stageActionMapper;

    private StageSignAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new StageSignAggregator(stageActionMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private StageAction action(long id, long projectId, String code, String name,
                               String ownerRole, String status, Date dueDate) {
        return StageAction.builder()
            .id(id).projectId(projectId).actionCode(code).actionName(name)
            .ownerRole(ownerRole).status(status).dueDate(dueDate).isBlocking("N")
            .build();
    }

    @Test
    @DisplayName("taskType 契约：固定 stage_sign（spec 页03:165 权威枚举）")
    void taskType_isStageSign() {
        assertThat(aggregator.taskType()).isEqualTo("stage_sign");
    }

    @Test
    @DisplayName("正常路径 + 卡字段契约：MARKET_PM 命中自己的 OPEN 动作，13 必填字段齐全")
    void collect_buildsFullContractCard() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").currentStage("CDP").status("ACTIVE").build());
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(action(101L, 10L, "CDP-01", "立项申请书", "MARKET_PM", "IN_PROGRESS", null)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("stage_sign"); // 必须 == taskType()
        assertThat(card.get("id")).isEqualTo(101L);
        assertThat(card.get("projectId")).isEqualTo(10L);
        assertThat(card.get("projectName")).isEqualTo("项目A");
        assertThat(card.get("projectCode")).isEqualTo("P-001");
        assertThat(card.get("actionCode")).isEqualTo("CDP-01");
        assertThat(card.get("title")).isEqualTo("立项申请书");
        assertThat(card.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(card.get("ownerRole")).isEqualTo("MARKET_PM");
        assertThat(card.get("isBlocking")).isEqualTo("N");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/projects/10/actions/101");
        assertThat(card).containsKeys("id", "projectId", "projectName", "projectCode", "actionCode",
            "title", "taskType", "status", "priority", "ownerRole", "dueDate", "isBlocking", "deepLink");
    }

    @Test
    @DisplayName("责任链：MARKET_PM 看不到 RD_PM 任务、看到 BOTH 任务")
    void collect_filtersByOwnerRole() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "立项", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "研发", "RD_PM", "IN_PROGRESS", null),
                action(103L, 10L, "CDP-03", "共担", "BOTH", "IN_PROGRESS", null)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).extracting(t -> t.get("actionCode"))
            .containsExactlyInAnyOrder("CDP-01", "CDP-03");
    }

    @Test
    @DisplayName("责任链：SUPER_ADMIN 命中全部 ownerRole")
    void collect_adminSeesAll() {
        IpdActor admin = new IpdActor(99L, "root", "SUPER_ADMIN", null);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "立项", "MARKET_PM", "IN_PROGRESS", null),
                action(102L, 10L, "CDP-02", "研发", "RD_PM", "IN_PROGRESS", null)));

        List<Map<String, Object>> tasks = aggregator.collect(admin, byId, new Date());

        assertThat(tasks).hasSize(2);
    }

    @Test
    @DisplayName("状态机：DONE/NA 不投递；逾期 OPEN 动作 priority=high")
    void collect_filtersTerminalStatusesAndMarksOverdue() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date past = new Date(System.currentTimeMillis() - 86400000L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "已完成", "MARKET_PM", "DONE", null),
                action(102L, 10L, "CDP-02", "不适用", "MARKET_PM", "NA", null),
                action(103L, 10L, "CDP-03", "已逾期", "MARKET_PM", "IN_PROGRESS", past),
                action(104L, 10L, "CDP-04", "未到期", "MARKET_PM", "IN_PROGRESS", null)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).extracting(t -> t.get("actionCode"))
            .containsExactly("CDP-03", "CDP-04");
        assertThat(tasks.get(0).get("priority")).isEqualTo("high");
        assertThat(tasks.get(1).get("priority")).isEqualTo("normal");
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发查询、返回空列表")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);

        List<Map<String, Object>> tasks = aggregator.collect(actor, new LinkedHashMap<>(), new Date());

        assertThat(tasks).isEmpty();
        verify(stageActionMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("completedCount：DONE 计数；NA/DONE 之外与空范围不计")
    void completedCount_countsDoneOnly() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                action(101L, 10L, "CDP-01", "已完成", "MARKET_PM", "DONE", null),
                action(102L, 10L, "CDP-02", "不适用", "MARKET_PM", "NA", null),
                action(103L, 10L, "CDP-03", "进行中", "MARKET_PM", "IN_PROGRESS", null)));

        assertThat(aggregator.completedCount(actor, byId)).isEqualTo(1);
        assertThat(aggregator.completedCount(actor, new LinkedHashMap<>())).isZero();
    }
}
