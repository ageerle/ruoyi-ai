package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.CoefficientChangeRequest;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.CoefficientChangeRequestMapper;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;
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
 * StrategicChangeAggregator 单测（P1.4，设计 §3 表 #7 双表合并；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StrategicChangeAggregatorTest {

    @Mock
    private LaunchDateChangeRequestMapper launchDateChangeMapper;
    @Mock
    private CoefficientChangeRequestMapper coefficientChangeMapper;

    private StrategicChangeAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new StrategicChangeAggregator(launchDateChangeMapper, coefficientChangeMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    @Test
    @DisplayName("taskType 契约：固定 strategic_change（spec 页03:165 权威枚举）")
    void taskType_isStrategicChange() {
        assertThat(aggregator.taskType()).isEqualTo("strategic_change");
    }

    @Test
    @DisplayName("上市日期变更：confirmer 匹配 → LD- 卡（PENDING_SECOND + deepLink changes 页）")
    void collect_confirmerGetsLaunchCard() {
        IpdActor confirmer = new IpdActor(2L, "bob", "RD_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(launchDateChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(LaunchDateChangeRequest.builder()
                .id(1101L).projectId(10L).status("PENDING_SECOND")
                .proposedLaunchDate(new Date()).confirmerId(2L).confirmerRole("RD_PM").build()));

        List<Map<String, Object>> tasks = aggregator.collect(confirmer, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("strategic_change");
        assertThat(card.get("id")).isEqualTo("LD-1101");
        assertThat(card.get("title")).asString().startsWith("上市日期变更待确认 → ");
        assertThat(card.get("status")).isEqualTo("PENDING_SECOND");
        assertThat(card.get("ownerRole")).isEqualTo("RD_PM");
        assertThat(card.get("dueDate")).isNull();
        assertThat(card.get("priority")).isEqualTo("normal");
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/projects/10/changes");
    }

    @Test
    @DisplayName("系数变更：leaderId 匹配 → CC- 卡（PENDING_LEADER + ownerRole=null）")
    void collect_leaderGetsCoefficientCard() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(coefficientChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(CoefficientChangeRequest.builder()
                .id(1201L).projectId(10L).status("PENDING_LEADER")
                .proposedCoefficient(new BigDecimal("1.20")).leaderId(3L).build()));

        List<Map<String, Object>> tasks = aggregator.collect(leader, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("id")).isEqualTo("CC-1201");
        assertThat(card.get("actionCode")).isEqualTo("COEFFICIENT-1201");
        assertThat(card.get("title")).asString().contains("1.20");
        assertThat(card.get("status")).isEqualTo("PENDING_LEADER");
        assertThat(card.get("ownerRole")).isNull();
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/projects/10/changes");
    }

    @Test
    @DisplayName("双表非当事人（confirmerId/leaderId 不匹配）均不投")
    void collect_skipsNonParties() {
        IpdActor other = new IpdActor(9L, "carol", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(launchDateChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(LaunchDateChangeRequest.builder()
                .id(1101L).projectId(10L).status("PENDING_SECOND").confirmerId(2L).build()));
        when(coefficientChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(CoefficientChangeRequest.builder()
                .id(1201L).projectId(10L).status("PENDING_LEADER").leaderId(3L).build()));

        assertThat(aggregator.collect(other, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("已定值/拒绝不投：mapper 只回 PENDING 行，Java 侧状态再验兜底（mock 盲区防御）")
    void collect_skipsDecidedRequests() {
        IpdActor actor = new IpdActor(2L, "bob", "RD_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(launchDateChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(LaunchDateChangeRequest.builder()
                .id(1101L).projectId(10L).status("CONFIRMED").confirmerId(2L).build()));
        when(coefficientChangeMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(CoefficientChangeRequest.builder()
                .id(1201L).projectId(10L).status("REJECTED").leaderId(2L).build()));

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：可见项目为空 → 双表都不触发查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(2L, "bob", "RD_PM", 10L);

        assertThat(aggregator.collect(actor, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(launchDateChangeMapper, never()).selectList(any());
        verify(coefficientChangeMapper, never()).selectList(any());
    }
}
