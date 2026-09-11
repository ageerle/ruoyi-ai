package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ContributionMapper;
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
 * ContributionConfirmAggregator 单测（P1.3，设计 §3 表 #6；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ContributionConfirmAggregatorTest {

    @Mock
    private ContributionMapper contributionMapper;

    private ContributionConfirmAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ContributionConfirmAggregator(contributionMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private Contribution contribution(long id, long projectId, Long leaderId, String status) {
        return Contribution.builder()
            .id(id).projectId(projectId).leaderId(leaderId).status(status).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 contribution_confirm（spec 页03:165 权威枚举）")
    void taskType_isContributionConfirm() {
        assertThat(aggregator.taskType()).isEqualTo("contribution_confirm");
    }

    @Test
    @DisplayName("正常路径：actor 是评定组长（leaderId）+ SUBMITTED → 1 卡（CT- 前缀 + 阻断标记）")
    void collect_leaderGetsPendingConfirmationCard() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(contributionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(contribution(801L, 10L, 3L, "SUBMITTED")));

        List<Map<String, Object>> tasks = aggregator.collect(leader, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("contribution_confirm");
        assertThat(card.get("id")).isEqualTo("CT-801");
        assertThat(card.get("projectId")).isEqualTo(10L);
        assertThat(card.get("projectName")).isEqualTo("项目A");
        assertThat(card.get("actionCode")).isEqualTo("CONTRIBUTION-801");
        assertThat(card.get("title")).asString().contains("贡献度确认");
        assertThat(card.get("status")).isEqualTo("SUBMITTED");
        assertThat(card.get("ownerRole")).isNull();
        assertThat(card.get("dueDate")).isNull();
        assertThat(card.get("priority")).isEqualTo("normal");
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/incentive/contribution");
    }

    @Test
    @DisplayName("非该评定组长（leaderId 不匹配）不投")
    void collect_skipsNonLeader() {
        IpdActor otherLeader = new IpdActor(4L, "other", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(contributionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(contribution(801L, 10L, 3L, "SUBMITTED")));

        assertThat(aggregator.collect(otherLeader, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("已确认（CONFIRMED）不投：mapper 只回 SUBMITTED 行")
    void collect_skipsConfirmed() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(contributionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(contribution(801L, 10L, 3L, "CONFIRMED")));

        assertThat(aggregator.collect(leader, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);

        assertThat(aggregator.collect(leader, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(contributionMapper, never()).selectList(any());
    }
}
