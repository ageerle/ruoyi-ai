package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
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
 * KeyGateAggregator 单测（P1.2，设计 §3 表 #3；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KeyGateAggregatorTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateReviewMapper gateReviewMapper;

    private KeyGateAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new KeyGateAggregator(gateMapper, gateReviewMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private Gate gate(long id, long projectId, String gateCode, String status, Date signDueAt) {
        return Gate.builder()
            .id(id).projectId(projectId).gateCode(gateCode).status(status)
            .currentRound(2).signDueAt(signDueAt).build();
    }

    private GateReview review(long id, long gateId, String reviewerType, long reviewerId,
                              String decision, Integer round, Date signDueAt) {
        return GateReview.builder()
            .id(id).gateId(gateId).reviewerType(reviewerType).reviewerId(reviewerId)
            .decision(decision).round(round).signDueAt(signDueAt).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 key_gate（spec 页03:165 权威枚举）")
    void taskType_isKeyGate() {
        assertThat(aggregator.taskType()).isEqualTo("key_gate");
    }

    @Test
    @DisplayName("正常路径：actor 未签的 PENDING gate 评审行 → 1 卡（13 必填字段 + GR- 前缀 + 阻断标记）")
    void collect_buildsCardForUnsignedReview() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date future = new Date(System.currentTimeMillis() + 86400000L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3", "PENDING", future)));
        when(gateReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(review(401L, 301L, "MARKET_PM", 1L, null, 2, future)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("key_gate");
        assertThat(card.get("id")).isEqualTo("GR-401");
        assertThat(card.get("projectId")).isEqualTo(10L);
        assertThat(card.get("projectName")).isEqualTo("项目A");
        assertThat(card.get("projectCode")).isEqualTo("P-001");
        assertThat(card.get("actionCode")).isEqualTo("GATE-SIGN-401");
        assertThat(card.get("title")).isEqualTo("Gate 签署：G3（第 2 轮）");
        assertThat(card.get("status")).isEqualTo("PENDING");
        assertThat(card.get("priority")).isEqualTo("normal");
        assertThat(card.get("ownerRole")).isEqualTo("MARKET_PM");
        assertThat(card.get("dueDate")).isEqualTo(future);
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/projects/10/gates");
    }

    @Test
    @DisplayName("已签（decision 非 null）不投递：mapper 只回已决行 → 0 卡")
    void collect_skipsSignedReviews() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3", "PENDING", null)));
        when(gateReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(review(401L, 301L, "MARKET_PM", 1L, "APPROVE", 1, null)));

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("gate 非 PENDING（已 APPROVED）不投递：gateMapper 空 → 短路不查 review")
    void collect_skipsSettledGates() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
        verify(gateReviewMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("期限逾期：review.signDueAt 昨日 → priority=high；回退口径 gate.signDueAt")
    void collect_marksOverdueAndFallsBackToGateDue() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Date past = new Date(System.currentTimeMillis() - 86400000L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        // review 自身无期限 → 回退 gate.signDueAt（昨日）
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G2", "PENDING", past)));
        when(gateReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(review(401L, 301L, "MARKET_PM", 1L, null, 1, null)));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks.get(0).get("priority")).isEqualTo("high");
        assertThat(tasks.get(0).get("dueDate")).isEqualTo(past);
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发任何查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);

        assertThat(aggregator.collect(actor, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(gateMapper, never()).selectList(any());
        verify(gateReviewMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("悬挂行防护：review.gateId 不在 gateById → 跳过不抛 NPE")
    void collect_skipsOrphanReviews() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G1", "PENDING", null)));
        // gateId=999 不存在于 gateById
        when(gateReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(review(401L, 999L, "MARKET_PM", 1L, null, 1, null)));

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
    }
}
