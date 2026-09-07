package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
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
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.KpiSharedCollectionService.DeadlineConfigView;
import org.ruoyi.ipd.service.NotificationService.Types;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIGH-4.1 验收：KPI 月度截止日配置化（5 测）。
 *
 * <p>覆盖维度：
 * <ol>
 *   <li>默认 5（无 DB 行 → Java fallback = 5）</li>
 *   <li>DB 配置 = 3 → resolveMonthlyDeadline 走配置（3 个工作日）</li>
 *   <li>DB 配置 = 7（边界） → 7 个工作日</li>
 *   <li>deadline 前 1 天 → scanDueSoon 触发 KPI_DUE_SOON FYI</li>
 *   <li>deadline 后 1 天 → scanMonthlyDeadlines 触发 KPI_SHARED_DEADLINE_DAY1</li>
 * </ol>
 *
 * <p>AC：HIGH-4.1（kpi.monthlyDeadlineDay 走配置 + 配置前后 1 天提醒）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KpiSharedCollectionDeadlineConfigAcceptanceTest {

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

    /* ============== 1. 默认 5（fallback） ============== */

    @Test
    @DisplayName("[HIGH-4.1-1] DB 无该行：scanMonthlyDeadlines 走 Java 默认值 5")
    void defaultFallback_returnsDay5() {
        when(systemConfigService.getValue("kpi.monthlyDeadlineDay", "")).thenReturn("");
        when(systemConfigService.resolveAsOf(eq("kpi.monthlyDeadlineDay"), any()))
            .thenReturn(configView(null, "NONE", 0));

        YearMonth nextMonth = YearMonth.of(2026, 10);
        // 算法契约：返回「第 N 个工作日」的次日 18:00（既有 P313AcceptanceTest 行为）；
        // day=5 → 第 5 工作日=10/7(Wed) → 算法返 10/8
        LocalDate resolved = service.resolveMonthlyDeadline(nextMonth, 5, ZoneOffset.UTC).toLocalDate();
        assertThat(resolved).isEqualTo(LocalDate.of(2026, 10, 8));

        DeadlineConfigView view = service.getDeadlineConfig();
        assertThat(view.dayOfMonth()).isEqualTo(5);
        assertThat(view.source()).isEqualTo("FACTORY_DEFAULT");
        assertThat(view.configuredValue()).isNull();
    }

    /* ============== 2. DB 配置 = 3 ============== */

    @Test
    @DisplayName("[HIGH-4.1-2] DB 配置 = 3：scanMonthlyDeadlines 走配置 3 → 第 3 工作日")
    void dbConfiguredDay3_drivesDeadline() {
        when(systemConfigService.getValue("kpi.monthlyDeadlineDay", "")).thenReturn("3");
        when(systemConfigService.resolveAsOf(eq("kpi.monthlyDeadlineDay"), any()))
            .thenReturn(configView("3", "DB_ACTIVE", 1));

        YearMonth nextMonth = YearMonth.of(2026, 10);
        // day=3 → 第 3 工作日=10/5(Mon) → 算法返 10/6
        LocalDate resolved = service.resolveMonthlyDeadline(nextMonth, 3, ZoneOffset.UTC).toLocalDate();
        assertThat(resolved).isEqualTo(LocalDate.of(2026, 10, 6));

        DeadlineConfigView view = service.getDeadlineConfig();
        assertThat(view.dayOfMonth()).isEqualTo(3);
        assertThat(view.source()).isEqualTo("DB_ACTIVE");
        assertThat(view.version()).isEqualTo(1);
        assertThat(view.configuredValue()).isEqualTo("3");
    }

    /* ============== 3. DB 配置 = 7（边界） ============== */

    @Test
    @DisplayName("[HIGH-4.1-3] DB 配置 = 7（边界）：scanMonthlyDeadlines 走配置 7 → 第 7 工作日")
    void dbConfiguredDay7_boundary() {
        when(systemConfigService.getValue("kpi.monthlyDeadlineDay", "")).thenReturn("7");
        when(systemConfigService.resolveAsOf(eq("kpi.monthlyDeadlineDay"), any()))
            .thenReturn(configView("7", "DB_ACTIVE", 2));

        YearMonth nextMonth = YearMonth.of(2026, 10);
        // day=7 → 第 7 工作日=10/9(Fri) → 算法返 10/10（Sat，按既有 P313 行为）
        LocalDate resolved = service.resolveMonthlyDeadline(nextMonth, 7, ZoneOffset.UTC).toLocalDate();
        assertThat(resolved).isEqualTo(LocalDate.of(2026, 10, 10));

        DeadlineConfigView view = service.getDeadlineConfig();
        assertThat(view.dayOfMonth()).isEqualTo(7);
        assertThat(view.source()).isEqualTo("DB_ACTIVE");
        assertThat(view.version()).isEqualTo(2);
    }

    /* ============== 4. deadline 前 1 天 → scanDueSoon ============== */

    @Test
    @DisplayName("[HIGH-4.1-4] deadline 前 1 天：scanDueSoon 触发 KPI_DUE_SOON FYI 提醒产品组长")
    void scanDueSoon_dayBeforeDeadline_reminds() {
        Project project = project();
        when(systemConfigService.getIntValue("kpi.monthlyDeadlineDay", 5)).thenReturn(5);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(
            member(101L, "MARKET_PM", 11L), member(201L, "RD_PM", 12L)));
        when(personMapper.selectById(101L)).thenReturn(person(101L, "MARKET_PM", 11L));
        when(personMapper.selectById(201L)).thenReturn(person(201L, "RD_PM", 12L));
        when(productGroupMapper.selectById(11L)).thenReturn(group(11L, 901L));
        when(productGroupMapper.selectById(12L)).thenReturn(group(12L, 902L));
        when(kpiRecordMapper.selectCount(any())).thenReturn(0L);
        when(notificationService.publishDaily(any(), any(), any(), any(), any(Long.class), any(), any(), any(), any()))
            .thenReturn(NotificationEvent.builder().id(1L).build());
        // 9 月归集 → 次月 10 月截止，10 月第 5 工作日算法返 10/8 → 前 1 天 = 10/7
        LocalDate scanDate = LocalDate.of(2026, 10, 7);

        KpiSharedCollectionService.DeadlineScanResult result = service.scanDueSoon(
            scanDate, YearMonth.of(2026, 9));

        assertThat(result.day1Reminders()).isEqualTo(2);
        assertThat(result.day3Escalations()).isZero();
        verify(notificationService, times(2)).publishDaily(
            any(), eq(Types.KPI_DUE_SOON), eq(NotificationService.KIND_FYI), any(),
            any(Long.class), any(), any(), any(), any());
        verify(notificationService, never()).publish(any(), any(), any(), any(), any(Long.class), any(), any(), any());
        verify(auditLogService, times(2)).append(any(AuditLog.class));
    }

    /* ============== 5. deadline 后 1 天 → scanMonthlyDeadlines ============== */

    @Test
    @DisplayName("[HIGH-4.1-5] deadline 后 1 天：scanMonthlyDeadlines 触发 KPI_SHARED_DEADLINE_DAY1")
    void scanMonthlyDeadlines_dayAfterDeadline_reminds() {
        Project project = project();
        when(systemConfigService.getIntValue("kpi.monthlyDeadlineDay", 5)).thenReturn(5);
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
        // 9 月归集 → 次月截止 10/8 → 后 1 天 = 10/9
        LocalDate scanDate = LocalDate.of(2026, 10, 9);

        KpiSharedCollectionService.DeadlineScanResult result = service.scanMonthlyDeadlines(
            scanDate, YearMonth.of(2026, 9));

        assertThat(result.day1Reminders()).isEqualTo(2);
        assertThat(result.day3Escalations()).isZero();
        verify(notificationService, times(2)).publish(
            any(), eq("KPI_SHARED_DEADLINE_DAY1"), eq("ACTION"), any(),
            any(Long.class), any(), any(), any());
        verify(notificationService, never()).publishDaily(any(), any(), any(), any(), any(Long.class), any(), any(), any(), any());
    }

    /* ============== helpers ============== */

    private static LinkedHashMap<String, Object> configView(String value, String resolvedFrom, int version) {
        LinkedHashMap<String, Object> v = new LinkedHashMap<>();
        v.put("key", "kpi.monthlyDeadlineDay");
        v.put("value", value);
        v.put("resolvedFrom", resolvedFrom);
        v.put("version", version);
        return v;
    }

    private static java.util.Optional<String> opt(String s) {
        return s == null ? java.util.Optional.empty() : java.util.Optional.of(s);
    }

    private static Project project() {
        return Project.builder().id(1L).mainGroupId(10L).status("ACTIVE").build();
    }

    private static ProjectMember member(Long id, String role, Long groupId) {
        return ProjectMember.builder().projectId(1L).personId(id).role(role).build();
    }

    private static Person person(Long id, String type, Long groupId) {
        return Person.builder().id(id).personType(type).groupId(groupId).build();
    }

    private static ProductGroup group(Long id, Long leaderId) {
        return ProductGroup.builder().id(id).leaderPersonId(leaderId).build();
    }
}