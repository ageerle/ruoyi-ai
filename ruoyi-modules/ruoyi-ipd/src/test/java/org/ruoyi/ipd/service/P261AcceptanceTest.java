package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-6.1 需求变更申请与影响评估快照验收测试
 * AC: AC-REQ-08（需求转需求变更单 ⇒ 可生成，走双签否决）
 * BR: BR-GATE-07
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P261AcceptanceTest {

    @Mock private RequirementChangeMapper requirementChangeMapper;
    @Mock private RequirementMapper requirementMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private RequirementChangeService requirementChangeService;

    private static final Long MARKET_PM = 300L;
    private static final Long RD_PM = 200L;
    private static final Long ADMIN = 999L;
    private static final Long CREATOR = 350L;
    private static final Long PROJECT_ID = 100L;
    private static final Long REQUIREMENT_ID = 500L;

    private static final IpdActor MARKET = new IpdActor(MARKET_PM, "市场经理", "MARKET_PM", 10L);
    private static final IpdActor RD = new IpdActor(RD_PM, "研发经理", "RD_PM", 10L);
    private static final IpdActor CREATOR_ACTOR = new IpdActor(CREATOR, "提交人", "MARKET_PM", 10L);

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p261-rc"),
            RequirementChange.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p261-req"),
            Requirement.class);
    }

    private Requirement requirement() {
        return Requirement.builder()
            .id(REQUIREMENT_ID)
            .projectId(PROJECT_ID)
            .title("测试需求")
            .status("SUBMITTED")
            .build();
    }

    private RequirementChange draftChange() {
        return RequirementChange.builder()
            .requirementId(REQUIREMENT_ID)
            .projectId(PROJECT_ID)
            .changeType("SCOPE_EXPAND")
            .reason("范围扩大")
            // P2-6.2 强化（AC-REQ-08）：快照必须显式覆盖四维度，键为中文「范围/成本/时限/质量」
            .beforeSnapshot("{\"范围\":\"原范围\",\"成本\":100,\"时限\":\"30d\",\"质量\":\"P1\"}")
            .afterSnapshot("{\"范围\":\"新范围\",\"成本\":150,\"时限\":\"45d\",\"质量\":\"P1\"}")
            .build();
    }

    private RequirementChange existingDraft() {
        RequirementChange rc = draftChange();
        rc.setId(11L);
        rc.setStatus(RequirementChangeService.STATUS_DRAFT);
        rc.setCreateBy(CREATOR);
        return rc;
    }

    @Test
    @DisplayName("createDraft_正常创建变更单_状态DRAFT_写审计")
    void createDraft_normal() {
        when(requirementMapper.selectById(REQUIREMENT_ID)).thenReturn(requirement());
        when(requirementChangeMapper.insert(any(RequirementChange.class))).thenAnswer(inv -> {
            RequirementChange arg = inv.getArgument(0);
            arg.setId(1L);
            return 1;
        });

        RequirementChange out = requirementChangeService.create(draftChange(), CREATOR_ACTOR);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_DRAFT);
        assertThat(out.getCreateBy()).isEqualTo(CREATOR);
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("REQ_CHANGE_CREATE");
    }

    @Test
    @DisplayName("createDraft_引用不存在需求_拒绝")
    void createDraft_missingRequirement() {
        when(requirementMapper.selectById(REQUIREMENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> requirementChangeService.create(draftChange(), CREATOR_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(requirementChangeMapper, never()).insert(any(RequirementChange.class));
    }

    @Test
    @DisplayName("create_changeType为空_PARAM_INVALID")
    void create_missingChangeType() {
        RequirementChange change = draftChange();
        change.setChangeType(null);

        assertThatThrownBy(() -> requirementChangeService.create(change, CREATOR_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("create_reason为空_PARAM_INVALID")
    void create_missingReason() {
        RequirementChange change = draftChange();
        change.setReason("");

        assertThatThrownBy(() -> requirementChangeService.create(change, CREATOR_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("create_影响快照在创建时冻结")
    void create_snapshotFrozen() {
        when(requirementMapper.selectById(REQUIREMENT_ID)).thenReturn(requirement());
        when(requirementChangeMapper.insert(any(RequirementChange.class))).thenAnswer(inv -> {
            RequirementChange arg = inv.getArgument(0);
            arg.setId(40L);
            return 1;
        });

        RequirementChange input = draftChange();
        String beforeJson = input.getBeforeSnapshot();
        RequirementChange out = requirementChangeService.create(input, CREATOR_ACTOR);

        assertThat(out.getBeforeSnapshot()).isEqualTo(beforeJson);
        assertThat(out.getAfterSnapshot()).contains("新范围");
    }

    @Test
    @DisplayName("submit_影响快照缺失_拒绝")
    void submit_missingSnapshot() {
        RequirementChange existing = RequirementChange.builder()
            .id(11L).requirementId(REQUIREMENT_ID).projectId(PROJECT_ID)
            .changeType("SCOPE_EXPAND").reason("x")
            .beforeSnapshot(null).afterSnapshot("{\"scope\":\"new\"}")
            .status(RequirementChangeService.STATUS_DRAFT).build();
        existing.setCreateBy(CREATOR);
        when(requirementChangeMapper.selectById(11L)).thenReturn(existing);

        assertThatThrownBy(() -> requirementChangeService.submit(11L, CREATOR_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(requirementChangeMapper, never()).updateById(any(RequirementChange.class));
    }

    @Test
    @DisplayName("submitDraft_正常进入双签队列")
    void submitDraft_normal() {
        RequirementChange existing = existingDraft();
        existing.setId(12L);
        when(requirementChangeMapper.selectById(12L)).thenReturn(existing);

        RequirementChange out = requirementChangeService.submit(12L, CREATOR_ACTOR);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_PENDING_SIGN);
        verify(requirementChangeMapper, times(1)).updateById(existing);
    }

    @Test
    @DisplayName("submit_非DRAFT_状态冲突")
    void submit_stateConflict() {
        RequirementChange existing = existingDraft();
        existing.setId(13L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        when(requirementChangeMapper.selectById(13L)).thenReturn(existing);

        assertThatThrownBy(() -> requirementChangeService.submit(13L, CREATOR_ACTOR))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("submit_非创建人_越权拒绝")
    void submit_forbidden() {
        RequirementChange existing = existingDraft();
        existing.setId(14L);
        when(requirementChangeMapper.selectById(14L)).thenReturn(existing);

        IpdActor stranger = new IpdActor(123L, "路人", "MARKET_PM", 99L);
        assertThatThrownBy(() -> requirementChangeService.submit(14L, stranger))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("sign_双APPROVE_整体APPROVED")
    void sign_dualApprove() {
        RequirementChange existing = existingDraft();
        existing.setId(20L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        existing.setSignatures("MARKET_PM:300=APPROVE");
        when(requirementChangeMapper.selectById(20L)).thenReturn(existing);

        RequirementChange out = requirementChangeService.sign(20L, "APPROVE", "ok", RD);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_APPROVED);
        assertThat(out.getSignatures()).contains("RD_PM:200=APPROVE");
    }

    @Test
    @DisplayName("sign_单方APPROVE_保持PENDING_SIGN")
    void sign_partialApprove() {
        RequirementChange existing = existingDraft();
        existing.setId(21L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        existing.setSignatures("");
        when(requirementChangeMapper.selectById(21L)).thenReturn(existing);

        RequirementChange out = requirementChangeService.sign(21L, "APPROVE", "ok", MARKET);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_PENDING_SIGN);
        assertThat(out.getSignatures()).contains("MARKET_PM:300=APPROVE");
        assertThat(out.getSignatures()).doesNotContain("RD_PM:200=APPROVE");
    }

    @Test
    @DisplayName("sign_任一REJECT_整体REJECTED")
    void sign_anyReject() {
        RequirementChange existing = existingDraft();
        existing.setId(22L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        existing.setSignatures("MARKET_PM:300=APPROVE");
        when(requirementChangeMapper.selectById(22L)).thenReturn(existing);

        RequirementChange out = requirementChangeService.sign(22L, "REJECT", "成本过高", RD);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_REJECTED);
        assertThat(out.getSignatures()).contains("RD_PM:200=REJECT");
    }

    @Test
    @DisplayName("sign_非PM角色_FORBIDDEN")
    void sign_nonPmRole() {
        IpdActor stranger = new IpdActor(123L, "组长", "GROUP_LEADER", 10L);
        assertThatThrownBy(() -> requirementChangeService.sign(23L, "APPROVE", "ok", stranger))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("sign_同方重复签_状态冲突")
    void sign_duplicate() {
        RequirementChange existing = existingDraft();
        existing.setId(24L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        existing.setSignatures("MARKET_PM:300=APPROVE");
        when(requirementChangeMapper.selectById(24L)).thenReturn(existing);

        assertThatThrownBy(() -> requirementChangeService.sign(24L, "APPROVE", "ok", MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("sign_非法decision拒绝")
    void sign_invalidDecision() {
        assertThatThrownBy(() -> requirementChangeService.sign(25L, "MAYBE", "x", MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("listOpenByProject_返回DRAFT与PENDING_SIGN")
    void listOpen() {
        when(requirementChangeMapper.selectList(any())).thenReturn(List.of(
            RequirementChange.builder().id(1L).status("DRAFT").build(),
            RequirementChange.builder().id(2L).status("PENDING_SIGN").build()
        ));
        List<RequirementChange> open = requirementChangeService.listOpenByProject(PROJECT_ID);
        assertThat(open).hasSize(2);
    }

    @Test
    @DisplayName("hasOpenChange_有未闭环返回true")
    void hasOpen_true() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(3L);
        assertThat(requirementChangeService.hasOpenChange(PROJECT_ID)).isTrue();
    }

    @Test
    @DisplayName("hasOpenChange_无未闭环返回false")
    void hasOpen_false() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(0L);
        assertThat(requirementChangeService.hasOpenChange(PROJECT_ID)).isFalse();
    }

    @Test
    @DisplayName("detail_返回含快照与签名进度视图")
    void detail_view() {
        RequirementChange existing = existingDraft();
        existing.setId(30L);
        existing.setStatus(RequirementChangeService.STATUS_PENDING_SIGN);
        existing.setSignatures("MARKET_PM:300=APPROVE");
        existing.setCreateTime(new Date(1700000000000L));
        existing.setUpdateTime(new Date(1700000001000L));
        when(requirementChangeMapper.selectById(30L)).thenReturn(existing);

        var view = requirementChangeService.detail(30L);

        assertThat(view).containsEntry("id", 30L);
        assertThat(view).containsEntry("status", RequirementChangeService.STATUS_PENDING_SIGN);
        assertThat(view).containsEntry("signatures", "MARKET_PM:300=APPROVE");
        assertThat(view.get("beforeSnapshot").toString()).contains("原范围");
    }
}
