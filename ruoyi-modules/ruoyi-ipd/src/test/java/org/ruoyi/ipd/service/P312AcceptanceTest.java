package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.SharedKpiController;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.dto.SharedKpiCollectReq;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.vo.SharedKpiCollectView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P3-1.2 共担 KPI 归集、样本与 40% 权重验收。 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P312AcceptanceTest {

    @Mock private KpiRecordMapper kpiRecordMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private IpdPermission permission;
    @Mock private AuditLogService auditLogService;

    private KpiSharedCollectionService service;
    private IpdActor leader;

    @BeforeEach
    void setUp() {
        service = new KpiSharedCollectionService(
            kpiRecordMapper, projectMapper, projectMemberMapper, personMapper,
            permission, auditLogService);
        leader = new IpdActor(900L, "组长", "GROUP_LEADER", 10L);
        when(permission.requireLeaderOrAdmin()).thenReturn(leader);
    }

    @Test
    @DisplayName("AC-KPI-05/07/08/10：四项按来源与样本计算，双 PM 同分")
    void collectSharedKpi_calculatesFourSourcesAndBothPmSame() {
        project(10L, "500.00", 80, 50, 4);
        activeMembers(101L, 201L);

        SharedKpiCollectView result = service.collectSharedKpi(leader, request());

        assertThat(result.sharedScore()).isEqualByComparingTo("95.50");
        assertThat(result.items())
            .extracting(SharedKpiCollectView.Metric::code)
            .containsExactly("K01", "K02", "K03", "K04");
        assertThat(result.items().get(0).score()).isEqualByComparingTo("96.00");
        assertThat(result.items().get(1).score()).isEqualByComparingTo("95.00");
        assertThat(result.items().get(2).included()).isFalse();
        assertThat(result.items().get(2).message()).contains("样本不足 30");
        assertThat(result.items().get(3).score()).isEqualByComparingTo("95.00");

        ArgumentCaptor<KpiRecord> captor = ArgumentCaptor.forClass(KpiRecord.class);
        verify(kpiRecordMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        Map<Long, KpiRecord> rows = captor.getAllValues().stream()
            .collect(java.util.stream.Collectors.toMap(KpiRecord::getPersonId, row -> row));
        assertThat(rows.get(101L).getComprehensiveScore()).isEqualByComparingTo("95.50");
        assertThat(rows.get(201L).getComprehensiveScore()).isEqualByComparingTo("95.50");
        assertThat(rows.get(101L).getSegment()).isEqualTo("FULL_SHARED");
        assertThat(rows.get(201L).getSegment()).isEqualTo("FULL_SHARED");
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("AC-KPI-09：NPS 达到 30 份后恢复计入")
    void collectSharedKpi_npsWithThirtySamplesIncluded() {
        project(10L, "500.00", 80, 50, 4);
        activeMembers(101L, 201L);
        SharedKpiCollectReq request = new SharedKpiCollectReq(
            1L, "2026-08", "400", "500", "60", 80, 35, 0, 35, 3, 4);

        SharedKpiCollectView result = service.collectSharedKpi(leader, request);

        assertThat(result.items().get(2).included()).isTrue();
        assertThat(result.items().get(2).score()).isEqualByComparingTo("97.00");
        assertThat(result.sharedScore()).isEqualByComparingTo("95.88");
    }

    @Test
    @DisplayName("AC-KPI-20：普通 PM 不能录入或复核 K01-K04")
    void collectSharedKpi_pmRejected() {
        IpdActor marketPm = new IpdActor(101L, "市场PM", "MARKET_PM", 10L);
        when(permission.requireLeaderOrAdmin()).thenReturn(marketPm);

        assertThatThrownBy(() -> service.collectSharedKpi(marketPm, request()))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("产品组长");
        verify(kpiRecordMapper, never()).insert(any(KpiRecord.class));
    }

    @Test
    @DisplayName("AC-KPI-20：产品组长不能跨产品组归集")
    void collectSharedKpi_crossGroupRejected() {
        IpdActor outsider = new IpdActor(999L, "外组组长", "GROUP_LEADER", 99L);
        when(permission.requireLeaderOrAdmin()).thenReturn(outsider);
        project(10L, "500.00", 80, 50, 4);

        assertThatThrownBy(() -> service.collectSharedKpi(outsider, request()))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("产品组");
        verify(kpiRecordMapper, never()).insert(any(KpiRecord.class));
    }

    @Test
    @DisplayName("版本留痕：重复归集为每人追加 revision，不覆盖旧版本")
    void collectSharedKpi_appendOnlyRevision() {
        project(10L, "500.00", 80, 50, 4);
        activeMembers(101L, 201L);
        KpiRecord old = KpiRecord.builder().revision(1).build();
        when(kpiRecordMapper.selectOne(any())).thenReturn(old);
        doAnswer(invocation -> {
            KpiRecord row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(System.nanoTime());
            }
            return 1;
        }).when(kpiRecordMapper).insert(any(KpiRecord.class));

        service.collectSharedKpi(leader, request());
        service.collectSharedKpi(leader, request());

        ArgumentCaptor<KpiRecord> captor = ArgumentCaptor.forClass(KpiRecord.class);
        verify(kpiRecordMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertThat(captor.getAllValues()).allMatch(row -> row.getRevision() == 2);
    }

    @Test
    @DisplayName("HTTP：POST /api/v1/kpi/shared 返回双 PM 共担得分")
    void sharedKpiHttpEndpoint() throws Exception {
        project(10L, "500.00", 80, 50, 4);
        activeMembers(101L, 201L);
        when(permission.requireInternal()).thenReturn(leader);
        when(permission.requireLeaderOrAdmin()).thenReturn(leader);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new SharedKpiController(permission, service)).build();

        mvc.perform(post("/api/v1/kpi/shared")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId":1,"period":"2026-08","actualSales":"400",
                     "targetSales":"500","actualChannels":"60","targetChannels":80,
                     "promoters":10,"detractors":5,"npsSampleSize":29,
                     "landedScenarios":3,"plannedScenarios":4}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.sharedScore").value(95.50))
            .andExpect(jsonPath("$.data.items.length()").value(4))
            .andExpect(jsonPath("$.data.items[2].included").value(false));
    }

    @Test
    @DisplayName("目标值必须大于 0，销量达成率低于 50% 计 0")
    void collectSharedKpi_invalidTargetAndBelowFifty() {
        Project project = project(10L, "0.00", 80, 50, 4);
        activeMembers(101L, 201L);
        SharedKpiCollectReq request = new SharedKpiCollectReq(
            1L, "2026-08", "40", "500", "60", 80, 17, 12, 30, 3, 4);

        assertThatThrownBy(() -> service.collectSharedKpi(leader, request))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("目标销售额");
        assertThat(service.calculateSharedAchievement(new BigDecimal("49.99"), new BigDecimal("100")).setScale(2))
            .isEqualByComparingTo("0.00");
        verify(kpiRecordMapper, never()).insert(any(KpiRecord.class));
        assertThat(project.getId()).isNotNull();
    }

    private SharedKpiCollectReq request() {
        return new SharedKpiCollectReq(
            1L, "2026-08", "400", "500", "60", 80, 10, 5, 29, 3, 4);
    }

    private Project project(long groupId, String targetSales, int channels, int nps, int scenes) {
        Project project = Project.builder()
            .id(1L).mainGroupId(groupId)
            .targetSalesAmount(new BigDecimal(targetSales))
            .targetChannelCount(channels).targetNps(nps).targetSceneCount(scenes)
            .status("ACTIVE").build();
        when(projectMapper.selectById(1L)).thenReturn(project);
        return project;
    }

    private void activeMembers(Long marketPmId, Long rdPmId) {
        ProjectMember market = member(marketPmId, "MARKET_PM");
        ProjectMember rd = member(rdPmId, "RD_PM");
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(market, rd));
        when(personMapper.selectById(marketPmId)).thenReturn(Person.builder()
            .id(marketPmId).personType("MARKET_PM").groupId(11L).build());
        when(personMapper.selectById(rdPmId)).thenReturn(Person.builder()
            .id(rdPmId).personType("RD_PM").groupId(12L).build());
    }

    private static ProjectMember member(Long id, String role) {
        return ProjectMember.builder().projectId(1L).personId(id).role(role).build();
    }
}
