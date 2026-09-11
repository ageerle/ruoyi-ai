package org.ruoyi.ipd.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * W5-E-Guard：IpdIdorGuard 包级静态 IDOR 守卫单测（@Tag("dev") 必须）。
 *
 * <p>覆盖五大守卫全部分支（每守卫 ≥2 测）+ W4-Security 租户容忍口径直测：
 * requireAuthenticated（3）/ requireSelfOrSuperAdmin（5）/ requireProjectMemberOrSuperAdmin（8，
 * 含 wrapper 列名口径锁定与 fail-closed）/ requireRoleOrSuperAdmin（4）/ requireSuperAdmin（3）/
 * requireTenantMatch + currentTenantId（3）。
 *
 * <p>纯 JVM 单测无 MP 运行时，@BeforeAll 手动初始化 ProjectMember lambda 列缓存
 * （LambdaQueryWrapper.eq/.isNull 需列名解析，DeletionRequestServiceTest 同款）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("W5-E-Guard IpdIdorGuard 包级静态 IDOR 守卫（四大 P0 修复模式抽取）")
class IpdIdorGuardTest {

    private static final Long PROJECT_ID = 5L;
    private static final Long PERSON_SELF = 7L;
    private static final Long PERSON_OTHER = 99L;

    private static final IpdActor ACTOR_NULL_ID = new IpdActor(null, "无名", "MARKET_PM", 1L);
    private static final IpdActor ACTOR_SELF = new IpdActor(PERSON_SELF, "张三", "MARKET_PM", 1L);
    private static final IpdActor ACTOR_OTHER = new IpdActor(PERSON_OTHER, "李四", "RD_PM", 2L);
    private static final IpdActor ACTOR_LEADER = new IpdActor(10L, "王五", "GROUP_LEADER", 20L);
    private static final IpdActor ACTOR_ADMIN = new IpdActor(1L, "超管", "SUPER_ADMIN", null);

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectMapper projectMapper;

    /** 纯 JVM 单测无 MP 运行时，手动初始化 lambda 列缓存（守卫 3 的成员查询需列名解析） */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    private static Project projectWithTenant(String tenantId) {
        Project project = new Project();
        project.setTenantId(tenantId);
        return project;
    }

    // ==================== 守卫 1：requireAuthenticated（3 测） ====================

    @Test
    @DisplayName("G1-1 actor == null → UNAUTHORIZED「未登录」")
    void requireAuthenticated_nullActor_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.requireAuthenticated(null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("G1-2 actor.id() == null → UNAUTHORIZED「未登录」（防御性兜底，控制器已守门）")
    void requireAuthenticated_nullId_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.requireAuthenticated(ACTOR_NULL_ID))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("G1-3 合法 actor → 放行")
    void requireAuthenticated_valid_passes() {
        assertThatCode(() -> IpdIdorGuard.requireAuthenticated(ACTOR_SELF))
            .doesNotThrowAnyException();
    }

    // ==================== 守卫 2：requireSelfOrSuperAdmin（5 测） ====================

