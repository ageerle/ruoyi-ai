package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 招标单服务单测（BR-TEAM-03/05；@Tag("dev") 必须）
 * PERF-P0-2 契约锁：expireOverdue 必须保持单 SQL 条件批量 UPDATE（零 selectCount、零逐条 update）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidInvitationServiceTest {

    @Mock
    private BidInvitationMapper bidInvitationMapper;
    @Mock
    private BidResponseMapper bidResponseMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private BidInvitationService service;

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存（LambdaUpdateWrapper.set/.eq 需列名解析），同 DeletionRequestServiceTest 模式 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
    }

    @BeforeEach
    void setUp() {
        service = new BidInvitationService(bidInvitationMapper, bidResponseMapper, auditLogService, notificationService);
    }

    @Test
    @DisplayName("过期扫描（PERF-P0-2 批量化）：单 SQL 条件 UPDATE，无应标计数、无逐条写")
    void expireOverdueSingleSqlUpdate() {
        when(bidInvitationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(7);

        int expired = service.expireOverdue();

        assertThat(expired).isEqualTo(7);
        // 条件批量 UPDATE 仅一次 mapper.update；应标计数（旧 N+1 路径）不得出现
        verify(bidInvitationMapper).update(any(), any(LambdaUpdateWrapper.class));
        verifyNoInteractions(bidResponseMapper);
    }

    @Test
    @DisplayName("无过期行时 affected=0 直接返回，不抛异常")
    void expireOverdueEmpty() {
        when(bidInvitationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThat(service.expireOverdue()).isZero();
        verifyNoInteractions(bidResponseMapper);
    }

    /* ----------------- ZK-IPD §四.1.3 招标到期无人应标提醒市场 PM ----------------- */

    @Test
    @DisplayName("ZK-IPD §四.1.3：到期无人应标 ⇒ 通知市场 PM（createBy）")
    void expireOverdueNotifiesMarketPm() {
        BidInvitation inv1 = new BidInvitation();
        inv1.setId(101L);
        inv1.setTitle("人脸门禁");
        inv1.setCreateBy(7L);
        BidInvitation inv2 = new BidInvitation();
        inv2.setId(102L);
        inv2.setTitle("智能会议");
        inv2.setCreateBy(9L);
        when(bidInvitationMapper.selectList(any())).thenReturn(java.util.List.of(inv1, inv2));
        when(bidInvitationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(2);

        int expired = service.expireOverdue();

        assertThat(expired).isEqualTo(2);
        // 通知两位发起人
        verify(notificationService).publish(
            org.mockito.ArgumentMatchers.eq(7L),
            org.mockito.ArgumentMatchers.eq(NotificationService.Types.BID_EXPIRED_NO_RESPONSE),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq("bid_invitation"),
            org.mockito.ArgumentMatchers.eq(101L),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        );
        verify(notificationService).publish(
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.eq(NotificationService.Types.BID_EXPIRED_NO_RESPONSE),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq("bid_invitation"),
            org.mockito.ArgumentMatchers.eq(102L),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("ZK-IPD §四.1.3：到期无行受影响 ⇒ 不发通知")
    void expireOverdueEmptyNoNotification() {
        when(bidInvitationMapper.selectList(any())).thenReturn(java.util.List.of());

        int expired = service.expireOverdue();

        assertThat(expired).isZero();
        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }
}
