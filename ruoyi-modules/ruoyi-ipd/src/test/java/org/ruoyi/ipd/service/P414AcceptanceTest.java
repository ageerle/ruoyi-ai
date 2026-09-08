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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P4-1.4 需求七态处理、采纳与变更关联 验收测试 (BR-REQ-05; AC-REQ-05/06/07/08)
 *
 * <p>卡面统一收口 4 项验收 AC：
 * <ol>
 *   <li>AC-REQ-05 七态流转：DRAFT → SUBMITTED → ROUTED → ACCEPTED → CHANGED → CLOSED 严格顺序，越界拒绝</li>
 *   <li>AC-REQ-06 ROUTED → ACCEPTED：双 PM 任一批准即受理；同方拒则 ROUTED → REJECTED</li>
 *   <li>AC-REQ-07 ACCEPTED → CHANGED：自动创建 RequirementChange DRAFT（ADOPTION_MODIFIED）</li>
 *   <li>AC-REQ-08 CHANGED → CLOSED：变更单生效后进入终态 CLOSED</li>
 * </ol>
 *
 * <p>实现见 {@link RequirementStateMachine}（P4-1.4）；鉴权链由 IpdIdorGuard 兜底。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class P414AcceptanceTest {

    @Mock private RequirementMapper requirementMapper;
    @Mock private RequirementChangeMapper requirementChangeMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AuditLogService auditLogService;

    private RequirementStateMachine service;

    private static final Long PROJECT_ID = 99L;
    private static final Long GROUP_ID = 11L;

    /** GROUP_LEADER：可走全部 7 态（除 ROUTED→ACCEPTED/REJECTED 之外的桥接步骤）。 */
    private static final IpdActor LEADER = new IpdActor(10L, "王组长", "GROUP_LEADER", GROUP_ID);
    /** MARKET_PM：仅 ROUTED→ACCEPTED/REJECTED、ACCEPTED→CHANGED/REJECTED 合法。 */
    private static final IpdActor MARKET_PM = new IpdActor(101L, "mkt", "MARKET_PM", GROUP_ID);
    /** RD_PM：同上。 */
    private static final IpdActor RD_PM = new IpdActor(202L, "rd", "RD_PM", GROUP_ID);
    /** SUPER_ADMIN：兜底绕过鉴权。 */
    private static final IpdActor ADMIN = new IpdActor(1L, "超管", "SUPER_ADMIN", null);

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, Requirement.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMemberMapper.class);
    }

    @BeforeEach
    void setUp() {
        service = new RequirementStateMachine(requirementMapper, requirementChangeMapper,
            projectMapper, projectMemberMapper, auditLogService);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        // 默认：项目存在、mainGroupId 匹配、actor 是项目成员（鉴权链兜底）
        Project project = Project.builder().id(PROJECT_ID).mainGroupId(GROUP_ID).tenantId("000000").build();
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
    }

    private Requirement req(Long id, String status) {
        return Requirement.builder()
            .id(id).status(status).projectId(PROJECT_ID)
            .queryCode("AB12CD" + (id % 10)).build();
    }

    private void givenReq(Long id, String status) {
        when(requirementMapper.selectById(id)).thenAnswer(inv -> req(id, status));
    }

    /* ============================================================
     *  AC-REQ-05  七态严格流转
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-05] DRAFT → SUBMITTED（submit）合法")
    void AC_REQ_05_DRAFT_到_SUBMITTED() {
        givenReq(1L, RequirementStateMachine.ST_DRAFT);
        Requirement after = service.transition(1L, RequirementStateMachine.ST_SUBMITTED, "submit", LEADER);
        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_SUBMITTED);
        verify(requirementMapper).updateById(any(Requirement.class));
        verify(requirementChangeMapper, never()).insert(any(RequirementChange.class));
    }

    @Test
    @DisplayName("[AC-REQ-05] SUBMITTED → ROUTED（route，按产品路由双 PM）合法")
    void AC_REQ_05_SUBMITTED_到_ROUTED() {
        givenReq(2L, RequirementStateMachine.ST_SUBMITTED);
        Requirement after = service.transition(2L, RequirementStateMachine.ST_ROUTED, "route to dual PM", LEADER);
        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_ROUTED);
    }

    @Test
    @DisplayName("[AC-REQ-05] ROUTED → ACCEPTED（双 PM 受理）合法")
    void AC_REQ_05_ROUTED_到_ACCEPTED() {
        givenReq(3L, RequirementStateMachine.ST_ROUTED);
        Requirement after = service.transition(3L, RequirementStateMachine.ST_ACCEPTED, "双 PM 受理", MARKET_PM);
        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_ACCEPTED);
    }

    @Test
    @DisplayName("[AC-REQ-06] ROUTED → REJECTED（双 PM 任一拒）合法")
    void AC_REQ_06_ROUTED_到_REJECTED() {
        givenReq(4L, RequirementStateMachine.ST_ROUTED);
        Requirement after = service.transition(4L, RequirementStateMachine.ST_REJECTED, "双 PM 拒", RD_PM);
        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_REJECTED);
    }

    @Test
    @DisplayName("[AC-REQ-05] 越界拒绝：SUBMITTED → ACCEPTED 直接跳状态 ⇒ STATE_CONFLICT")
    void AC_REQ_05_越界拒绝_SUBMITTED_ACCEPTED() {
        givenReq(5L, RequirementStateMachine.ST_SUBMITTED);
        assertThatThrownBy(() -> service.transition(5L, RequirementStateMachine.ST_ACCEPTED, "skip route", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(requirementMapper, never()).updateById(any(Requirement.class));
    }

    @Test
    @DisplayName("[AC-REQ-05] 越界拒绝：DRAFT → ACCEPTED（一步跳到 ACCEPTED）⇒ STATE_CONFLICT")
    void AC_REQ_05_越界拒绝_DRAFT_ACCEPTED() {
        givenReq(6L, RequirementStateMachine.ST_DRAFT);
        assertThatThrownBy(() -> service.transition(6L, RequirementStateMachine.ST_ACCEPTED, "jump", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[AC-REQ-05] 越界拒绝：CHANGED → ACCEPTED（已变更不能回到 ACCEPTED）⇒ STATE_CONFLICT")
    void AC_REQ_05_越界拒绝_CHANGED_ACCEPTED() {
        givenReq(7L, RequirementStateMachine.ST_CHANGED);
        assertThatThrownBy(() -> service.transition(7L, RequirementStateMachine.ST_ACCEPTED, "revert", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[AC-REQ-05] 终态守卫：REJECTED → 任何状态 ⇒ STATE_CONFLICT")
    void AC_REQ_05_终态守卫_REJECTED() {
        givenReq(8L, RequirementStateMachine.ST_REJECTED);
        for (String toState : new String[] {
            RequirementStateMachine.ST_DRAFT, RequirementStateMachine.ST_SUBMITTED,
            RequirementStateMachine.ST_ROUTED, RequirementStateMachine.ST_ACCEPTED,
            RequirementStateMachine.ST_CHANGED, RequirementStateMachine.ST_CLOSED
        }) {
            assertThatThrownBy(() -> service.transition(8L, toState, "from terminal", ADMIN))
                .as("REJECTED → %s 应拒绝", toState)
                .isInstanceOf(IpdBusinessException.class)
                .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        }
    }

    @Test
    @DisplayName("[AC-REQ-05] 终态守卫：CLOSED → 任何状态 ⇒ STATE_CONFLICT")
    void AC_REQ_05_终态守卫_CLOSED() {
        givenReq(9L, RequirementStateMachine.ST_CLOSED);
        for (String toState : new String[] {
            RequirementStateMachine.ST_DRAFT, RequirementStateMachine.ST_SUBMITTED,
            RequirementStateMachine.ST_ROUTED, RequirementStateMachine.ST_ACCEPTED,
            RequirementStateMachine.ST_CHANGED, RequirementStateMachine.ST_REJECTED
        }) {
            assertThatThrownBy(() -> service.transition(9L, toState, "from terminal", ADMIN))
                .as("CLOSED → %s 应拒绝", toState)
                .isInstanceOf(IpdBusinessException.class)
                .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        }
    }

    @Test
    @DisplayName("[AC-REQ-05] 目标状态非法字面量 ⇒ PARAM_INVALID")
    void AC_REQ_05_非法目标状态() {
        givenReq(10L, RequirementStateMachine.ST_DRAFT);
        assertThatThrownBy(() -> service.transition(10L, "INVALID_STATE", "x", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    /* ============================================================
     *  AC-REQ-07  ACCEPTED → CHANGED 自动创建 RequirementChange DRAFT
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-07] ACCEPTED → CHANGED 自动创建 RequirementChange DRAFT（ADOPTION_MODIFIED）")
    void AC_REQ_07_ACCEPTED_到_CHANGED_创建变更单() {
        givenReq(11L, RequirementStateMachine.ST_ACCEPTED);

        Requirement after = service.transition(11L, RequirementStateMachine.ST_CHANGED,
            "采纳后需要调整范围", MARKET_PM);

        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_CHANGED);
        ArgumentCaptor<RequirementChange> cap = ArgumentCaptor.forClass(RequirementChange.class);
        verify(requirementChangeMapper).insert(cap.capture());
        RequirementChange created = cap.getValue();
        assertThat(created.getRequirementId()).isEqualTo(11L);
        assertThat(created.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(created.getChangeType()).isEqualTo("ADOPTION_MODIFIED");
        assertThat(created.getStatus()).isEqualTo(RequirementChangeService.STATUS_DRAFT);
        assertThat(created.getCreateBy()).isEqualTo(MARKET_PM.id());
        assertThat(created.getReason()).contains("采纳后需要调整范围");
        assertThat(created.getBeforeSnapshot()).contains("ACCEPTED");
        assertThat(created.getAfterSnapshot()).contains("CHANGED");
    }

    @Test
    @DisplayName("[AC-REQ-07] ROUTED → CHANGED（绕过 ACCEPTED）⇒ STATE_CONFLICT，不创建变更单")
    void AC_REQ_07_越界到CHANGED不创建变更单() {
        givenReq(12L, RequirementStateMachine.ST_ROUTED);
        assertThatThrownBy(() -> service.transition(12L, RequirementStateMachine.ST_CHANGED,
            "skip accept", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(requirementChangeMapper, never()).insert(any(RequirementChange.class));
    }

    /* ============================================================
     *  AC-REQ-08  CHANGED → CLOSED
     * ========================================================= === */

    @Test
    @DisplayName("[AC-REQ-08] CHANGED → CLOSED（变更单生效后进入终态）合法")
    void AC_REQ_08_CHANGED_到_CLOSED() {
        givenReq(13L, RequirementStateMachine.ST_CHANGED);
        Requirement after = service.transition(13L, RequirementStateMachine.ST_CLOSED, "变更完成", LEADER);
        assertThat(after.getStatus()).isEqualTo(RequirementStateMachine.ST_CLOSED);
        verify(requirementMapper).updateById(any(Requirement.class));
        // CHANGED → CLOSED 不再创建新变更单（仅 ACCEPTED → CHANGED 自动创建）
        verify(requirementChangeMapper, never()).insert(any(RequirementChange.class));
    }

    @Test
    @DisplayName("[AC-REQ-08] ACCEPTED → CLOSED（绕过 CHANGED）⇒ FORBIDDEN（角色门先于状态机守卫）")
    void AC_REQ_08_越界_ACCEPTED_CLOSED() {
        givenReq(14L, RequirementStateMachine.ST_ACCEPTED);
        // ACCEPTED → CLOSED 无 TRANSITION_ROLES 映射，角色门先于状态机守卫报 FORBIDDEN
        assertThatThrownBy(() -> service.transition(14L, RequirementStateMachine.ST_CLOSED,
            "skip change", MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        // 用 SUPER_ADMIN 绕过角色门后，仍被状态机守卫拒 ⇒ STATE_CONFLICT
        assertThatThrownBy(() -> service.transition(14L, RequirementStateMachine.ST_CLOSED,
            "skip change", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    /* ============================================================
     *  isAllowed() 单元测试（业务契约锁定）
     * ========================================================= === */

    @Test
    @DisplayName("isAllowed 锁定合法迁移表：7 状态全集 + 越界全 false")
    void isAllowed契约锁定() {
        for (String s : RequirementStateMachine.ALL_STATES) {
            assertThat(service.isAllowed(s, s)).as("self-loop %s→%s 必须 false", s, s).isFalse();
        }
        for (String from : RequirementStateMachine.TERMINAL_STATES) {
            for (String to : RequirementStateMachine.ALL_STATES) {
                assertThat(service.isAllowed(from, to))
                    .as("终态 %s → %s 必须 false", from, to)
                    .isFalse();
            }
        }
        assertThat(service.isAllowed(RequirementStateMachine.ST_DRAFT, RequirementStateMachine.ST_SUBMITTED)).isTrue();
        assertThat(service.isAllowed(RequirementStateMachine.ST_SUBMITTED, RequirementStateMachine.ST_ROUTED)).isTrue();
        assertThat(service.isAllowed(RequirementStateMachine.ST_ROUTED, RequirementStateMachine.ST_ACCEPTED)).isTrue();
        assertThat(service.isAllowed(RequirementStateMachine.ST_ACCEPTED, RequirementStateMachine.ST_CHANGED)).isTrue();
        assertThat(service.isAllowed(RequirementStateMachine.ST_CHANGED, RequirementStateMachine.ST_CLOSED)).isTrue();
    }

    @Test
    @DisplayName("ALL_STATES 词表：恰好 7 状态（DRAFT/SUBMITTED/ROUTED/ACCEPTED/REJECTED/CHANGED/CLOSED）")
    void ALL_STATES词表7状态() {
        assertThat(RequirementStateMachine.ALL_STATES).hasSize(7);
        assertThat(RequirementStateMachine.ALL_STATES).containsExactlyInAnyOrder(
            RequirementStateMachine.ST_DRAFT,
            RequirementStateMachine.ST_SUBMITTED,
            RequirementStateMachine.ST_ROUTED,
            RequirementStateMachine.ST_ACCEPTED,
            RequirementStateMachine.ST_REJECTED,
            RequirementStateMachine.ST_CHANGED,
            RequirementStateMachine.ST_CLOSED);
    }

    /* ============================================================
     *  防御性输入校验
     * ========================================================= === */

    @Test
    @DisplayName("需求 ID 为 null ⇒ PARAM_INVALID")
    void 需求ID空() {
        assertThatThrownBy(() -> service.transition(null, RequirementStateMachine.ST_SUBMITTED, "x", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("需求不存在 ⇒ NOT_FOUND")
    void 需求不存在() {
        when(requirementMapper.selectById(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.transition(404L, RequirementStateMachine.ST_SUBMITTED, "x", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("操作人 actor 为 null ⇒ UNAUTHORIZED")
    void 操作人空() {
        givenReq(15L, RequirementStateMachine.ST_DRAFT);
        assertThatThrownBy(() -> service.transition(15L, RequirementStateMachine.ST_SUBMITTED, "x", null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    /* ============================================================
     *  审计：每次迁移落 1 条 REQUIREMENT_TRANSITION 审计
     * ========================================================= === */

    @Test
    @DisplayName("合法迁移：写 1 条 REQUIREMENT_TRANSITION 审计 + entityType=requirement_v2")
    void 合法迁移审计() {
        givenReq(16L, RequirementStateMachine.ST_SUBMITTED);
        service.transition(16L, RequirementStateMachine.ST_ROUTED, "audit reason", LEADER);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        AuditLog audit = cap.getValue();
        assertThat(audit.getAction()).isEqualTo("REQUIREMENT_TRANSITION");
        assertThat(audit.getEntityType()).isEqualTo("requirement_v2");
        assertThat(audit.getEntityId()).isEqualTo(16L);
        assertThat(audit.getOperatorRole()).isEqualTo("GROUP_LEADER");
        assertThat(audit.getReason()).contains("from=SUBMITTED").contains("to=ROUTED");
    }
}