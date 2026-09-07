package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * W5-E-2.4 P0 #5 IDOR 修复验收：BidResponseService 3 公开方法 actor 化。
 *
 * <p>核心：listByRdPm(IpdActor, Long) 三分支放行——本人 / SUPER_ADMIN / 关联项目在职
 * ProjectMember；其余 FORBIDDEN。submit/withdraw 以 actor.id() 为服务端权威身份并做
 * UNAUTHORIZED 入口兜底（service 层不信任 controller 必传）。
 *
 * <p>业务逻辑回归（状态机/幂等/审计/摘要校验）另见 P232AcceptanceTest（已同步升级 3-arg）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidResponseServiceTest {

    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BidResponseService service;

    private static final Long RD_PM_A = 200L;   // 应标人（被查询对象）
    private static final Long MARKET_PM = 300L; // 关联项目市场PM（豁免路径）
    private static final Long OUTSIDER = 999L;  // 无关第三方（FORBIDDEN 路径）

    /** actor 模板（IpdActor record：id, name, role, groupId） */
    private static final IpdActor SELF_ACTOR = new IpdActor(RD_PM_A, "研发PM甲", "RD_PM", 1L);
    private static final IpdActor MARKET_PM_ACTOR = new IpdActor(MARKET_PM, "市场PM", "MARKET_PM", 1L);
    private static final IpdActor ADMIN_ACTOR = new IpdActor(1L, "超管", "SUPER_ADMIN", null);
    private static final IpdActor OUTSIDER_ACTOR = new IpdActor(OUTSIDER, "路人", "RD_PM", 2L);

    /** 构造 ≥40 字的应标方案摘要（spec 页21 solution_summary 下限映射） */
    private static String summary(String tail) {
        return "技术方案摘要：架构选型与里程碑拆解，含风险对策与资源投入说明，覆盖验收标准。" + tail;
    }

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 构造期解析列缓存，纯 Mockito 环境需先注册 TableInfo（仓内 P232/P171 同惯例）
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-w5e24-test-bid"),
            BidResponse.class);
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-w5e24-test-pm"),
            ProjectMember.class);
    }

    private BidResponse row(Long id, Long invitationId, Long rdPmId) {
        return BidResponse.builder().id(id).invitationId(invitationId).rdPmId(rdPmId)
            .status("PENDING").responseNote(summary("A方案")).build();
    }

    private BidInvitation invitation(Long id, Long projectId) {
        return BidInvitation.builder().id(id).projectId(projectId).mode("PUBLIC")
            .title("公开招标").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)).build();
    }

    // ==================== 件 3.1.1 actor 缺失 → UNAUTHORIZED ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-5] listByRdPm actor == null → UNAUTHORIZED（防御性兜底，controller 已守门）")
    void listByRdPm_nullActor_unauthorized() {
        assertThatThrownBy(() -> service.listByRdPm(null, RD_PM_A))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(bidResponseMapper, bidInvitationMapper, projectMemberMapper);
    }

    @Test
    @DisplayName("[W5-E-2.4-IDOR-6] listByRdPm actor.id == null → UNAUTHORIZED")
    void listByRdPm_nullActorId_unauthorized() {
        IpdActor ghost = new IpdActor(null, "幽灵", "RD_PM", 1L);
        assertThatThrownBy(() -> service.listByRdPm(ghost, RD_PM_A))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录");
    }

    // ==================== 件 3.1.2 本人查询（正常） ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-1] 本人查自己的应标 → 返回行，不触发成员关系探测（三分支短路）")
    void listByRdPm_self_returnsRows_withoutMembershipProbe() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));

        List<BidResponse> result = service.listByRdPm(SELF_ACTOR, RD_PM_A);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRdPmId()).isEqualTo(RD_PM_A);
        // 本人分支短路：不得发起招标单/成员关系探测查询
        verifyNoInteractions(bidInvitationMapper, projectMemberMapper);
    }

    // ==================== 件 3.1.3 查他人 → FORBIDDEN ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-2] 第三方查他人应标（非关联项目成员）→ FORBIDDEN（P0 #5 核心）")
    void listByRdPm_otherNonMember_forbidden() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidInvitationMapper.selectBatchIds(any())).thenReturn(List.of(invitation(1001L, 100L)));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.listByRdPm(OUTSIDER_ACTOR, RD_PM_A))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权查看他人应标")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("[W5-E-2.4-IDOR-8] 第三方查他人应标且目标无任何应标行 → 仍 FORBIDDEN（fail-closed，不泄露有无应标 oracle）")
    void listByRdPm_emptyRows_thirdParty_failClosed() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.listByRdPm(OUTSIDER_ACTOR, RD_PM_A))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        // 空行短路：不发起招标单/成员关系探测
        verify(bidInvitationMapper, never()).selectBatchIds(any());
        verify(projectMemberMapper, never()).selectCount(any());
    }

    // ==================== 件 3.1.4 SUPER_ADMIN 豁免 ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-3] SUPER_ADMIN 查他人应标 → 豁免返回（三分支短路，不触成员探测）")
    void listByRdPm_superAdmin_exempted() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));

        List<BidResponse> result = service.listByRdPm(ADMIN_ACTOR, RD_PM_A);

        assertThat(result).hasSize(1);
        verifyNoInteractions(bidInvitationMapper, projectMemberMapper);
    }

    // ==================== 件 3.1.5 ProjectMember 豁免 ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-4] 关联项目在职成员查他人应标 → 豁免返回（M-2：锁定 in(project_id)+eq(person_id)+exit_date IS NULL 口径）")
    void listByRdPm_relatedProjectMember_exempted() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidInvitationMapper.selectBatchIds(any())).thenReturn(List.of(invitation(1001L, 100L)));
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);

        List<BidResponse> result = service.listByRdPm(MARKET_PM_ACTOR, RD_PM_A);

        assertThat(result).hasSize(1);
        // M-2 防假绿：成员豁免必须锚定 应标→招标单→项目→project_member 在职行 链路（排除已退出）
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectMemberMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
            .contains("project_id").contains("person_id").contains("exit_date");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(MARKET_PM);
    }

    // ==================== 件 1.5 参数校验 ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-7] listByRdPm rdPmId == null → PARAM_INVALID")
    void listByRdPm_nullRdPmId_paramInvalid() {
        assertThatThrownBy(() -> service.listByRdPm(SELF_ACTOR, null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("rdPmId 不能为空")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    // ==================== 件 1.5 submit/withdraw actor 化跟随 ====================

    @Test
    @DisplayName("[W5-E-2.4-IDOR-9] submit actor == null → UNAUTHORIZED，零 mapper 交互")
    void submit_nullActor_unauthorized() {
        assertThatThrownBy(() -> service.submit(null, row(2001L, 1001L, RD_PM_A)))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(bidInvitationMapper, bidResponseMapper, auditLogService);
    }

    @Test
    @DisplayName("[W5-E-2.4-IDOR-10] submit 合法 actor → rdPmId 取 actor.id()（服务端权威保持，不信任请求体）")
    void submit_actorIdIsServerAuthoritative() {
        BidInvitation pub = invitation(1001L, 100L);
        when(bidInvitationMapper.selectByIdForUpdate(1001L)).thenReturn(pub);
        when(bidResponseMapper.selectOne(any())).thenReturn(null);
        when(bidResponseMapper.insert(any(BidResponse.class))).thenReturn(1);

        // 请求体伪造 rdPmId=OUTSIDER，应被 actor.id() 覆盖
        BidResponse payload = row(null, 1001L, OUTSIDER);
        BidResponse result = service.submit(SELF_ACTOR, payload);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRdPmId()).isEqualTo(RD_PM_A);
    }

    @Test
    @DisplayName("[W5-E-2.4-IDOR-11] withdraw actor == null → UNAUTHORIZED")
    void withdraw_nullActor_unauthorized() {
        assertThatThrownBy(() -> service.withdraw(null, 2001L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(bidResponseMapper);
    }

    @Test
    @DisplayName("[W5-E-2.4-IDOR-12] withdraw 非本人应标 → FORBIDDEN（actor 化后归属校验保持）")
    void withdraw_nonOwner_forbidden() {
        when(bidResponseMapper.selectById(2001L)).thenReturn(row(2001L, 1001L, RD_PM_A));

        assertThatThrownBy(() -> service.withdraw(OUTSIDER_ACTOR, 2001L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }
}
