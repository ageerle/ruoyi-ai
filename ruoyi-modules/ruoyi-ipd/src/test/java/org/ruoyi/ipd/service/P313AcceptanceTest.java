package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.NotificationService.Types;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P3-1.3 KPI 月度截止日、提醒升级与导入分段验收。 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P313AcceptanceTest {

    @Mock private KpiRecordMapper kpiRecordMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper productGroupMapper;
    @Mock private IpdPermission permission;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private NotificationService notificationService;

    private KpiSharedCollectionService service;

    @BeforeEach
    void setUp() {
        service = new KpiSharedCollectionService(
            kpiRecordMapper, projectMapper, projectMemberMapper, personMapper,
            permission, auditLogService, productGroupMapper, systemConfigService,
            notificationService);
    }

    @Test
    @DisplayName("AC-KPI-21b：monthlyDeadlineDay=5/10 时截止日即时变化")
    void monthlyDeadlineDay_isConfigurable() {
        YearMonth nextMonth = YearMonth.of(2026, 10);
        LocalDate day5 = service.resolveMonthlyDeadline(nextMonth, 5, ZoneOffset.UTC)
            .toLocalDate();
        LocalDate day10 = service.resolveMonthlyDeadline(nextMonth, 10, ZoneOffset.UTC)
            .toLocalDate();

        assertThat(day5).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(day10).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(day10).isAfter(day5);
    }

    @Test
    @DisplayName("截止日第 1 天提醒产品组长，不自动关闭")
    void scan_dayOne_remindsGroupLeaderAndKeepsOpen() {
        Project project = project();
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(
            member(101L, "MARKET_PM", 11L), member(201L, "RD_PM", 12L)));
        when(personMapper.selectById(101L)).thenReturn(person(101L, "MARKET_PM", 11L));
        when(personMapper.selectById(201L)).thenReturn(person(201L, "RD_PM", 12L));
        when(productGroupMapper.selectById(11L)).thenReturn(group(11L, 901L));
        when(productGroupMapper.selectById(12L)).thenReturn(group(12L, 902L));
        when(kpiRecordMapper.selectCount(any())).thenReturn(0L);
        when(notificationService.publish(any(), any(), any(), any(), any(Long.class), any(), any(), any()))
            .thenReturn(NotificationEvent.builder().id(1L).build());
        LocalDate scanDate = YearMonth.of(2026, 9).atEndOfMonth()
            .plusDays(9); // 2026-10-08 截止后的第 1 天

        KpiSharedCollectionService.DeadlineScanResult result = service.scanMonthlyDeadlines(
            scanDate, YearMonth.of(2026, 9), 5);

        assertThat(result.day1Reminders()).isEqualTo(2);
        assertThat(result.day3Escalations()).isZero();
        verify(notificationService, org.mockito.Mockito.times(2)).publish(
            any(), eq("KPI_SHARED_DEADLINE_DAY1"), eq("ACTION"), any(),
            any(Long.class), any(), any(), any());
        verify(notificationService, never()).publish(
            any(), eq("KPI_SHARED_DEADLINE_DAY3"), any(), any(), any(Long.class), any(), any(), any());
        verify(auditLogService, org.mockito.Mockito.times(2)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("截止日第 3 天升级所有超管")
    void scan_dayThree_escalatesAllSuperAdmins() {
        when(projectMapper.selectList(any())).thenReturn(List.of(project()));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(101L, "MARKET_PM", 11L)));
        when(personMapper.selectById(101L)).thenReturn(person(101L, "MARKET_PM", 11L));
        when(productGroupMapper.selectById(11L)).thenReturn(group(11L, 901L));
        when(personMapper.selectList(any())).thenReturn(List.of(
            person(901L, "SUPER_ADMIN", null), person(902L, "SUPER_ADMIN", null)));
        when(kpiRecordMapper.selectCount(any())).thenReturn(0L);
        when(notificationService.publish(any(), any(), any(), any(), any(Long.class), any(), any(), any()))
            .thenReturn(NotificationEvent.builder().id(1L).build());
        LocalDate scanDate = YearMonth.of(2026, 9).atEndOfMonth().plusDays(11);

        KpiSharedCollectionService.DeadlineScanResult result = service.scanMonthlyDeadlines(
            scanDate, YearMonth.of(2026, 9), 5);

        assertThat(result.day3Escalations()).isEqualTo(2);
        verify(notificationService, org.mockito.Mockito.times(2)).publish(
            any(), eq("KPI_SHARED_DEADLINE_DAY3"), eq("ACTION"), any(),
            any(Long.class), any(), any(), any());
    }

    @Test
    @DisplayName("已有最终归集的项目不产生提醒，也不自动关闭")
    void scan_collectedProjectIsSkipped() {
        when(projectMapper.selectList(any())).thenReturn(List.of(project()));
        when(kpiRecordMapper.selectCount(any())).thenReturn(1L);

        KpiSharedCollectionService.DeadlineScanResult result = service.scanMonthlyDeadlines(
            LocalDate.of(2026, 10, 9), YearMonth.of(2026, 9), 5);

        assertThat(result.skippedProjects()).isEqualTo(1);
        assertThat(result.day1Reminders()).isZero();
        assertThat(result.day3Escalations()).isZero();
        verify(notificationService, never()).publish(any(), any(), any(), any(), any(Long.class), any(), any(), any());
        verify(projectMemberMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("导入分段：功能补入后全量、共担不分段")
    void functionalEntry_andSharedEntrySegmentRules() {
        KpiRecord functional = KpiRecord.builder().segment("FULL").build();
        KpiRecord shared = KpiRecord.builder().segment("FULL_SHARED").build();
        assertThat(functional.getSegment()).isEqualTo("FULL");
        assertThat(shared.getSegment()).isEqualTo("FULL_SHARED");
    }

    private static Project project() {
        return Project.builder().id(1L).mainGroupId(10L).status("ACTIVE").build();
    }

    private static ProjectMember member(Long id, String role, Long groupId) {
        Person person = person(id, role, groupId);
        return ProjectMember.builder().projectId(1L).personId(id).role(role).build();
    }

    private static Person person(Long id, String type, Long groupId) {
        return Person.builder().id(id).personType(type).groupId(groupId).build();
    }

    private static ProductGroup group(Long id, Long leaderId) {
        return ProductGroup.builder().id(id).leaderPersonId(leaderId).build();
    }
}
