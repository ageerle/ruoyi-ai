package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.service.OverdueReminderService.ScanResult;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-4.4 深管逾期与轻管免打扰（AC-IPD-12 / AC-IPD-13）。
 *
 * <ul>
 *   <li>深管逾期 → 状态机唯一入口 transit(…, DELAYED, …, SYSTEM) 自动标记；</li>
 *   <li>深管逾期 → 主责人（在册未退出）每日 ACTION 通知，dedupKey 按日去重；</li>
 *   <li>轻管逾期 → 不标记、不提醒（免打扰）；</li>
 *   <li>已 DELAYED → 不重复标记，但每日仍续提醒；</li>
 *   <li>主责人已退出 → 只标记不通知；</li>
 *   <li>开关 action.overdueReminder.enabled=false → 扫描短路；</li>
 *   <li>transit 抛 ServiceException（项目不可写/乐观锁）→ 跳过标记不中断整轮提醒。</li>
 * </ul>
 */
@Tag("dev")
class P144AcceptanceTest {

    private StageActionMapper actionMapper;
    private StageActionService actionService;
    private ProjectMemberMapper memberMapper;
    private NotificationService notificationService;
    private SystemConfigService configService;
    private OverdueReminderService service;

    private final Date now = new Date();

    @BeforeEach
    void setUp() {
        actionMapper = mock(StageActionMapper.class);
        actionService = mock(StageActionService.class);
        memberMapper = mock(ProjectMemberMapper.class);
        notificationService = mock(NotificationService.class);
        configService = mock(SystemConfigService.class);
        when(configService.getBoolValue(anyString(), anyBoolean())).thenReturn(true);
        service = new OverdueReminderService(actionMapper, actionService,
            memberMapper, notificationService, configService);
    }

    /** 深管动作，dueDate 已过一天，默认已配在册未退出的主责人。 */
    private StageAction seedOverdueAction(String status) {
        StageAction a = StageAction.builder()
            .id(7L).projectId(100L).stageId(10L).actionCode("REQ_ANALYSIS")
            .actionName("需求分析报告").ownerRole("MARKET_PM")
            .depth("DEEP").status(status).isBlocking("1").version(0)
            .dueDate(new Date(now.getTime() - 86_400_000L))
            .build();
        when(actionMapper.selectList(any())).thenReturn(List.of(a));
        when(memberMapper.selectOne(any())).thenReturn(
            ProjectMember.builder().id(55L).projectId(100L).personId(9L)
                .role("MARKET_PM").build()); // exitDate null = 在册
        return a;
    }

    @Test
    @DisplayName("AC-IPD-12 深管逾期：状态机唯一入口自动标记 DELAYED（actor=SYSTEM）")
    void deepOverdue_autoMarksDelayed_viaStateMachine() {
        seedOverdueAction("NOT_STARTED");

        ScanResult r = service.scanForDate(now);

        verify(actionService).transit(eq(7L), eq("DELAYED"), contains("逾期"), eq("SYSTEM"));
        assertThat(r.scanned()).isEqualTo(1);
        assertThat(r.deepMarked()).isEqualTo(1);
        assertThat(r.markSkipped()).isZero();
    }

    @Test
    @DisplayName("AC-IPD-12 深管逾期：主责人收 ACTION_OVERDUE 通知（按日 dedup 通道）")
    void deepOverdue_notifiesOwner_viaDailyChannel() {
        seedOverdueAction("NOT_STARTED");

        ScanResult r = service.scanForDate(now);

        verify(notificationService).publishDaily(eq(9L),
            eq(NotificationService.Types.ACTION_OVERDUE),
            eq(NotificationService.KIND_ACTION),
            eq(OverdueReminderService.SOURCE_TYPE), eq(7L),
            contains("需求分析报告"), anyString(), anyString(), eq(now));
        assertThat(r.deepReminded()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-IPD-13 轻管逾期：不标记、不提醒（免打扰）")
    void lightOverdue_neverMarked_neverNotified() {
        seedOverdueAction("NOT_STARTED").setDepth("LIGHT");

        ScanResult r = service.scanForDate(now);

        verify(actionService, never()).transit(anyLong(), anyString(), anyString(), anyString());
        verify(notificationService, never()).publishDaily(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString(), any());
        assertThat(r.lightSkipped()).isEqualTo(1);
        assertThat(r.deepMarked()).isZero();
        assertThat(r.deepReminded()).isZero();
    }

    @Test
    @DisplayName("重复扫描：已 DELAYED 不重复标记，但每日仍续提醒（标记一次、提醒每日）")
    void alreadyDelayed_notRemarked_butStillReminded() {
        seedOverdueAction("DELAYED");

        ScanResult r = service.scanForDate(now);

        verify(actionService, never()).transit(anyLong(), anyString(), anyString(), anyString());
        verify(notificationService).publishDaily(eq(9L), anyString(), anyString(),
            anyString(), eq(7L), anyString(), anyString(), anyString(), eq(now));
        assertThat(r.deepMarked()).isZero();
        assertThat(r.deepReminded()).isEqualTo(1);
    }

    @Test
    @DisplayName("主责人已退出/缺失：标记照常，通知不发")
    void ownerMissing_markStillHappens_noNotification() {
        seedOverdueAction("NOT_STARTED");
        when(memberMapper.selectOne(any())).thenReturn(null); // 无在册主责人

        ScanResult r = service.scanForDate(now);

        verify(actionService).transit(eq(7L), eq("DELAYED"), anyString(), eq("SYSTEM"));
        verify(notificationService, never()).publishDaily(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString(), any());
        assertThat(r.deepMarked()).isEqualTo(1);
        assertThat(r.deepReminded()).isZero();
    }

    @Test
    @DisplayName("开关关闭（action.overdueReminder.enabled=false）：扫描短路不查库")
    void switchOff_scanSkippedEarly() {
        when(configService.getBoolValue(OverdueReminderService.CONFIG_ENABLED, true)).thenReturn(false);

        ScanResult r = service.scanForDate(now);

        verify(actionMapper, never()).selectList(any());
        assertThat(r.scanned()).isZero();
        assertThat(r.deepMarked()).isZero();
        assertThat(r.deepReminded()).isZero();
    }

    @Test
    @DisplayName("transit 冲突（项目不可写/乐观锁）：跳过标记，不中断整轮提醒")
    void transitConflict_markSkipped_reminderStillSent() {
        seedOverdueAction("NOT_STARTED");
        doThrow(new ServiceException("项目当前状态不可编辑")).when(actionService)
            .transit(anyLong(), anyString(), anyString(), anyString());

        ScanResult r = service.scanForDate(now);

        assertThat(r.markSkipped()).isEqualTo(1);
        assertThat(r.deepMarked()).isZero();
        verify(notificationService).publishDaily(eq(9L), anyString(), anyString(),
            anyString(), eq(7L), anyString(), anyString(), anyString(), eq(now));
        assertThat(r.deepReminded()).isEqualTo(1);
    }
}
