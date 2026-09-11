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
import org.ruoyi.ipd.mapper.ProjectMapper;
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
    @Mock
    private ProjectMapper projectMapper;

    private KeyGateArbitrationAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new KeyGateArbitrationAggregator(gateMapper, gateArbitrationMapper, projectMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private Gate gate(long id, long projectId, String gateCode) {
        // 仲裁仅存在于被驳回的 Gate（requireArbitratable 语义，2026-09-08 修正）
        return Gate.builder()
            .id(id).projectId(projectId).gateCode(gateCode).status("REJECTED").build();
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
    @DisplayName("gate 非 REJECTED 不投递：签署中（PENDING）无仲裁语义 → 防御过滤拦下")
    void collect_skipsNonRejectedGates() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        // mock 故意返回 PENDING gate + 未裁行：SQL 过滤在 mock 不生效，验证 Java 防御双保险
        Gate pendingGate = Gate.builder()
            .id(301L).projectId(10L).gateCode("G3").status("PENDING").build();
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(pendingGate));
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, null, 2)));

        assertThat(aggregator.collect(leader, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：无未裁仲裁行 → 短路不查 gate（查询顺序以仲裁行为锚）")
    void collect_skipsWhenNoUndecidedRows() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(aggregator.collect(leader, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(gateMapper, never()).selectList(any());
        verify(projectMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("回归（真活 gate 999101）：组长非项目成员（visibleProjects 空）→ 仲裁卡仍投递，项目补查 ACTIVE")
    void collect_nonMemberLeaderStillGetsCard() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, null, 2)));
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3")));
        // 组长不在 project_members → visibleProjects 空；补查返回 ACTIVE 项目
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build()));

        List<Map<String, Object>> tasks = aggregator.collect(leader, new LinkedHashMap<>(), new Date());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("projectName")).isEqualTo("项目A");
        assertThat(tasks.get(0).get("projectCode")).isEqualTo("P-001");
    }

    @Test
    @DisplayName("防御：旧轮残留未裁行（round=1，gate 已在第 2 轮）不投，避免幽灵卡")
    void collect_skipsStaleRoundRows() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, null, 1)));
        Gate round2Gate = Gate.builder()
            .id(301L).projectId(10L).gateCode("G3").status("REJECTED").currentRound(2).build();
        when(gateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(round2Gate));

        assertThat(aggregator.collect(leader, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("口径对齐：项目非 ACTIVE（补查空）不投卡")
    void collect_skipsWhenProjectNotActive() {
        IpdActor leader = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        when(gateArbitrationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(arbitration(601L, 301L, "GROUP_LEADER", 3L, null, 2)));
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(gate(301L, 10L, "G3")));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(aggregator.collect(leader, new LinkedHashMap<>(), new Date())).isEmpty();
    }
}
