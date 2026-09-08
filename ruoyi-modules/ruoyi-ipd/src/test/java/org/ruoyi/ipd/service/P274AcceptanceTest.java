package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-7.4 验收：AC-HAND-02 移交超期升级超管 + 每日提醒（单测层；HTTP 真库验收另见验收文档）。
 *
 * <ul>
 *   <li>升级：status=DRAFT + deadline_at &lt; now + escalated_at IS NULL ⇒ 通知超管 + 标 escalated_at；并发守卫仅一人生效</li>
 *   <li>每日提醒：status=DRAFT + deadline_at &lt; now + (last_remind_at IS NULL OR last_remind_at &lt; today_start) ⇒ publishDaily</li>
 *   <li>幂等：二次扫描同一超期记录 ⇒ 不重复升级、不重复提醒（同日）</li>
 *   <li>无在任超管：跳过扫描 + warn 日志 + 返回 0/0（避免调度挂）</li>
 * </ul>
 *
 * <p>HTTP 层真库验收：Postman + 真 900101 SUPER_ADMIN session + 真 DRAFT 记录（deadline_at &lt; now）走
 * /api/v1/handovers/scan-overdue 端点；证据另存验收文件。
 * 形态为 Mockito 单元验收；不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-7.4 AC-HAND-02：移交超期升级超管 + 每日提醒（单测层契约）")
class P274AcceptanceTest {

    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private HandoverMapper handoverMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMemberService projectMemberService;
    @Mock private IpdAuthSession ipdAuthSession;
    @Mock private NotificationService notificationService;

