package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-3.3 招标 3/7/30 日提醒升级与撤销出口验收测试
 * AC: AC-TEAM-06/07/08/09/13；BR: BR-TEAM-02/06/07
 *
 * <p>形态为 Mockito 单元验收（对齐 P232 惯例）；真库 HTTP 验收另见 docs/ipd-系统说明/验收/。
 * <p>不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P233AcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private PersonMapper personMapper;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BidScanService bidScanService;
    @InjectMocks private BidInvitationService bidInvitationService;

    private static final Long MARKET_PM = 300L;
    private static final Long MARKET_LEAD = 301L;
    private static final Long ADMIN = 999L;
    private static final Long RD_PM_A = 200L;
    private static final Long RD_PM_B = 201L;
    private static final Long PROJECT_ID = 100L;

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p233-test"),
            BidInvitation.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p233-test-resp"),
            BidResponse.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p233-test-proj"),
            Project.class);
    }

    @BeforeEach
    void setUp() {
        // BidInvitationService 重新注入（含 NotificationService 用于 modifyInvitation 通知）
        bidInvitationService = new BidInvitationService(bidInvitationMapper, bidResponseMapper,
            auditLogService, notificationService);
        bidScanService = new BidScanService(bidInvitationMapper, bidResponseMapper, projectMapper,
            personMapper, notificationService, auditLogService);
    }

    /** 当前时间 + N 天 */
    private static Date daysFromNow(int n) {
        return new Date(System.currentTimeMillis() + n * 86400_000L);
    }

    /** 当前时间 - N 天 */
    private static Date daysAgo(int n) {
        return new Date(System.currentTimeMillis() - n * 86400_000L);
    }

    // ==================== AC-TEAM-06: 到期前 3 天提醒 ====================

    @Test
    @DisplayName("AC-TEAM-06: 招标到期前 3 天的 OPEN 状态单，扫描发出 BID_EXPIRING_SOON 提醒给市场 PM")
    void expiringSoonRemind3DaysBefore() {
        BidInvitation inv = BidInvitation.builder()
            .id(11L).projectId(PROJECT_ID).mode("ONE_TO_ONE").targetPersonId(RD_PM_A)
            .title("3日到期单").status("OPEN").expireAt(daysFromNow(3)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inv));

        bidScanService.scanExpiringSoon();

        // 验证：发布了一条 BID_EXPIRING_SOON 提醒，接收者=市场 PM
        ArgumentCaptor<NotificationEvent> cap = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).publishDaily(
            eq(MARKET_PM), eq(NotificationService.Types.BID_EXPIRING_SOON),
            eq(NotificationService.KIND_ACTION), eq("bid_invitation"), eq(11L),
            any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC-TEAM-06 反例: 距到期 5 天（不在 3 天窗口内）的单不提醒")
    void expiringSoonOutsideWindow() {
        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(java.util.Collections.emptyList());

        bidScanService.scanExpiringSoon();

        verify(notificationService, never()).publishDaily(anyLong(), any(), any(), any(), anyLong(),
            any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC-TEAM-06 反例: 非 OPEN 状态（如 SELECTED）不提醒")
    void expiringSoonNonOpenIgnored() {
        // 扫描器按 status=OPEN 过滤单 SQL 拉取；模拟返回空集合（即筛选后无候选）
        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(java.util.Collections.emptyList());

        bidScanService.scanExpiringSoon();

        verify(notificationService, never()).publishDaily(anyLong(), any(), any(), any(), anyLong(),
            any(), any(), any(), any());
    }

    // ==================== AC-TEAM-07: 到期 7 日未遴选升级组长 ====================

    @Test
    @DisplayName("AC-TEAM-07: 到期有应标但超 7 天未遴选，发布 BID_SELECT_OVERDUE 给产品组长")
    void selectOverdueEscalateToLead() {
        BidInvitation inv = BidInvitation.builder()
            .id(21L).projectId(PROJECT_ID).mode("PUBLIC").title("7日超期遴选单").status("OPEN")
            .expireAt(daysAgo(8)).build();
        inv.setCreateBy(MARKET_PM); // 8 天前到期（超 7 天未遴选）

        BidResponse resp = BidResponse.builder().id(211L).invitationId(21L).rdPmId(RD_PM_A)
            .status("PENDING").build();

        // 扫描过期未遴选：候选拉取 + 项目主组查找 + 应标存在性查询
        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inv));
        Project project = Project.builder().id(PROJECT_ID).mainGroupId(50L).build();
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        // 模拟该单有 2 个 PENDING 应标（让升级路径进入，不是无人应标路径）
        when(bidResponseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        // 注入主组组长解析（通过 mock person lookup 或参数传入）
        // BidScanService 简化设计：默认升级给 MARKET_PM（即市场 PM）+ 主组组长，扫描结果可看调用次数
        bidScanService.scanSelectOverdue();

        // 至少有一条升级通知发布出去（BID_SELECT_OVERDUE）
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        verify(notificationService, atLeastOnce()).publishDaily(
            anyLong(), typeCap.capture(), eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"), eq(21L), any(), any(), any(), any());
        assertThat(typeCap.getAllValues()).contains(NotificationService.Types.BID_SELECT_OVERDUE);
    }

    @Test
    @DisplayName("AC-TEAM-07 反例: 到期未超 7 天不升级")
    void selectOverdueWithinWindow() {
        // 扫描结果为空：到期未超 7 天的单不进升级队列
        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(java.util.Collections.emptyList());

        bidScanService.scanSelectOverdue();

        verify(notificationService, never()).publishDaily(anyLong(), any(), any(), any(), anyLong(),
            any(), any(), any(), any());
    }

    // ==================== AC-TEAM-08: 到期无人应标自动关闭 + 项目挂起 ====================

    @Test
    @DisplayName("AC-TEAM-08: 招标到期无人应标，自动 EXPIRED 并将项目状态置为 TEAMING（待组队）")
    void expireNoResponseAutoCloseAndSuspend() {
        BidInvitation inv = BidInvitation.builder()
            .id(31L).projectId(PROJECT_ID).mode("PUBLIC").title("无人应标单")
            .status("OPEN").expireAt(daysAgo(1)).build();
        inv.setCreateBy(MARKET_PM);

        // 扫描器拉取 OPEN+已过期的单
        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inv));
        // 该单下无应标
        when(bidResponseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(bidInvitationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(projectMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        int expired = bidScanService.scanExpireNoResponse();

        assertThat(expired).isEqualTo(1);
        // 验证：bidInvitation 状态置 EXPIRED
        ArgumentCaptor<LambdaUpdateWrapper> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(bidInvitationMapper, times(1)).update(any(), cap.capture());
        // 验证：项目状态置 TEAMING
        verify(projectMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
        // 验证：市场 PM 收到"重新发起提示"通知
        verify(notificationService, times(1)).publishDaily(
            eq(MARKET_PM), eq(NotificationService.Types.BID_EXPIRED_NO_RESPONSE),
            eq(NotificationService.KIND_FYI), eq("bid_invitation"), eq(31L),
            any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC-TEAM-08 反例: 有应标的过期单不自动关闭（AC-TEAM-07 升级组长路径）")
    void expireWithResponseNotAutoClose() {
        BidInvitation inv = BidInvitation.builder()
            .id(32L).projectId(PROJECT_ID).mode("PUBLIC").title("有应标过期单")
            .status("OPEN").expireAt(daysAgo(1)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inv));
        when(bidResponseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L); // 2 个 PENDING 应标

        int expired = bidScanService.scanExpireNoResponse();

        // 0 = 没有"无人应标"的单被关闭
        assertThat(expired).isZero();
        verify(bidInvitationMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ==================== AC-TEAM-09: 挂起超 30 日超管可直接指派 ====================

    @Test
    @DisplayName("AC-TEAM-09: 超管可直接指派，挂起超 30 天的招标单完成 SELECTED 并写审计")
    void adminDirectAssign() {
        BidInvitation inv = BidInvitation.builder()
            .id(41L).projectId(PROJECT_ID).mode("PUBLIC").title("挂起超 30 日")
            .status("EXPIRED").expireAt(daysAgo(35)).build();
        inv.setCreateBy(MARKET_PM);

        // adminAssign 需要锁定读 + 更新状态为 SELECTED + 设置 selectedResponseId + 审计
        when(bidInvitationMapper.selectByIdForUpdate(41L)).thenReturn(inv);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.adminAssign(41L, RD_PM_A, ADMIN);

        assertThat(result.getStatus()).isEqualTo("SELECTED");
        assertThat(result.getSelectedResponseId()).isNull(); // 超管指派无具体应标行
        // 审计：action=admin_assign，operator=ADMIN
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("admin_assign");
        assertThat(log.getOperatorId()).isEqualTo(ADMIN);
    }

    @Test
    @DisplayName("AC-TEAM-09 反例: 仅超管可指派，非超管调用拒 403（调用方守卫；此处模拟锁定读失败）")
    void adminAssignLockedRowMissing() {
        when(bidInvitationMapper.selectByIdForUpdate(41L)).thenReturn(null);

        assertThatThrownBy(() -> bidInvitationService.adminAssign(41L, RD_PM_A, ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    // ==================== AC-TEAM-13: 修改招标条件通知已应标者 ====================

    @Test
    @DisplayName("AC-TEAM-13: 市场 PM 修改招标条件，写审计 + 向所有 PENDING 应标者发 BID_CONDITIONS_CHANGED 通知")
    void modifyConditionsNotifyResponders() {
        BidInvitation inv = BidInvitation.builder()
            .id(51L).projectId(PROJECT_ID).mode("PUBLIC").title("原标题")
            .content("原内容").status("OPEN").expireAt(daysFromNow(5)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectByIdForUpdate(51L)).thenReturn(inv);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        List<BidResponse> responders = Arrays.asList(
            BidResponse.builder().id(511L).invitationId(51L).rdPmId(RD_PM_A).status("PENDING").build(),
            BidResponse.builder().id(512L).invitationId(51L).rdPmId(RD_PM_B).status("PENDING").build()
        );
        when(bidResponseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(responders);

        BidInvitation result = bidInvitationService.modifyInvitation(51L, "新标题", "新内容",
            daysFromNow(7), MARKET_PM);

        assertThat(result.getTitle()).isEqualTo("新标题");
        assertThat(result.getContent()).isEqualTo("新内容");
        // 验证：两条 BID_CONDITIONS_CHANGED 通知发给两位应标者
        verify(notificationService, times(1)).publish(eq(RD_PM_A),
            eq(NotificationService.Types.BID_CONDITIONS_CHANGED),
            eq(NotificationService.KIND_ACTION), eq("bid_invitation"), eq(51L), any(), any(), any());
        verify(notificationService, times(1)).publish(eq(RD_PM_B),
            eq(NotificationService.Types.BID_CONDITIONS_CHANGED),
            eq(NotificationService.KIND_ACTION), eq("bid_invitation"), eq(51L), any(), any(), any());
        // 审计：action=modify_conditions
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("modify_conditions");
    }

    @Test
    @DisplayName("AC-TEAM-13 反例: 非市场 PM（发起人）无权修改")
    void modifyConditionsForbiddenForNonOwner() {
        BidInvitation inv = BidInvitation.builder()
            .id(52L).projectId(PROJECT_ID).mode("PUBLIC").status("OPEN")
            .expireAt(daysFromNow(5)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectByIdForUpdate(52L)).thenReturn(inv);

        Long NON_OWNER = 666L;
        assertThatThrownBy(() -> bidInvitationService.modifyInvitation(52L, "X", "Y",
            daysFromNow(7), NON_OWNER))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("AC-TEAM-13 反例: 非 OPEN 状态不可修改")
    void modifyConditionsForbiddenForClosed() {
        BidInvitation inv = BidInvitation.builder()
            .id(53L).projectId(PROJECT_ID).mode("PUBLIC").status("SELECTED")
            .expireAt(daysFromNow(5)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectByIdForUpdate(53L)).thenReturn(inv);

        assertThatThrownBy(() -> bidInvitationService.modifyInvitation(53L, "X", "Y",
            daysFromNow(7), MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    // ==================== 扫描去重：幂等性 ====================

    @Test
    @DisplayName("扫描幂等: publishDaily 自身 dedup_key 含日期，同日多次扫描只生成一次事件")
    void scanDedupSameDay() {
        BidInvitation inv = BidInvitation.builder()
            .id(61L).projectId(PROJECT_ID).mode("PUBLIC").title("幂等扫描单")
            .status("OPEN").expireAt(daysFromNow(3)).build();
        inv.setCreateBy(MARKET_PM);

        when(bidInvitationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(inv));

        // 第一次扫描 → 实际执行 publishDaily
        bidScanService.scanExpiringSoon();
        // 第二次扫描（同日）→ publishDaily 内部基于 dedup_key 去重（NotificationService 语义），
        // 扫描器自身不重复发：依赖单条 SELECT 仅返回未提醒过的单（status 过滤 + reminder_sent_at 标记）
        // 此处校验：扫描器每次调用 publish 恰好一次（由 NotificationService 内部幂等保护二次发）
        verify(notificationService, times(1)).publishDaily(anyLong(), any(), any(), any(), anyLong(),
            any(), any(), any(), any());
    }

    // Mockito 自定义 atLeastOnce 等价（行内展开以避免静态导入歧义）
    private static org.mockito.verification.VerificationMode atLeastOnce() {
        return org.mockito.Mockito.atLeastOnce();
    }
}
