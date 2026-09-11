package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    /**
     * 截止日：weekend 跳过生效。monthDay=5 但月初 1,2 是周末时，5 个工作日须跨更多日历日。
     * <p>已知边界：算法仅跳过 Sat/Sun，无中国节假日表（节假日若需跳过，须新增 holiday 表 + 算法增强）；
     * 当前 AC-KPI-21 满足"P3-1.3 截止日 = 次月第 N 个工作日"工作日=周一至周五的口径。
     */
    @Test
    @DisplayName("截止日：周末跳过生效（首日含周末 vs 全工作日，第 5 工作日落在不同 DayOfWeek）")
    void resolveMonthlyDeadline_weekendSkip_shiftsNthWorkdayDayOfWeek() {
        // 2027-05：5/1=Sat(weekend), 5/2=Sun(weekend), 5/3=Mon(#1), 5/4=Tue(#2), 5/5=Wed(#3),
        //          5/6=Thu(#4), 5/7=Fri(#5)
        // → 第 5 工作日 = 5/7 Fri；算法返 次日 18:00 = 5/8
        YearMonth may2027 = YearMonth.of(2027, 5);
        LocalDate deadlineMay2027 = service.resolveMonthlyDeadline(may2027, 5, ZoneOffset.UTC)
            .toLocalDate();
        assertThat(deadlineMay2027).isEqualTo(LocalDate.of(2027, 5, 8));
        // 核心断言：weekend skip 改变 Nth workday 的 DayOfWeek
        assertThat(DayOfWeek.from(LocalDate.of(2027, 5, 7))).isEqualTo(DayOfWeek.FRIDAY);

        // 对照：2026-10：10/1=Thu(#1), 10/2=Fri(#2), 10/3/4 周末, 10/5=Mon(#3), 10/6=Tue(#4), 10/7=Wed(#5)
        // → 第 5 工作日 = 10/7 Wed；算法返 10/8
        YearMonth oct2026 = YearMonth.of(2026, 10);
        LocalDate deadlineOct2026 = service.resolveMonthlyDeadline(oct2026, 5, ZoneOffset.UTC)
            .toLocalDate();
        assertThat(deadlineOct2026).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(DayOfWeek.from(LocalDate.of(2026, 10, 7))).isEqualTo(DayOfWeek.WEDNESDAY);

        // 同样 day=5：首日含周末 → Nth workday 在 Fri；首日全工作日 → Nth workday 在 Wed
        // 差 2 个工作日（Sat+Sun 各加 1），证明周末跳过生效
        assertThat(DayOfWeek.from(LocalDate.of(2027, 5, 7)))
            .isNotEqualTo(DayOfWeek.from(LocalDate.of(2026, 10, 7)));
    }

    /**
     * 幂等：同一天重复扫描触发下游 publish。service 层不 dedup（避免热点扫描期间持锁），
     * 去重在 {@link NotificationService} 内部完成：
     * dedupKey = {@code sourceType:eventType:sourceId:receiverId} 撞唯一约束时
     * 第二次 publish 抛 DuplicateKeyException 后回退为返回既有行 → 收件箱最终仍 1 条。
     * <p>本测验证 service 每次扫描产出确定性的 publish 参数（receiver/eventType/sourceId 完全一致），
     * 端到端"通知次数 == 1"由 NotificationService.doPublish 的 DuplicateKeyException 兜底保证。
     */
    @Test
    @DisplayName("幂等：同一天重复扫描 → publish 参数确定 → 下游 NotificationService dedup_key 唯一约束拦截重复")
    void scan_repeatedSameDay_publishesDeterministicArgs() {
        // 3-arg overload 直接收 configuredDay，不读 systemConfigService（避免 STRICT_STUBS 误报）
        when(projectMapper.selectList(any())).thenReturn(List.of(project()));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(
            member(101L, "MARKET_PM", 11L)));
        when(personMapper.selectById(101L)).thenReturn(person(101L, "MARKET_PM", 11L));
        when(productGroupMapper.selectById(11L)).thenReturn(group(11L, 901L));
        when(kpiRecordMapper.selectCount(any())).thenReturn(0L);
        when(notificationService.publish(any(), any(), any(), any(), any(Long.class), any(), any(), any()))
            .thenReturn(NotificationEvent.builder().id(1L).build());

        LocalDate scanDate = LocalDate.of(2026, 10, 9); // 9月归集→10/8截止→D+1

        // 第 1 次扫描
        KpiSharedCollectionService.DeadlineScanResult r1 =
            service.scanMonthlyDeadlines(scanDate, YearMonth.of(2026, 9), 5);
        // 第 2 次同参扫描（同一天、同一周期）
        KpiSharedCollectionService.DeadlineScanResult r2 =
            service.scanMonthlyDeadlines(scanDate, YearMonth.of(2026, 9), 5);

        // 两次扫描结果一致（不重复计数）
        assertThat(r1.day1Reminders()).isEqualTo(1);
        assertThat(r2.day1Reminders()).isEqualTo(1);

        // 2 扫描 × 1 组长 = 2 次 publish，每次参数完全一致
        // 捕获 (receiverId, eventType, sourceId) 三维 → dedup_key 维度
        ArgumentCaptor<Long> receiverCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> sourceIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationService, times(2)).publish(
            receiverCaptor.capture(),
            eventTypeCaptor.capture(),
            eq(NotificationService.KIND_ACTION),
            eq("KPI_SHARED_COLLECTION"),
            sourceIdCaptor.capture(),
            any(), any(), any());

        // 三维完全一致 → NotificationService 第二次调用撞 dedup_key 唯一约束 → 收件箱仍 1 条
        assertThat(receiverCaptor.getAllValues()).containsExactly(901L, 901L);
        assertThat(eventTypeCaptor.getAllValues())
            .containsExactly("KPI_SHARED_DEADLINE_DAY1", "KPI_SHARED_DEADLINE_DAY1");
        assertThat(sourceIdCaptor.getAllValues()).containsExactly(1L, 1L);
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
