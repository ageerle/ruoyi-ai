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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-5.2 selectResponse confirmToken 验收测试
 *
 * <p>6 字符 token + 24h 过期校验：
 * <ol>
 *   <li>缺 token → PARAM_INVALID</li>
 *   <li>token 不匹配 → PARAM_INVALID</li>
 *   <li>token 已过期 → STATE_CONFLICT</li>
 *   <li>token 正确 + 未过期 → 遴选成功，invitation.confirmToken 清空</li>
 *   <li>issueConfirmToken 生成 6 字符 + 24h expiresAt</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidInvitationSelectConfirmTokenAcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private BidInvitationService bidInvitationService;

    private BidInvitation invitation;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
        TableInfoHelper.initTableInfo(assistant, BidResponse.class);
    }

    @BeforeEach
    void setUp() {
        bidInvitationService = new BidInvitationService(
            bidInvitationMapper, bidResponseMapper, auditLogService, notificationService);
        invitation = BidInvitation.builder()
            .id(5001L).projectId(100L).mode("PUBLIC").title("P1-5.2 验证").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .confirmToken("ABC234")
            .confirmTokenExpires(new Date(System.currentTimeMillis() + 24 * 3600 * 1000L))
            .build();
    }

    @Test
    @DisplayName("AC#1 selectResponse 缺 confirmToken ⇒ PARAM_INVALID")
    void selectMissingToken_rejected() {
        when(bidInvitationMapper.selectByIdForUpdate(5001L)).thenReturn(invitation);
        assertThatThrownBy(() -> bidInvitationService.selectResponse(5001L, 2001L, null, 700L))
            .isInstanceOf(IpdBusinessException.class)
            .matches(e -> ((IpdBusinessException) e).getErrorCode() == ApiV1ErrorCode.PARAM_INVALID);
        verify(bidResponseMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("AC#2 selectResponse confirmToken 不匹配 ⇒ PARAM_INVALID")
    void selectWrongToken_rejected() {
        when(bidInvitationMapper.selectByIdForUpdate(5001L)).thenReturn(invitation);
        assertThatThrownBy(() -> bidInvitationService.selectResponse(5001L, 2001L, "WRONG99", 700L))
            .isInstanceOf(IpdBusinessException.class)
            .matches(e -> ((IpdBusinessException) e).getErrorCode() == ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("AC#3 selectResponse confirmToken 已过期 ⇒ STATE_CONFLICT")
    void selectExpiredToken_rejected() {
        invitation.setConfirmTokenExpires(new Date(System.currentTimeMillis() - 1000L));
        when(bidInvitationMapper.selectByIdForUpdate(5001L)).thenReturn(invitation);
        assertThatThrownBy(() -> bidInvitationService.selectResponse(5001L, 2001L, "ABC234", 700L))
            .isInstanceOf(IpdBusinessException.class)
            .matches(e -> ((IpdBusinessException) e).getErrorCode() == ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("AC#4 selectResponse confirmToken 正确且未过期 ⇒ SELECTED 成功 + confirmToken 清空")
    void selectCorrectToken_succeeds() {
        BidResponse winner = BidResponse.builder()
            .id(2001L).invitationId(5001L).rdPmId(200L).status("PENDING").build();
        when(bidInvitationMapper.selectByIdForUpdate(5001L)).thenReturn(invitation);
        when(bidResponseMapper.selectById(2001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(java.util.List.of());
        BidInvitation result = bidInvitationService.selectResponse(5001L, 2001L, "ABC234", 700L);
        assertThat(result.getStatus()).isEqualTo("SELECTED");
        assertThat(result.getSelectedResponseId()).isEqualTo(2001L);
        assertThat(result.getConfirmToken()).isNull();
        assertThat(result.getConfirmTokenExpires()).isNull();
    }

    @Test
    @DisplayName("AC#5 issueConfirmToken 生成 6 字符 token + 24h expiresAt")
    void issueToken_generates6CharWith24hExpiry() {
        when(bidInvitationMapper.selectByIdForUpdate(5001L)).thenReturn(invitation);
        BidInvitationService.ConfirmTokenView view = bidInvitationService.issueConfirmToken(5001L);
        assertThat(view.token()).hasSize(6);
        assertThat(view.expiresAt()).isAfter(new Date(System.currentTimeMillis() + 23 * 3600 * 1000L));
        assertThat(invitation.getConfirmToken()).isEqualTo(view.token());
    }

    @Test
    @DisplayName("AC#6 newConfirmToken 仅从去歧义字符集生成（无 0/O/1/I/L）")
    void newConfirmToken_avoidsAmbiguousChars() {
        for (int i = 0; i < 50; i++) {
            String tok = BidInvitationService.newConfirmToken();
            assertThat(tok).hasSize(6);
            for (char c : tok.toCharArray()) {
                assertThat("01OIL".indexOf(c)).isEqualTo(-1);
            }
        }
    }
}