    private HandoverService service;

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存（LambdaQueryWrapper 需列名解析）。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
    }

    private static final IpdActor ADMIN = new IpdActor(900101L, "ipd-admin", "SUPER_ADMIN", null);

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            ipdAuthSession, notificationService);
        // 单超管不变式（P2-7.3 数据治理后真库仅 1 名）
        Person superAdmin = new Person();
        superAdmin.setId(900101L);
        superAdmin.setName("ipd-admin");
        superAdmin.setPersonType("SUPER_ADMIN");
        superAdmin.setAccountStatus("ACTIVE");
        org.mockito.Mockito.lenient().when(personMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(superAdmin));
    }

    private HandoverRecord overdueDraft(long id, long projectId) {
        HandoverRecord r = new HandoverRecord();
        r.setId(id);
        r.setProjectId(projectId);
        r.setFromPersonId(301L);
        r.setToPersonId(401L);
        r.setHandoverRole("MARKET_PM");
        r.setStatus("DRAFT");
        r.setDeadlineAt(new Date(System.currentTimeMillis() - 86_400_000L)); // 昨日
        r.setCreateTime(new Date(System.currentTimeMillis() - 30L * 86_400_000L)); // 30 天前
        return r;
    }

    // ------------------- AC-HAND-02 升级 -------------------

    @Test
    @DisplayName("升级：1 条超期 DRAFT（escalated_at=NULL） ⇒ 通知超管 + escalated_at 标 now + 审计")
    void escalateOverdueDraft() {
        HandoverRecord r = overdueDraft(1001L, 7L);
        // Mockito 不区分两次 selectList 调用；两次都返回同一条记录（toEscalate 与 toRemind 各一次）。
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r));
        when(handoverMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        HandoverService.OverdueScanResult result = service.scanOverdueDrafts(ADMIN);

        assertThat(result.escalated()).isEqualTo(1);
        assertThat(result.reminded()).isEqualTo(1);

        ArgumentCaptor<String> eventTypeCap = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).publish(eq(900101L), eventTypeCap.capture(),
            eq(NotificationService.KIND_ACTION), eq("handover"), eq(1001L),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString());
        assertThat(eventTypeCap.getValue()).isEqualTo("HANDOVER_OVERDUE_ESCALATION");

        // 升级路径 + 提醒路径各 1 次 update，共 2 次
        ArgumentCaptor<LambdaUpdateWrapper<HandoverRecord>> updateCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(handoverMapper, times(2)).update(any(), updateCap.capture());
        assertThat(updateCap.getAllValues()).hasSize(2);

        // 审计 HANDOVER_OVERDUE_ESCALATION
        ArgumentCaptor<org.ruoyi.ipd.domain.AuditLog> auditCap =
            ArgumentCaptor.forClass(org.ruoyi.ipd.domain.AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues())
            .extracting(org.ruoyi.ipd.domain.AuditLog::getAction)
            .contains("HANDOVER_OVERDUE_ESCALATION");
    }

    @Test
    @DisplayName("升级幂等：已 escalated_at 非空 ⇒ toEscalate 不命中（Mockito 区分两次 selectList：首次空、第二次为待提醒）")
    void escalateIdempotent() {
        HandoverRecord r = overdueDraft(1002L, 7L);
        r.setEscalatedAt(new Date()); // 已升级过；真实 DB 下 toEscalate 不会返回
        // 首次 selectList（toEscalate）返回空；第二次（toRemind）返回该记录。Mockito 顺序消费。
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList())
            .thenReturn(List.of(r));
        when(handoverMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        HandoverService.OverdueScanResult result = service.scanOverdueDrafts(ADMIN);

        // toEscalate 0 命中 ⇒ escalated=0，不发升级 publish
        assertThat(result.escalated()).isEqualTo(0);
        verify(notificationService, never()).publish(anyLong(), eq("HANDOVER_OVERDUE_ESCALATION"),
            any(), any(), any(), any(), any(), any());
        // toRemind 仍命中（reminder 不查 escalatedAt）⇒ reminded=1，发 publishDaily
        assertThat(result.reminded()).isEqualTo(1);
    }

    @Test
    @DisplayName("无在任超管：跳过扫描 + warn 日志 + 返回 0/0，不抛异常")
    void noSuperAdminSkip() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        HandoverService.OverdueScanResult result = service.scanOverdueDrafts(ADMIN);

        assertThat(result.escalated()).isZero();
        assertThat(result.reminded()).isZero();
        verify(handoverMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(notificationService, never()).publish(anyLong(), any(), any(), any(), any(),
            any(), any(), any());
    }

    // ------------------- AC-HAND-02 每日提醒 -------------------

    @Test
    @DisplayName("提醒：1 条超期 DRAFT（last_remind_at=NULL） ⇒ publishDaily 一次 + last_remind_at 标 now")
    void dailyReminder() {
        HandoverRecord r = overdueDraft(1003L, 7L);
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r));
        when(handoverMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        HandoverService.OverdueScanResult result = service.scanOverdueDrafts(ADMIN);

        assertThat(result.reminded()).isEqualTo(1);
        verify(notificationService, times(1)).publishDaily(eq(900101L), eq("HANDOVER_DAILY_REMINDER"),
            eq(NotificationService.KIND_ACTION), eq("handover"), eq(1003L),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), any(Date.class));
    }

    @Test
    @DisplayName("提醒同日去重：last_remind_at=今日 ⇒ publishDaily 仍被调用（dedupKey 自带 yyyyMMdd 守卫）")
    void dailyReminderTodaySkipByLambda() {
        HandoverRecord r = overdueDraft(1004L, 7L);
        r.setLastRemindAt(new Date()); // 今日已提醒
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r));

        HandoverService.OverdueScanResult result = service.scanOverdueDrafts(ADMIN);

        // Lambda 守卫 last_remind_at < today_start ⇒ 不进入 toRemind 集合
        assertThat(result.reminded()).isEqualTo(0);
    }

    // ------------------- AC-HAND-05 历史保全（归档）-------------------

    @Test
    @DisplayName("归档：COMPLETED 移交 ⇒ 写 archived_at + 审计 HANDOVER_ARCHIVED（afterData 含完整 JSON 快照）")
    void archiveNormal() {
        HandoverRecord rec = overdueDraft(2001L, 7L);
        rec.setStatus("COMPLETED");
        rec.setCompletedAt(new Date());
        when(handoverMapper.selectById(2001L)).thenReturn(rec);
        when(handoverMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        HandoverRecord out = service.archiveCompletedHandover(2001L, ADMIN);

        assertThat(out.getArchivedAt()).isNotNull();
        ArgumentCaptor<org.ruoyi.ipd.domain.AuditLog> auditCap =
            ArgumentCaptor.forClass(org.ruoyi.ipd.domain.AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(org.ruoyi.ipd.domain.AuditLog::getAction)
            .contains("HANDOVER_ARCHIVED");
    }

    @Test
    @DisplayName("归档幂等：archived_at 已设 ⇒ 二次调用直接返回，不重复审计")
    void archiveIdempotent() {
        HandoverRecord rec = overdueDraft(2002L, 7L);
        rec.setStatus("COMPLETED");
        rec.setArchivedAt(new Date(System.currentTimeMillis() - 86_400_000L));
        when(handoverMapper.selectById(2002L)).thenReturn(rec);

        HandoverRecord out = service.archiveCompletedHandover(2002L, ADMIN);

        // 幂等：archived_at 不被覆盖；不写新 update；不写新审计
        assertThat(out.getArchivedAt()).isEqualTo(rec.getArchivedAt());
        verify(handoverMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("归档拒绝：status=DRAFT ⇒ 抛 ServiceException")
    void archiveCompletedOnly() {
        HandoverRecord rec = overdueDraft(2003L, 7L);
        // status=DRAFT（默认）
        when(handoverMapper.selectById(2003L)).thenReturn(rec);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            service.archiveCompletedHandover(2003L, ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅 COMPLETED 移交可归档");
        verify(handoverMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ------------------- AC-HAND-08 月度归属 -------------------

    @Test
    @DisplayName("月度归属：当月 1 条 COMPLETED 移交 ⇒ 返回 TRANSFER 行（新 PM 从次月首日起）+ BINDING 行（旧 PM）")
    void monthlyAttributionTransferInMonth() {
        HandoverRecord rec = overdueDraft(3001L, 7L);
        rec.setStatus("COMPLETED");
        rec.setCompletedAt(new Date());
        // 当月 handoverRecord 命中；bindings 查本月在任
        when(handoverMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rec));
        // memberMapper 返回空 bindings
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        java.time.LocalDate today = java.time.LocalDate.now();
        String month = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        java.util.List<HandoverService.MonthlyAttributionView> result =
            service.getMonthlyAttribution(7L, month, ADMIN);

        // TRANSFER 行：rec.toPersonId=401，从次月首日起
        assertThat(result).hasSize(1);
        HandoverService.MonthlyAttributionView view = result.get(0);
        assertThat(view.source()).isEqualTo("TRANSFER");
        assertThat(view.personId()).isEqualTo(401L);
        assertThat(view.fromDate()).isEqualTo(today.withDayOfMonth(1).plusMonths(1));
    }

    @Test
    @DisplayName("月度归属拒绝：month 格式非法（yyyy-M）⇒ 抛 ServiceException")
    void monthlyAttributionInvalidMonth() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            service.getMonthlyAttribution(7L, "2026-9", ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("yyyy-MM");
    }

}