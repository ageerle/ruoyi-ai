package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * P2-3.2 应标/遴选验收测试（AC-TEAM-03/05，BR-TEAM-03、BR-REC-BID-03）
 *
 * <p>形态为 Mockito 单元验收；真库 HTTP 验收另见 docs/ipd-系统说明/验收/ 证据。
 * <p>不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P232AcceptanceTest {

    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BidResponseService bidResponseService;
    @InjectMocks private BidInvitationService bidInvitationService;

    private static final Long MARKET_PM = 300L;
    private static final Long RD_PM_A = 200L;
    private static final Long RD_PM_B = 201L;
    private static final Long RD_PM_C = 202L;

    /** W5-E-2.4：submit/withdraw 已升级 (IpdActor, ...) 签名，按人配 actor 投影 */
    private static final IpdActor ACTOR_RD_PM_A = new IpdActor(RD_PM_A, "研发PM甲", "RD_PM", 1L);
    private static final IpdActor ACTOR_RD_PM_B = new IpdActor(RD_PM_B, "研发PM乙", "RD_PM", 1L);
    private static final IpdActor ACTOR_RD_PM_C = new IpdActor(RD_PM_C, "研发PM丙", "RD_PM", 1L);

    private BidInvitation oneToOneInvitation;
    private BidInvitation publicInvitation;

    /** 构造 ≥40 字的应标方案摘要（spec 页21 solution_summary 下限映射） */
    private static String summary(String tail) {
        return "技术方案摘要：架构选型与里程碑拆解，含风险对策与资源投入说明，覆盖验收标准。" + tail;
    }

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper.set 构造期解析列缓存，纯 Mockito 环境需先注册 TableInfo（仓内 P064/P171 同惯例）
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p232-test-bid"),
            BidResponse.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p232-test-inv"),
            BidInvitation.class);
        oneToOneInvitation = BidInvitation.builder()
            .id(1001L).projectId(100L).mode("ONE_TO_ONE").targetPersonId(RD_PM_A)
            .title("研发PM招标-项目A").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .build();
        oneToOneInvitation.setCreateBy(MARKET_PM);
        publicInvitation = BidInvitation.builder()
            .id(1002L).projectId(100L).mode("PUBLIC").targetPersonId(null)
            .title("研发PM公开招标-项目B").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
            .build();
        publicInvitation.setCreateBy(MARKET_PM);
    }

    private BidResponse acceptPayload(Long invitationId) {
        return BidResponse.builder()
            .invitationId(invitationId).decision("accept")
            .responseNote(summary("A方案")).build();
    }

    // ==================== AC-TEAM-03：拒绝不留痕 ====================

    @Test
    @DisplayName("AC-TEAM-03 拒绝应标：返回 null，不写业务表、不写审计、不通知（BR-TEAM-03）")
    void submit_reject_noTrace() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        BidResponse payload = BidResponse.builder()
            .invitationId(1001L).decision("reject").build();

        BidResponse result = bidResponseService.submit(ACTOR_RD_PM_A, payload);

        assertThat(result).isNull();
        verifyNoInteractions(bidResponseMapper);
        verifyNoInteractions(auditLogService);
    }

    // ==================== 应标提交：名单/状态机/字段校验 ====================

    @Test
    @DisplayName("ONE_TO_ONE 非目标研发PM 应标 ⇒ 30001 FORBIDDEN（不在邀请名单）")
    void submit_oneToOne_outsider_forbidden() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_B, acceptPayload(1001L)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("招标单不存在 ⇒ 50001 NOT_FOUND")
    void submit_invitationMissing_notFound() {
        when(bidInvitationMapper.selectByIdForUpdate(9999L)).thenReturn(null);

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_A, acceptPayload(9999L)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("招标单非 OPEN ⇒ 50002 STATE_CONFLICT（spec 40001 已被 GATE 语义占用，映射现役码）")
    void submit_invitationNotOpen_conflict() {
        oneToOneInvitation.setStatus("SELECTED");
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_A, acceptPayload(1001L)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("方案摘要不足 40 字 ⇒ 10001 PARAM_INVALID（spec 页21 solution_summary ≥40 字）")
    void submit_summaryTooShort_paramInvalid() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        BidResponse payload = BidResponse.builder()
            .invitationId(1001L).decision("accept").responseNote("太短的摘要").build();

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_A, payload))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    // ==================== BR-REC-BID-03：幂等 ====================

    @Test
    @DisplayName("首次 accept：落库 PENDING，rdPmId 取会话用户（服务端权威），写审计 accept")
    void submit_firstAccept_insertsPendingAndAudits() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        when(bidResponseMapper.selectOne(any())).thenReturn(null);
        when(bidResponseMapper.insert(any(BidResponse.class))).thenReturn(1);

        BidResponse result = bidResponseService.submit(ACTOR_RD_PM_A, acceptPayload(1001L));

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRdPmId()).isEqualTo(RD_PM_A);
        verify(bidResponseMapper).insert(any(BidResponse.class));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("accept");
        assertThat(captor.getValue().getEntityType()).isEqualTo("bid_response");
        assertThat(captor.getValue().getOperatorId()).isEqualTo(RD_PM_A);
    }

    @Test
    @DisplayName("幂等重提交（已有 PENDING）：覆盖更新不产生第二行，仍写审计")
    void submit_duplicatePending_updatesSameRow() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        BidResponse existing = BidResponse.builder()
            .id(2001L).invitationId(1001L).rdPmId(RD_PM_A).status("PENDING")
            .responseNote(summary("旧方案")).build();
        when(bidResponseMapper.selectOne(any())).thenReturn(existing);
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);

        BidResponse result = bidResponseService.submit(ACTOR_RD_PM_A, acceptPayload(1001L));

        assertThat(result.getId()).isEqualTo(2001L);
        assertThat(result.getResponseNote()).contains("A方案");
        verify(bidResponseMapper, never()).insert(any(BidResponse.class));
        verify(bidResponseMapper).updateById(existing);
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("已中标后再次应标 ⇒ 50002 STATE_CONFLICT（遴选已定不可再应标）")
    void submit_afterSelected_conflict() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        BidResponse accepted = BidResponse.builder()
            .id(2001L).invitationId(1001L).rdPmId(RD_PM_A).status("ACCEPTED").build();
        when(bidResponseMapper.selectOne(any())).thenReturn(accepted);

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_A, acceptPayload(1001L)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(bidResponseMapper, never()).insert(any(BidResponse.class));
        verify(bidResponseMapper, never()).updateById(any(BidResponse.class));
    }

    @Test
    @DisplayName("PUBLIC 招标：任何内部研发PM 可应标（无名单限制）")
    void submit_public_anyInternalRdPm() {
        when(bidInvitationMapper.selectByIdForUpdate(1002L)).thenReturn(publicInvitation);
        when(bidResponseMapper.selectOne(any())).thenReturn(null);
        when(bidResponseMapper.insert(any(BidResponse.class))).thenReturn(1);

        BidResponse result = bidResponseService.submit(ACTOR_RD_PM_C, acceptPayload(1002L));

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRdPmId()).isEqualTo(RD_PM_C);
    }

    // ==================== AC-TEAM-05：遴选原子提交与落选 ====================

    @Test
    @DisplayName("AC-TEAM-05 三人应标选一人：中标 ACCEPTED、落选两人单 SQL 批量 REJECTED、招标单 SELECTED、审计含落选清单")
    void selectResponse_threeCandidates_atomicSelectAndReject() {
        when(bidInvitationMapper.selectByIdForUpdate(1002L)).thenReturn(publicInvitation);
        BidResponse winner = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(RD_PM_A).status("PENDING").build();
        BidResponse loser1 = BidResponse.builder()
            .id(2002L).invitationId(1002L).rdPmId(RD_PM_B).status("PENDING").build();
        BidResponse loser2 = BidResponse.builder()
            .id(2003L).invitationId(1002L).rdPmId(RD_PM_C).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(winner);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loser1, loser2));
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);
        when(bidResponseMapper.update(any(), any())).thenReturn(2);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = bidInvitationService.selectResponse(1002L, 2001L, MARKET_PM);

        assertThat(result.getStatus()).isEqualTo("SELECTED");
        assertThat(result.getSelectedResponseId()).isEqualTo(2001L);
        assertThat(winner.getStatus()).isEqualTo("ACCEPTED");
        // 落选批量：单 SQL 条件更新（写放大消除），逐行 updateById 仅用于中标行
        verify(bidResponseMapper, times(1)).updateById(any(BidResponse.class));
        // M-2 防假绿：锁定批量 UPDATE 的 SET/WHERE 契约（ne(id)+eq(status=PENDING)+eq(invitationId)，漏 ne 会把中标行改回 REJECTED）
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<BidResponse>> uw = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(bidResponseMapper, times(1)).update(any(), uw.capture());
        LambdaUpdateWrapper<BidResponse> loserWrapper = uw.getValue();
        assertThat(loserWrapper.getSqlSet()).contains("status");
        assertThat(loserWrapper.getSqlSegment()).contains("<>").contains("status");
        assertThat(loserWrapper.getParamNameValuePairs().values())
            .contains("REJECTED").contains("PENDING").contains(2001L).contains(1002L);
        // 审计留痕：中标者与落选者清单进入 afterData（落选通知可查的承载）
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("select");
        assertThat(audit.getEntityType()).isEqualTo("bid_invitation");
        assertThat(audit.getOperatorId()).isEqualTo(MARKET_PM);
        assertThat(audit.getAfterData())
            .contains("\"selectedRdPmId\":200")
            .contains("201").contains("202");
    }

    @Test
    @DisplayName("遴选：中标行缺少研发PM身份（rd_pm_id null）⇒ 拒绝（无法原子绑定）")
    void selectResponse_winnerMissingRdPm_rejected() {
        when(bidInvitationMapper.selectByIdForUpdate(1002L)).thenReturn(publicInvitation);
        BidResponse anonymous = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(null).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(anonymous);

        assertThatThrownBy(() -> bidInvitationService.selectResponse(1002L, 2001L, MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("研发PM身份");
    }

    // ==================== 撤回与隐私 ====================

    @Test
    @DisplayName("decision 非法值（拼错/未知枚举）⇒ 10001 PARAM_INVALID，不得静默按 accept 落库留痕（L-2）")
    void submit_illegalDecision_paramInvalid() {
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(oneToOneInvitation);
        BidResponse payload = BidResponse.builder()
            .invitationId(1001L).decision("rejct").responseNote(summary("A方案")).build();

        assertThatThrownBy(() -> bidResponseService.submit(ACTOR_RD_PM_A, payload))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verifyNoInteractions(bidResponseMapper);
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("撤回应标：非本人 ⇒ 30001 FORBIDDEN（横向越权修复）")
    void withdraw_notOwner_forbidden() {
        BidResponse resp = BidResponse.builder()
            .id(2001L).invitationId(1001L).rdPmId(RD_PM_A).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(resp);

        assertThatThrownBy(() -> bidResponseService.withdraw(ACTOR_RD_PM_B, 2001L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("撤回应标：本人 PENDING ⇒ WITHDRAWN")
    void withdraw_ownerPending_success() {
        BidResponse resp = BidResponse.builder()
            .id(2001L).invitationId(1001L).rdPmId(RD_PM_A).status("PENDING").build();
        when(bidResponseMapper.selectById(2001L)).thenReturn(resp);
        when(bidResponseMapper.updateById(any(BidResponse.class))).thenReturn(1);

        BidResponse result = bidResponseService.withdraw(ACTOR_RD_PM_A, 2001L);

        assertThat(result.getStatus()).isEqualTo("WITHDRAWN");
    }

    @Test
    @DisplayName("隐私：非发起人查询应标列表仅见本人行（M-2 防假绿：wrapper 断言锁定 rd_pm_id 过滤条件）")
    void listResponses_nonOwner_seesOnlyOwnRows() {
        when(bidInvitationMapper.selectById(1002L)).thenReturn(publicInvitation);
        BidResponse own = BidResponse.builder()
            .id(2003L).invitationId(1002L).rdPmId(RD_PM_C).status("PENDING").build();
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(own));

        List<BidResponse> result = bidInvitationService.listResponses(1002L, RD_PM_C);

        assertThat(result).allSatisfy(r -> assertThat(r.getRdPmId()).isEqualTo(RD_PM_C));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<BidResponse>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bidResponseMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("rd_pm_id");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(RD_PM_C);
    }

    @Test
    @DisplayName("隐私：发起人（createBy）查询应标列表可见全量")
    void listResponses_owner_seesAll() {
        when(bidInvitationMapper.selectById(1002L)).thenReturn(publicInvitation);
        BidResponse r1 = BidResponse.builder()
            .id(2001L).invitationId(1002L).rdPmId(RD_PM_A).status("PENDING").build();
        BidResponse r2 = BidResponse.builder()
            .id(2002L).invitationId(1002L).rdPmId(RD_PM_B).status("PENDING").build();
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(r1, r2));

        List<BidResponse> result = bidInvitationService.listResponses(1002L, MARKET_PM);

        assertThat(result).hasSize(2);
    }
}