    @Test
    @DisplayName("G2-1 本人操作本人数据 → 放行")
    void selfOrSuperAdmin_self_passes() {
        assertThatCode(() -> IpdIdorGuard.requireSelfOrSuperAdmin(ACTOR_SELF, PERSON_SELF))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G2-2 非超管操作他人数据 → FORBIDDEN「无权操作他人数据」（W5-E-2.1 changePassword 核心）")
    void selfOrSuperAdmin_other_forbidden() {
        assertThatThrownBy(() -> IpdIdorGuard.requireSelfOrSuperAdmin(ACTOR_SELF, PERSON_OTHER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权操作他人数据")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("G2-3 SUPER_ADMIN 运维豁免操作他人数据 → 放行")
    void selfOrSuperAdmin_superAdminExempt_passes() {
        assertThatCode(() -> IpdIdorGuard.requireSelfOrSuperAdmin(ACTOR_ADMIN, PERSON_OTHER))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G2-4 personId == null → PARAM_INVALID「personId 不能为空」")
    void selfOrSuperAdmin_nullPersonId_paramInvalid() {
        assertThatThrownBy(() -> IpdIdorGuard.requireSelfOrSuperAdmin(ACTOR_SELF, null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("personId 不能为空")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("G2-5 actor 缺失 → UNAUTHORIZED（链式前置守卫 1）")
    void selfOrSuperAdmin_nullActor_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.requireSelfOrSuperAdmin(null, PERSON_SELF))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    // ==================== 守卫 3：requireProjectMemberOrSuperAdmin（8 测） ====================

    @Test
    @DisplayName("G3-1 在职项目成员（selectCount=1）→ 放行")
    void projectMember_activeMember_passes() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(projectWithTenant("000000"));
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
        assertThatCode(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_SELF, PROJECT_ID, projectMemberMapper, projectMapper))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G3-2 非在职成员（selectCount=0）→ FORBIDDEN「非项目成员，无权访问」")
    void projectMember_nonMember_forbidden() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(projectWithTenant("000000"));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_OTHER, PROJECT_ID, projectMemberMapper, projectMapper))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("非项目成员，无权访问")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("G3-3 selectCount 返回 null（异常口径）→ fail-closed FORBIDDEN，不放行")
    void projectMember_countNull_failClosed() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(projectWithTenant("000000"));
        when(projectMemberMapper.selectCount(any())).thenReturn(null);
        assertThatThrownBy(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_OTHER, PROJECT_ID, projectMemberMapper, projectMapper))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("非项目成员，无权访问")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("G3-4 SUPER_ADMIN 豁免 → 放行且不触达任何 DB 读（豁免先于项目加载）")
    void projectMember_superAdminExempt_noDbRead() {
        assertThatCode(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_ADMIN, PROJECT_ID, projectMemberMapper, projectMapper))
            .doesNotThrowAnyException();
        verifyNoInteractions(projectMapper, projectMemberMapper);
    }

    @Test
    @DisplayName("G3-5 项目不存在 → FORBIDDEN「无权访问该项目」（不泄漏存在性）且不发起成员探测")
    void projectMember_projectMissing_forbidden() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);
        assertThatThrownBy(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_SELF, PROJECT_ID, projectMemberMapper, projectMapper))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权访问该项目")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(projectMemberMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("G3-6 projectId == null → PARAM_INVALID「projectId 不能为空」（先于任何 DB 读）")
    void projectMember_nullProjectId_paramInvalid() {
        assertThatThrownBy(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_SELF, null, projectMemberMapper, projectMapper))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("projectId 不能为空")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verifyNoInteractions(projectMapper, projectMemberMapper);
    }

    @Test
    @DisplayName("G3-7 actor 缺失 → UNAUTHORIZED 且双 mapper 零交互")
    void projectMember_nullActor_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            null, PROJECT_ID, projectMemberMapper, projectMapper))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(projectMapper, projectMemberMapper);
    }

    @Test
    @DisplayName("G3-8 成员查询列名口径锁定：project_id + person_id + exit_date IS NULL（KpiSharedCollectionService 同款）")
    @SuppressWarnings("unchecked")
    void projectMember_wrapperCriteria_locked() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(projectWithTenant("000000"));
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
        IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            ACTOR_SELF, PROJECT_ID, projectMemberMapper, projectMapper);
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectMemberMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
            .contains("project_id")
            .contains("person_id")
            .contains("exit_date");
    }

    // ==================== 守卫 4：requireRoleOrSuperAdmin（4 测） ====================

    @Test
    @DisplayName("G4-1 角色匹配（GROUP_LEADER）→ 放行")
    void roleOrSuperAdmin_matchingRole_passes() {
        assertThatCode(() -> IpdIdorGuard.requireRoleOrSuperAdmin(ACTOR_LEADER, "GROUP_LEADER"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G4-2 角色不匹配（RD_PM 冒充组长）→ FORBIDDEN「无权执行该操作」（先于任何 DB 读）")
    void roleOrSuperAdmin_mismatch_forbidden() {
        assertThatThrownBy(() -> IpdIdorGuard.requireRoleOrSuperAdmin(ACTOR_OTHER, "GROUP_LEADER"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权执行该操作")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("G4-3 SUPER_ADMIN 豁免（角色不匹配仍放行）")
    void roleOrSuperAdmin_superAdminExempt_passes() {
        assertThatCode(() -> IpdIdorGuard.requireRoleOrSuperAdmin(ACTOR_ADMIN, "GROUP_LEADER"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G4-4 requiredRole 空白 → PARAM_INVALID（fail-closed，杜绝 NPE 伪装 500）")
    void roleOrSuperAdmin_blankRequiredRole_paramInvalid() {
        assertThatThrownBy(() -> IpdIdorGuard.requireRoleOrSuperAdmin(ACTOR_LEADER, " "))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("requiredRole 不能为空")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    // ==================== 守卫 5：requireSuperAdmin（3 测） ====================

    @Test
    @DisplayName("G5-1 SUPER_ADMIN → 放行")
    void superAdmin_admin_passes() {
        assertThatCode(() -> IpdIdorGuard.requireSuperAdmin(ACTOR_ADMIN))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("G5-2 非超管（GROUP_LEADER 冒充）→ FORBIDDEN「仅超管可执行」（先于任何 DB 读）")
    void superAdmin_nonAdmin_forbidden() {
        assertThatThrownBy(() -> IpdIdorGuard.requireSuperAdmin(ACTOR_LEADER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅超管可执行")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("G5-3 actor 缺失 → UNAUTHORIZED（链式前置守卫 1）")
    void superAdmin_nullActor_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.requireSuperAdmin(null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    // ==================== W4-Security 租户口径直测（3 测，包级可见性） ====================

    @Test
    @DisplayName("T-1 会话租户与项目租户均非空且不一致 → FORBIDDEN「无权访问该项目」")
    void tenantMatch_mismatch_forbidden() {
        assertThatThrownBy(() -> IpdIdorGuard.requireTenantMatch("tenant-a", projectWithTenant("tenant-b")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权访问该项目")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("T-2 租户上下文缺失（null/空串）或项目无租户 → 放行（W4-Security 决策 1 容忍口径）")
    void tenantMatch_missingContext_passes() {
        Project crossTenantProject = projectWithTenant("tenant-b");
        assertThatCode(() -> IpdIdorGuard.requireTenantMatch(null, crossTenantProject))
            .doesNotThrowAnyException();
        assertThatCode(() -> IpdIdorGuard.requireTenantMatch("", crossTenantProject))
            .doesNotThrowAnyException();
        assertThatCode(() -> IpdIdorGuard.requireTenantMatch("tenant-a", projectWithTenant(null)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("T-3 无 Sa-Token 上下文的纯 JVM 单测环境 currentTenantId() → null/空串且不抛（fail-open 边界由调用方 requireTenantMatch 兜住）")
    void currentTenantId_noSaTokenContext_blank() {
        String tenant = IpdIdorGuard.currentTenantId();
        assertThat(tenant).isNullOrEmpty();
    }
}
