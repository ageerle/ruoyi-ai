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
import org.ruoyi.ipd.domain.ProjectScoreTask;
import org.ruoyi.ipd.mapper.ProjectScoreTaskMapper;
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
 * CloseoutAggregator 单测（P1.3，设计 §3 表 #9；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class CloseoutAggregatorTest {

    @Mock
    private ProjectScoreTaskMapper projectScoreTaskMapper;

    private CloseoutAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new CloseoutAggregator(projectScoreTaskMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private ProjectScoreTask task(long id, long projectId, long personId, String targetType,
                                  String status, Date dueAt) {
        return ProjectScoreTask.builder()
            .id(id).projectId(projectId).personId(personId).targetType(targetType)
            .status(status).dueAt(dueAt).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 closeout（spec 页03:165 权威枚举）")
    void taskType_isCloseout() {
        assertThat(aggregator.taskType()).isEqualTo("closeout");
    }

    @Test
    @DisplayName("正常路径：SELF_SCORING 自评 + LEADER_REVIEW 评定 → 2 卡（PS- 前缀 + dueAt 逾期 high）")
    void collect_buildsCardsForBothTargetTypes() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date past = new Date(System.currentTimeMillis() - 86400000L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(projectScoreTaskMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                task(901L, 10L, 1L, "SELF_SCORING", "PENDING", past),
                task(902L, 10L, 1L, "LEADER_REVIEW", "PENDING", null)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).hasSize(2);
        Map<String, Object> self = tasks.get(0);
        assertThat(self.get("taskType")).isEqualTo("closeout");
        assertThat(self.get("id")).isEqualTo("PS-901");
        assertThat(self.get("title")).isEqualTo("项目绩效自评");
        assertThat(self.get("actionCode")).isEqualTo("SCORE-TASK-901");
        assertThat(self.get("priority")).isEqualTo("high"); // dueAt 已过
        assertThat(self.get("isBlocking")).isEqualTo("1");
        assertThat(self.get("deepLink")).isEqualTo("/ipd/kpi/project-score");
        assertThat(tasks.get(1).get("title")).isEqualTo("项目绩效组长评定");
        assertThat(tasks.get(1).get("priority")).isEqualTo("normal");
    }

    @Test
    @DisplayName("非本人（personId 不匹配）不投")
    void collect_skipsOtherPersons() {
        IpdActor other = new IpdActor(9L, "carol", "RD_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(projectScoreTaskMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(task(901L, 10L, 1L, "SELF_SCORING", "PENDING", null)));

        assertThat(aggregator.collect(other, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);

        assertThat(aggregator.collect(actor, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(projectScoreTaskMapper, never()).selectList(any());
    }
}
