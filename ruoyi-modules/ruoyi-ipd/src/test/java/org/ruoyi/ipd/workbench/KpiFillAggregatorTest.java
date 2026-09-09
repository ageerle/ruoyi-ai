package org.ruoyi.ipd.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.text.SimpleDateFormat;
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
 * KpiFillAggregator 单测（P1.4，设计 §3 表 #8；@Tag("dev")）。
 * 状态值域以代码为准（EDITING，非设计文档写的 DRAFT）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KpiFillAggregatorTest {

    @Mock
    private KpiRecordMapper kpiRecordMapper;

    private KpiFillAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new KpiFillAggregator(kpiRecordMapper);
    }

    private Map<Long, Project> scope(Project... projects) {
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project p : projects) {
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private KpiRecord record(long id, long projectId, long personId, String kpiType,
                             String period, String status) {
        return KpiRecord.builder()
            .id(id).projectId(projectId).personId(personId).kpiType(kpiType)
            .period(period).status(status).build();
    }

    @Test
    @DisplayName("taskType 契约：固定 kpi_fill（spec 页03:165 权威枚举）")
    void taskType_isKpiFill() {
        assertThat(aggregator.taskType()).isEqualTo("kpi_fill");
    }

    @Test
    @DisplayName("正常路径：EDITING + 当月 → KF- 卡（功能 KPI deepLink functional）")
    void collect_personGetsFunctionalCard() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        String period = new SimpleDateFormat("yyyy-MM").format(new Date());
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(record(1301L, 10L, 1L, "FUNCTIONAL", period, "EDITING")));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).hasSize(1);
        Map<String, Object> card = tasks.get(0);
        assertThat(card.get("taskType")).isEqualTo("kpi_fill");
        assertThat(card.get("id")).isEqualTo("KF-1301");
        assertThat(card.get("title")).isEqualTo("KPI 月度填报：" + period + "（功能）");
        assertThat(card.get("status")).isEqualTo("EDITING");
        assertThat(card.get("ownerRole")).isNull();
        assertThat(card.get("dueDate")).isNull();
        assertThat(card.get("priority")).isEqualTo("normal");
        assertThat(card.get("isBlocking")).isEqualTo("1");
        assertThat(card.get("deepLink")).isEqualTo("/ipd/kpi/functional");
    }

    @Test
    @DisplayName("共担 KPI（SHARED）deepLink 指向归集页 /ipd/kpi/shared")
    void collect_sharedKpiLinksToSharedPage() {
        IpdActor actor = new IpdActor(3L, "leader", "GROUP_LEADER", 10L);
        String period = new SimpleDateFormat("yyyy-MM").format(new Date());
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(record(1302L, 10L, 3L, "SHARED", period, "EDITING")));

        List<Map<String, Object>> tasks = aggregator.collect(actor, byId, new Date());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("deepLink")).isEqualTo("/ipd/kpi/shared");
        assertThat(tasks.get(0).get("title")).asString().contains("共担归集");
    }

    @Test
    @DisplayName("非当月（period=上月）不投：Java 侧 period 再验兜底（mock 盲区防御）")
    void collect_skipsNonCurrentPeriod() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(record(1301L, 10L, 1L, "FUNCTIONAL", "2026-08", "EDITING")));

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("非本人（personId 不匹配）或非 EDITING（APPROVED）不投")
    void collect_skipsOtherPersonsAndFinalized() {
        IpdActor actor = new IpdActor(9L, "carol", "RD_PM", 10L);
        String period = new SimpleDateFormat("yyyy-MM").format(new Date());
        Map<Long, Project> byId = scope(Project.builder()
            .id(10L).code("P-001").name("项目A").status("ACTIVE").build());
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                record(1301L, 10L, 1L, "FUNCTIONAL", period, "EDITING"),
                record(1302L, 10L, 9L, "FUNCTIONAL", period, "APPROVED")));

        assertThat(aggregator.collect(actor, byId, new Date())).isEmpty();
    }

    @Test
    @DisplayName("边界：可见项目为空 → 不触发查询")
    void collect_shortCircuitsOnEmptyScope() {
        IpdActor actor = new IpdActor(1L, "alice", "MARKET_PM", 10L);

        assertThat(aggregator.collect(actor, new LinkedHashMap<>(), new Date())).isEmpty();
        verify(kpiRecordMapper, never()).selectList(any());
    }
}
