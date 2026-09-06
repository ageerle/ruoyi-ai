package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-REV-HANDOVER-01：initiateOnBehalf 入口项目归属校验。
 *
 * <p>Bug 复现：HandoverService.initiateOnBehalf 入口未做项目归属校验，
 * 任何登录用户都可对任意 projectId 触发代移交，绕过项目归属（横向越权）。
 *
 * <p>修复：入口加 {@code projectMapper.selectById} + assertSameGroup
 * （SUPER_ADMIN 豁免）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverInitiateAuthTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private HandoverMapper handoverMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ProjectMemberService projectMemberService;

    private HandoverService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE);
    }

    private Project projectInGroup(Long mainGroupId) {
        return Project.builder().id(100L).name("Test-Project").mainGroupId(mainGroupId).build();
    }

    @Test
    @DisplayName("Bug#1: 跨组用户触发代移交 → 拒绝（横向越权防护）")
    void initiateOnBehalf_rejectsCrossGroupLeader() {
        // 项目 mainGroupId = 20 (Group B)
        when(projectMapper.selectById(100L)).thenReturn(projectInGroup(20L));
        // 操作人 groupId = 10 (Group A) —— 跨组
        IpdActor crossGroupLeader = new IpdActor(99L, "leader-A", "GROUP_LEADER", 10L);

        assertThatThrownBy(() -> service.initiateOnBehalf(
                100L, "MARKET_PM", 88L, "note", null, crossGroupLeader))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("代移交项目");

        // 后续任何写入都不应发生 —— 边界在入口，未透传到底层
        verify(handoverMapper, never()).insert(any(org.ruoyi.ipd.domain.HandoverRecord.class));
        verify(auditLogService, never()).append(any());
        verify(memberMapper, never()).update(any(), any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("Bug#1: SUPER_ADMIN 触发代移交 → 入口豁免（不抛横向越权异常）")
    void initiateOnBehalf_superAdminBypassesMembershipCheck() {
        when(projectMapper.selectById(100L)).thenReturn(projectInGroup(20L));
        // memberMapper.selectOne 未 mock → 返回 null → 抛"该项目该角色无在任成员"
        // 这是预期的、非本次 bug 范围的失败；断言：不应抛"代移交项目"
        IpdActor superAdmin = new IpdActor(1L, "admin", "SUPER_ADMIN", null);

        try {
            service.initiateOnBehalf(100L, "MARKET_PM", 88L, "note", null, superAdmin);
        } catch (ServiceException e) {
            assertThat(e.getMessage())
                .as("SUPER_ADMIN 应豁免项目归属校验，不应抛越权异常")
                .doesNotContain("代移交项目");
        }
    }

    @Test
    @DisplayName("Bug#1: 同组 GROUP_LEADER 触发代移交 → 入口不抛越权异常")
    void initiateOnBehalf_sameGroupLeaderPassesAuth() {
        // 项目 mainGroupId = 10，操作人 groupId = 10 —— 同组
        when(projectMapper.selectById(100L)).thenReturn(projectInGroup(10L));
        IpdActor sameGroupLeader = new IpdActor(99L, "leader-A", "GROUP_LEADER", 10L);

        try {
            service.initiateOnBehalf(100L, "MARKET_PM", 88L, "note", null, sameGroupLeader);
        } catch (ServiceException e) {
            assertThat(e.getMessage())
                .as("同组操作人不应抛越权异常；其他 mock 缺失链路可正常失败")
                .doesNotContain("代移交项目");
        }
    }

    @Test
    @DisplayName("Bug#1: 项目不存在 → ServiceException（数据完整性兜底）")
    void initiateOnBehalf_projectNotFound() {
        when(projectMapper.selectById(404L)).thenReturn(null);
        IpdActor leader = new IpdActor(99L, "leader-A", "GROUP_LEADER", 10L);

        assertThatThrownBy(() -> service.initiateOnBehalf(
                404L, "MARKET_PM", 88L, "note", null, leader))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("Bug#1: 入参 role 不在 HANDOVER_ROLES → 后续 createDraft 兜底（与 auth 解耦）")
    void initiateOnBehalf_invalidRoleBypassAuthToCreateDraft() {
        when(projectMapper.selectById(100L)).thenReturn(projectInGroup(10L));
        IpdActor sameGroupLeader = new IpdActor(99L, "leader-A", "GROUP_LEADER", 10L);

        try {
            // role=GUEST 不在 HANDOVER_ROLES={MARKET_PM, RD_PM}
            service.initiateOnBehalf(100L, "GUEST", 88L, "note", null, sameGroupLeader);
        } catch (ServiceException e) {
            // auth 已通过，但 createDraft 拒绝非法 role
            assertThat(e.getMessage()).doesNotContain("代移交项目");
        }
    }
}
