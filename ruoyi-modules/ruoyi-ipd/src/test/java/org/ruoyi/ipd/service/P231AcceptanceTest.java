package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P2-3.1 招标域验收测试（AC-TEAM-03/05/08/13）
 *
 * <p>形态为 Mockito 单元验收；真库 HTTP 验收另见 evidence-p231-http-acceptance-*.json。
 * <p>不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P231AcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BidInvitationService bidInvitationService;

    private BidInvitation sampleInvitation;

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper.set 构造期解析列缓存，纯 Mockito 环境需先注册 TableInfo（仓内 P064/P171 同惯例；P2-3.2 起 expireOverdue/selectResponse 走 UpdateWrapper）
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p231-test-bid"),
            BidResponse.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p231-test-inv"),
            BidInvitation.class);
        sampleInvitation = BidInvitation.builder()
            .id(1001L)
            .projectId(100L)
            .mode("ONE_TO_ONE")
            .targetPersonId(200L)
            .title("研发PM招标-项目A")
            .content("需要一名研发PM负责项目A的技术推进")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .status("OPEN")
            .build();
        sampleInvitation.setCreateTime(new Date());
    }

    @Test
    @DisplayName("AC-TEAM-03 创建招标单：状态=OPEN，targetPersonId 非空（ONE_TO_ONE）")
    void createInvitation_oneToOne_statusOpen() {
        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.create(sampleInvitation);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getTargetPersonId()).isEqualTo(200L);
        assertThat(result.getMode()).isEqualTo("ONE_TO_ONE");
        verify(bidInvitationMapper).insert(any(BidInvitation.class));
    }

    @Test
    @DisplayName("AC-TEAM-03 创建招标单：PUBLIC 模式 targetPersonId 为空")
    void createInvitation_public_noTarget() {
        sampleInvitation.setMode("PUBLIC");
        sampleInvitation.setTargetPersonId(null);
        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.create(sampleInvitation);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getMode()).isEqualTo("PUBLIC");
        assertThat(result.getTargetPersonId()).isNull();
    }

    @Test
    @DisplayName("AC-TEAM-05 遴选应标：状态 OPEN→SELECTED，selectedResponseId 写入（P2-3.2 原子提交适配：+operatorId，无落选者）")
    void selectResponse_updatesStatusToSelected() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(sampleInvitation);
        BidResponse resp = BidResponse.builder()
            .id(2001L).invitationId(1001L).rdPmId(200L).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(resp);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of());
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.selectResponse(1001L, 2001L, 300L);

        assertThat(result.getStatus()).isEqualTo("SELECTED");
        assertThat(result.getSelectedResponseId()).isEqualTo(2001L);
        assertThat(resp.getStatus()).isEqualTo("ACCEPTED");
        verify(bidInvitationMapper).updateById(any(BidInvitation.class));
        verify(bidResponseMapper).updateById(any(BidResponse.class));
    }

    @Test
    @DisplayName("AC-TEAM-05 遴选应标：应标不属于该招标单 ⇒ 拒绝")
    void selectResponse_wrongInvitation_rejected() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(sampleInvitation);
        BidResponse resp = BidResponse.builder()
            .id(2001L).invitationId(9999L).rdPmId(200L).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(resp);

        assertThatThrownBy(() -> bidInvitationService.selectResponse(1001L, 2001L, 300L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不属于该招标单");
    }

    @Test
    @DisplayName("AC-TEAM-08 过期扫描：单 SQL 条件 UPDATE（PERF-P0-2：消除 N+1 selectCount 与逐行 updateById）")
    void expireOverdue_noResponse_statusExpired() {
        when(bidInvitationMapper.update(any(), any())).thenReturn(1);

        int count = bidInvitationService.expireOverdue();

        assertThat(count).isEqualTo(1);
        verify(bidInvitationMapper).update(any(), any());
        // N+1 消除证据：过期扫描不再触碰应标表，也不再逐行 selectList/updateById
        verifyNoInteractions(bidResponseMapper);
        verify(bidInvitationMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("AC-TEAM-13 撤回招标单：24h 内可撤回 ⇒ 状态 CLOSED")
    void withdraw_within24h_success() {
        sampleInvitation.setCreateTime(new Date(System.currentTimeMillis() - 3600 * 1000L)); // 1h ago
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(sampleInvitation);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.withdraw(1001L);

        assertThat(result.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("AC-TEAM-13 撤回招标单：超 24h ⇒ 拒绝")
    void withdraw_after24h_rejected() {
        sampleInvitation.setCreateTime(new Date(System.currentTimeMillis() - 25 * 3600 * 1000L)); // 25h ago
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(sampleInvitation);

        assertThatThrownBy(() -> bidInvitationService.withdraw(1001L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("超过24小时不可撤回");
    }

    @Test
    @DisplayName("状态机守护：非 OPEN 状态不可遴选")
    void selectResponse_notOpen_rejected() {
        sampleInvitation.setStatus("CLOSED");
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(sampleInvitation);

        assertThatThrownBy(() -> bidInvitationService.selectResponse(1001L, 2001L, 300L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("状态非 OPEN");
    }

    @Test
    @DisplayName("招标单不存在 ⇒ IllegalArgumentException")
    void getById_notFound_throws() {
        when(bidInvitationMapper.selectById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> bidInvitationService.getById(9999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("招标单不存在");
    }

    @Test
    @DisplayName("应标列表查询：发起人视角按 invitationId 全量过滤（P2-3.2 隐私适配）")
    void listResponses_filtersByInvitationId() {
        sampleInvitation.setCreateBy(300L);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(sampleInvitation);
        BidResponse r1 = BidResponse.builder().id(2001L).invitationId(1001L).status("PENDING").build();
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(r1));

        List<BidResponse> result = bidInvitationService.listResponses(1001L, 300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitationId()).isEqualTo(1001L);
    }
}
