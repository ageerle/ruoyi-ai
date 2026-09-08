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
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
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
 * KeyGateArbitrationAggregator 单测（P1.2，设计 §3 表 #4；@Tag("dev")）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KeyGateArbitrationAggregatorTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateArbitrationMapper gateArbitrationMapper;

    private KeyGateArbitrationAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new KeyGateArbitrationAggregator(gateMapper, gateArbitrationMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private Gate gate(long id, long projectId, String gateCode) {
        return Gate.builder()
            .id(id).projectId(projectId).gateCode(gateCode).status("PENDING").build();
    }

    private GateArbitration arbitration(long id, long gateId, String type, long arbitratorId,
                                        String decision, Integer round) {
        return GateArbitration.builder()
            .id(id).gateId(gateId).arbitratorType(type).arbitratorId(arbitratorId)
            .decision(decision).round(round).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 key_gate_arbitration（spec 页03:165 权威枚举）")
    void taskType_isKeyGateArbitration() {
        assertThat(aggregator.taskType()).isEqualTo("key_gate_arbitration");
    }

    @Test
    @DisplayName("正常路径：actor 是仲裁人且未裁 → 1 卡（GA- 前缀 + dueDate=null + 阻断标记）")
    void collect_buildsCardForUndecidedArbitration() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3")));
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, null, 2)));

        List<Map<String, Object>> tasks = aggregator.collect(leader, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("key_gate_arbitration");
        assertThat(card.get("id")).isEqualTo("GA-601");
        assertThat(card.get("projectId")).isEqualTo(10L);
        assertThat(card.get("projectName")).isEqualTo("项目A");
        assertThat(card.get("actionCode")).isEqualTo("GATE-ARB-601");
        assertThat(card.get("title")).isEqualTo("Gate 仲裁：G3（第 2 轮）");
        assertThat(card.get("status")).isEqualTo("PENDING");
        assertThat(card.get("priority")).isEqualTo("normal"); // 仲裁无期限恒 normal
        assertThat(card.get("ownerRole")).isEqualTo("GROUP_LEADER");
        assertThat(card.get("dueDate")).isNull();
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/projects/10/gates");
    }

    @Test
    @DisplayName("已裁（decision 非 null）不投递")
    void collect_skipsDecidedArbitrations() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3")));
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, "APPROVE", 2)));

        assertThat(aggregator.collect(leader, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("gate 非 PENDING 不投递：gateMapper 空 → 短路不查仲裁表")
    void collect_skipsSettledGates() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(aggregator.collect(leader, byId, new Date())).isEmpty();
        verify(gateArbitrationMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发任何查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);

        assertThat(aggregator.collect(leader, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(gateMapper, never()).selectList(any());
    }
}
