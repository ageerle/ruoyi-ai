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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SEC-REV round 2：BidInvitationService 安全修复回归（3 项）：
 * <ul>
 *   <li>Bug#4 高危：admin-assign 状态门禁 —— 拒绝在 OPEN/SELECTED 上强制指派</li>
 *   <li>Bug#4 高危：admin-assign 年龄判定 —— 挂起不足 30 日禁止强制指派</li>
 *   <li>Bug#4 高危：admin-assign targetPersonId 必填</li>
 *   <li>Bug#5 中危：escape() 处理 \n \r \t \b \f + U+0000..U+001F</li>
 *   <li>Bug#6 中危：admin-assign 兄弟路径门禁对等 —— 通知 BID_WON/BID_LOST</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidSecurityRound2Test {

    @Mock
    private BidInvitationMapper bidInvitationMapper;
    @Mock
    private BidResponseMapper bidResponseMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private BidInvitationService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
        TableInfoHelper.initTableInfo(assistant, BidResponse.class);
    }

    @BeforeEach
    void setUp() {
        service = new BidInvitationService(bidInvitationMapper, bidResponseMapper,
            auditLogService, notificationService);
    }

    private BidInvitation expiredInvitation(long expireAgeDays) {
        BidInvitation inv = new BidInvitation();
        inv.setId(99L);
        inv.setTitle("测试招标");
        inv.setCreateBy(7L);
        inv.setStatus("EXPIRED");
        Date expireAt = new Date(System.currentTimeMillis() - expireAgeDays * 24L * 60 * 60 * 1000L);
        inv.setExpireAt(expireAt);
        inv.setUpdateTime(expireAt);
        return inv;
    }

    // ==================== Bug#4：状态门禁 ====================

    @Test
    @DisplayName("Bug#4: admin-assign 在 OPEN 状态被拒绝（状态门禁）")
    void adminAssign_rejectsOpenStatus() {
        BidInvitation inv = expiredInvitation(31);
        inv.setStatus("OPEN");
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);

        assertThatThrownBy(() -> service.adminAssign(99L, 123L, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        verify(bidInvitationMapper, never()).updateById(any(BidInvitation.class));
        verify(auditLogService, never()).append(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Bug#4: admin-assign 在 SELECTED 状态被拒绝（状态门禁）")
    void adminAssign_rejectsSelectedStatus() {
        BidInvitation inv = expiredInvitation(31);
        inv.setStatus("SELECTED");
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);

        assertThatThrownBy(() -> service.adminAssign(99L, 123L, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    // ==================== Bug#4：年龄判定 ====================

    @Test
    @DisplayName("Bug#4: admin-assign 挂起不足 30 日被拒绝（年龄判定）")
    void adminAssign_rejectsInsufficientAge() {
        BidInvitation inv = expiredInvitation(5);
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);

        assertThatThrownBy(() -> service.adminAssign(99L, 123L, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        verify(bidInvitationMapper, never()).updateById(any(BidInvitation.class));
        verifyNoInteractions(notificationService);
    }

    // ==================== Bug#4：targetPersonId 必填 ====================

    @Test
    @DisplayName("Bug#4: admin-assign targetPersonId 为 null 被拒绝")
    void adminAssign_rejectsNullTarget() {
        BidInvitation inv = expiredInvitation(31);
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);

        assertThatThrownBy(() -> service.adminAssign(99L, null, 1L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    // ==================== Bug#6：兄弟路径通知对等 ====================

    @Test
    @DisplayName("Bug#6: admin-assign 成功 ⇒ 通知 targetPersonId BID_WON")
    void adminAssign_publishesBidWonToTarget() {
        BidInvitation inv = expiredInvitation(31);
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of());

        BidInvitation out = service.adminAssign(99L, 123L, 1L);

        assertThat(out.getStatus()).isEqualTo("SELECTED");
        verify(notificationService).publish(eq(123L),
            eq(NotificationService.Types.BID_WON),
            eq(NotificationService.KIND_ACTION),
            eq("bid_invitation"),
            eq(99L),
            anyString(),
            anyString(),
            anyString());
    }

    @Test
    @DisplayName("Bug#6: admin-assign 成功 ⇒ 其他 PENDING 应标者收到 BID_LOST")
    void adminAssign_publishesBidLostToLosers() {
        BidInvitation inv = expiredInvitation(31);
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);

        BidResponse loser1 = new BidResponse();
        loser1.setId(201L);
        loser1.setRdPmId(456L);
        loser1.setStatus("PENDING");
        BidResponse winner = new BidResponse();
        winner.setId(202L);
        winner.setRdPmId(123L);
        winner.setStatus("PENDING");
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loser1, winner));

        service.adminAssign(99L, 123L, 1L);

        verify(notificationService).publish(eq(123L),
            eq(NotificationService.Types.BID_WON), anyString(), anyString(), anyLong(),
            anyString(), anyString(), anyString());
        verify(notificationService).publish(eq(456L),
            eq(NotificationService.Types.BID_LOST), anyString(), anyString(), anyLong(),
            anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Bug#4: admin-assign 合法 EXPIRED + ≥ 30 日 + 有效 target ⇒ 状态 SELECTED + 审计")
    void adminAssign_happyPath() {
        BidInvitation inv = expiredInvitation(31);
        when(bidInvitationMapper.selectByIdForUpdate(99L)).thenReturn(inv);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of());

        BidInvitation out = service.adminAssign(99L, 123L, 1L);

        assertThat(out.getStatus()).isEqualTo("SELECTED");
        verify(bidInvitationMapper).updateById(any(BidInvitation.class));
        verify(auditLogService).append(any());
    }

    // ==================== Bug#5：JSON escape 控制字符 ====================

    @Test
    @DisplayName("Bug#5: escape 处理 \\n 换行")
    void escape_handlesNewline() {
        String escaped = BidInvitationService.escape("a\nb");
        assertThat(escaped).isEqualTo("a\\nb");
    }

    @Test
    @DisplayName("Bug#5: escape 处理 \\r 回车")
    void escape_handlesCarriageReturn() {
        String escaped = BidInvitationService.escape("a\rb");
        assertThat(escaped).isEqualTo("a\\rb");
    }

    @Test
    @DisplayName("Bug#5: escape 处理 \\t 制表符")
    void escape_handlesTab() {
        String escaped = BidInvitationService.escape("a\tb");
        assertThat(escaped).isEqualTo("a\\tb");
    }

    @Test
    @DisplayName("Bug#5: escape 处理 \\b \\f 与 U+0001 控制字符")
    void escape_handlesBackspaceFormfeedAndLowControl() {
        String escaped = BidInvitationService.escape("\b\f");
        assertThat(escaped).isEqualTo("\\b\\f\\u0001");
    }

    @Test
    @DisplayName("Bug#5: escape 保留 \\ 与 \" 原 escape 语义")
    void escape_preservesBackslashAndQuote() {
        String escaped = BidInvitationService.escape("a\\b\"c");
        assertThat(escaped).isEqualTo("a\\\\b\\\"c");
    }
}
