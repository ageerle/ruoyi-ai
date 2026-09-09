package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.HandoverMapper;
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
 * HandoverAggregator 单测（P1.3，设计 §3 表 #5；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverAggregatorTest {

    @Mock
    private HandoverMapper handoverMapper;

    private HandoverAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new HandoverAggregator(handoverMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private HandoverRecord record(long id, String type, long fromId, long toId, Long projectId,
                                  String status, String role) {
        return HandoverRecord.builder()
            .id(id).handoverType(type).fromPersonId(fromId).toPersonId(toId)
            .projectId(projectId).status(status).handoverRole(role).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 handover（spec 页03:165 权威枚举）")
    void taskType_isHandover() {
        assertThat(aggregator.taskType()).isEqualTo("handover");
    }

    @Test
    @DisplayName("承接人视角：toPersonId 匹配 → 「移交确认：项目（MARKET_PM）」卡")
    void collect_receiverGetsConfirmCard() {
        IpdActor toActor = new IpdActor(2L, "bob", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(record(701L, "PROJECT", 1L, 2L, 10L, "DRAFT", "MARKET_PM")));

        List<Map<String, Object>> tasks = aggregator.collect(toActor, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("handover");
        assertThat(card.get("id")).isEqualTo("HD-701");
        assertThat(card.get("title")).isEqualTo("移交确认：项目（MARKET_PM）");
        assertThat(card.get("status")).isEqualTo("DRAFT");
        assertThat(card.get("ownerRole")).isEqualTo("MARKET_PM");
        assertThat(card.get("dueDate")).isNull();
        assertThat(card.get("priority")).isEqualTo("normal");
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/handover");
    }

    @Test
    @DisplayName("移交人视角：fromPersonId 匹配 → 「移交跟进」卡")
    void collect_senderGetsFollowUpCard() {
        IpdActor fromActor = new IpdActor(1L, "alice", "RD_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(record(701L, "PROJECT", 1L, 2L, 10L, "CONFIRMED", null)));

        List<Map<String, Object>> tasks = aggregator.collect(fromActor, byId, new Date());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("title")).isEqualTo("移交跟进：项目");
        assertThat(tasks.get(0).get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("非当事人（第三方）不投递；COMPLETED 不投")
    void collect_skipsThirdPartyAndCompleted() {
        IpdActor other = new IpdActor(9L, "carol", "RD_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                record(701L, "PROJECT", 1L, 2L, 10L, "DRAFT", "RD_PM"),
                record(702L, "PROJECT", 9L, 2L, 10L, "COMPLETED", "RD_PM")));

        assertThat(aggregator.collect(other, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(1L, "alice", "RD_PM", 10L);

        assertThat(aggregator.collect(actor, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(handoverMapper, never()).selectList(any());
    }
}
