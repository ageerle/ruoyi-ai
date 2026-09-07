package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIGH-1.2 selectResponse 落选通知验收测试
 * <p>
 * 镜像 adminAssign 行 332-355 模式：中标者 ⇒ BID_WON；其余 PENDING 应标者 ⇒ BID_LOST。
 * NotificationService.publish 内置 dedupKey 幂等（receiver 维度去重）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidInvitationSelectResponseLostNotificationAcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    @InjectMocks private BidInvitationService bidInvitationService;

    private static final Long MARKET_PM = 300L;
    private static final Long WINNER_RD_PM = 200L;
    private static final Long LOSER1_RD_PM = 201L;
    private static final Long LOSER2_RD_PM = 202L;
    private static final Long ONE_TO_ONE_TARGET_RD_PM = 203L;
    private static final Long EXTRA_LOSER_RD_PM = 204L;

    private BidInvitation publicInvitation;
    private BidInvitation oneToOneInvitation;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
        TableInfoHelper.initTableInfo(assistant, BidResponse.class);
    }

    @BeforeEach
    void setUp() {
        publicInvitation = BidInvitation.builder()
            .id(1002L).projectId(100L).mode("PUBLIC").targetPersonId(null)
            .title("研发PM公开招标-项目B").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .build();
        publicInvitation.setCreateBy(MARKET_PM);

        oneToOneInvitation = BidInvitation.builder()
            .id(1003L).projectId(100L).mode("ONE_TO_ONE").targetPersonId(ONE_TO_ONE_TARGET_RD_PM)
            .title("研发PM一对一招标-项目C").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .build();
        oneToOneInvitation.setCreateBy(MARKET_PM);
    }

    /**
     * AC#1：公开招标 3 人遴选 1 人中标 ⇒ 其余 2 人各收 1 条 BID_LOST，中标者收 1 条 BID_WON。
     */
    @Test
    @DisplayName("AC#1 公开招标 3 应标 1 中标：2 loser 各收 1 BID_LOST + winner 收 1 BID_WON")
    void publicTender_threeCandidates_eachLoserReceivesBidLost() {
        when(bidInvitationMapper.selectByIdForUpdate(1002L)).thenReturn(publicInvitation);
        BidResponse winner = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(WINNER_RD_PM).status("PENDING").build();
        BidResponse loser1 = BidResponse.builder()
            .id(2002L).invitationId(1002L).rdPmId(LOSER1_RD_PM).status("PENDING").build();
        BidResponse loser2 = BidResponse.builder()
            .id(2003L).invitationId(1002L).rdPmId(LOSER2_RD_PM).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loser1, loser2));
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);
        when(bidResponseMapper.update(any(), any())).thenReturn(2);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        bidInvitationService.selectResponse(1002L, 2001L, MARKET_PM);

        // 中标者 ⇒ BID_WON
        verify(notificationService).publish(
            eq(WINNER_RD_PM),
            eq(NotificationService.Types.BID_WON),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(1002L),
            anyString(),
            anyString(),
            anyString()
        );
        // 两个 loser 各 ⇒ 1 BID_LOST
        verify(notificationService).publish(
            eq(LOSER1_RD_PM),
            eq(NotificationService.Types.BID_LOST),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(1002L),
            anyString(),
            anyString(),
            anyString()
        );
        verify(notificationService).publish(
            eq(LOSER2_RD_PM),
            eq(NotificationService.Types.BID_LOST),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(1002L),
            anyString(),
            anyString(),
            anyString()
        );
        // 总 3 条通知：1 winner BID_WON + 2 loser BID_LOST
        verify(notificationService, times(3)).publish(
            anyLong(), anyString(), anyString(), anyString(), anyLong(),
            anyString(), anyString(), anyString()
        );
    }

    /**
     * AC#2：ONE_TO_ONE 招标 2 应标 1 中标 ⇒ 1 loser 收 1 条 BID_LOST。
     * 注：业务上一对一招标只允许 target_person_id 应标；此处 2 应标者只是为测出 1 loser 场景。
     */
    @Test
    @DisplayName("AC#2 ONE_TO_ONE 招标 2 应标 1 中标：1 loser 收 1 BID_LOST")
    void oneToOneTender_oneLoser_receivesBidLost() {
        when(bidInvitationMapper.selectByIdForUpdate(1003L)).thenReturn(oneToOneInvitation);
        BidResponse winner = BidResponse.builder()
            .id(3001L).invitationId(1003L).rdPmId(ONE_TO_ONE_TARGET_RD_PM).status("PENDING").build();
        BidResponse extraLoser = BidResponse.builder()
            .id(3002L).invitationId(1003L).rdPmId(EXTRA_LOSER_RD_PM).status("PENDING").build();
        when(bidResponseMapper.selectById(3001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(extraLoser));
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);
        when(bidResponseMapper.update(any(), any())).thenReturn(1);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        bidInvitationService.selectResponse(1003L, 3001L, MARKET_PM);

        verify(notificationService).publish(
            eq(ONE_TO_ONE_TARGET_RD_PM),
            eq(NotificationService.Types.BID_WON),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(1003L),
            anyString(), anyString(), anyString()
        );
        verify(notificationService).publish(
            eq(EXTRA_LOSER_RD_PM),
            eq(NotificationService.Types.BID_LOST),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(1003L),
            anyString(), anyString(), anyString()
        );
        // 总 2 条通知：1 winner + 1 loser
        verify(notificationService, times(2)).publish(
            anyLong(), anyString(), anyString(), anyString(), anyLong(),
            anyString(), anyString(), anyString()
        );
    }

    /**
     * AC#3：中标人不收 BID_LOST（只收 BID_WON）—— winner 不会被写进 losers 集合。
     */
    @Test
    @DisplayName("AC#3 中标人不收 BID_LOST：仅收 BID_WON")
    void winner_doesNotReceiveBidLost_onlyBidWon() {
        when(bidInvitationMapper.selectByIdForUpdate(1002L)).thenReturn(publicInvitation);
        BidResponse winner = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(WINNER_RD_PM).status("PENDING").build();
        BidResponse loser = BidResponse.builder()
            .id(2002L).invitationId(1002L).rdPmId(LOSER1_RD_PM).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loser));
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);
        when(bidResponseMapper.update(any(), any())).thenReturn(1);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        bidInvitationService.selectResponse(1002L, 2001L, MARKET_PM);

        // 中标人 NEVER 收 BID_LOST
        verify(notificationService, never()).publish(
            eq(WINNER_RD_PM),
            eq(NotificationService.Types.BID_LOST),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        );
        // 中标人恰好收 1 条 BID_WON
        verify(notificationService, times(1)).publish(
            eq(WINNER_RD_PM),
            eq(NotificationService.Types.BID_WON),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        );
    }

    /**
     * AC#4：重复遴选（幂等）⇒ 通知不重发。
     * 状态机在第二次调用时把 invitation 状态从 SELECTED 拦下（STATE_CONFLICT），
     * 保证同一个 selectResponse 链路只产生一组通知。
     */
    @Test
    @DisplayName("AC#4 重复遴选：状态机拦下第二次调用，通知不重发")
    void repeatSelectResponse_idempotentNoDuplicateNotification() {
        // 第二次 selectByIdForUpdate 返回 SELECTED 状态（模拟第一次已成功的副作用）
        when(bidInvitationMapper.selectByIdForUpdate(1002L))
            .thenReturn(publicInvitation)
            .thenReturn(buildSelectedInvitation(1002L, 2001L));
        BidResponse winner = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(WINNER_RD_PM).status("PENDING").build();
        BidResponse loser = BidResponse.builder()
            .id(2002L).invitationId(1002L).rdPmId(LOSER1_RD_PM).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loser));
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);
        when(bidResponseMapper.update(any(), any())).thenReturn(1);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        // 第一次调用：成功
        bidInvitationService.selectResponse(1002L, 2001L, MARKET_PM);
        // 第二次调用：状态机 STATE_CONFLICT 拒绝（invitation.status=SELECTED）
        assertThatThrownBy(() -> bidInvitationService.selectResponse(1002L, 2002L, MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        // 通知总数稳定为 2（第一次调用产生）：1 winner BID_WON + 1 loser BID_LOST
        verify(notificationService, times(1)).publish(
            eq(WINNER_RD_PM),
            eq(NotificationService.Types.BID_WON),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        );
        verify(notificationService, times(1)).publish(
            eq(LOSER1_RD_PM),
            eq(NotificationService.Types.BID_LOST),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()
        );
        verify(notificationService, times(2)).publish(
            anyLong(), anyString(), anyString(), anyString(), anyLong(),
            anyString(), anyString(), anyString()
        );
    }

    private BidInvitation buildSelectedInvitation(Long id, Long selectedResponseId) {
        BidInvitation inv = BidInvitation.builder()
            .id(id).projectId(100L).mode("PUBLIC").targetPersonId(null)
            .title("研发PM公开招标-项目B").status("SELECTED")
            .selectedResponseId(selectedResponseId)
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .build();
        inv.setCreateBy(MARKET_PM);
        return inv;
    }
}