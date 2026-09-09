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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 删除审核引擎状态机单测（BR-DEL/F29；@Tag("dev") 必须）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DeletionRequestServiceTest {

    @Mock
    private DeletionRequestMapper deletionRequestMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DeleteAuditService deleteAuditService;
    /** ROOT-R3-P0-1 修复：跨状态机守卫 mock（fail-closed 改造后必显式注入，否则 preCheckGuard 抛 IpdBusinessException） */
    @Mock
    private StateMachineGuard stateMachineGuard;
    /** W5-E-2.2：目标归属解析 mapper mock（submit/leaderDecision 的 IDOR 校验路径） */
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private GateMapper gateMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private PersonMapper personMapper;

    private DeletionRequestService service;

    /**
     * W5-E-2.2：actor 身份常量（id, name, role, groupId）。
     * 组长属 20 组，目标项目 100 的主组亦为 20（本组组长可匹配；99 组为外组不可匹配）。
     */
    private static final IpdActor ACTOR_REQUESTER = new IpdActor(1L, "市场PM甲", "MARKET_PM", null);
    private static final IpdActor ACTOR_LEADER = new IpdActor(5L, "本组组长", "GROUP_LEADER", 20L);
    private static final IpdActor ACTOR_FOREIGN_LEADER = new IpdActor(6L, "外组组长", "GROUP_LEADER", 99L);
    private static final IpdActor ACTOR_ADMIN = new IpdActor(2L, "超管", "SUPER_ADMIN", null);
    private static final IpdActor ACTOR_PM_OUTSIDER = new IpdActor(7L, "无关市场PM", "MARKET_PM", null);
    private static final IpdActor ACTOR_RD_OUTSIDER = new IpdActor(8L, "无关研发PM", "RD_PM", 99L);

    /** PERF-P0-1：纯 JVM 单测无 MP 运行时，手动初始化 lambda 列缓存（LambdaUpdateWrapper.set/.eq 需列名解析）；
     *  W5-E-2.2：补 ProjectMember（isActiveProjectMember 的 LambdaQueryWrapper 列解析） */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DeletionRequest.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeletionRequestService(
            deletionRequestMapper, systemConfigService, auditLogService, deleteAuditService,
            projectMemberMapper, projectMapper, gateMapper, productMapper, personMapper);
        // ROOT-R3-P0-1 修复：注入 mock 守卫（fail-closed 改造后，preCheckGuard 必显式 fail-fast）
        service.setStateMachineGuard(stateMachineGuard);
    }

    /** W5-E-2.2：目标项目 stub（项目 100 所属主组 groupId） */
    private Project projectOfGroup(Long groupId) {
        Project project = new Project();
        project.setMainGroupId(groupId);
        return project;
    }

    private DeletionRequest saved(Long id, String status, Date createTime, Date leaderDueAt) {
        DeletionRequest request = DeletionRequest.builder()
            .id(id).entityType("projects").entityId(100L).reason("测试删除")
            .requesterId(1L).status(status).leaderDueAt(leaderDueAt)
            .build();
        request.setCreateTime(createTime);
        return request;
    }

    @Test
    @DisplayName("提交 → LEADER_REVIEW，期限 = 2 个工作日（跳过周末）")
    void submitGoesToLeaderReviewWithWorkdayDeadline() {
        when(systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2)).thenReturn(2);
        // W5-E-2.2：在职 ProjectMember 路径（项目 100 成员）
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
        // 周五提交（2026-09-04 是周五）→ +2 工作日 = 下周二
        // R14 教训 14e5d85f 根因类 1：helper date(y,m,d) 内部 m-1，调用方必须传 1-based。
        // Calendar.SEPTEMBER=8 是 0-based → 8-1=7=8月，双重减一；改为字面量 9。
        Date friday = date(2026, 9, 4);
        // R15：注入固定时钟（周五 2026-09-04）→ 期限断言不再依赖真实日历
        // （main 已加 withClock 注入缝，默认 Clock.systemDefaultZone；行内全限定避免 import 区变更）
        service.withClock(java.time.Clock.fixed(friday.toInstant(), java.time.ZoneId.systemDefault()));
        DeletionRequest request = service.submit(ACTOR_REQUESTER, "projects", 100L, "{}", "测试删除");

        assertThat(request.getStatus()).isEqualTo(DeletionRequestService.ST_LEADER_REVIEW);
        Calendar due = Calendar.getInstance();
        due.setTime(request.getLeaderDueAt());
        // 周五 +1 工作日 = 周一(7)，+2 = 周二(8)
        assertThat(due.get(Calendar.DATE)).isEqualTo(8);
        verify(deletionRequestMapper).insert(any(DeletionRequest.class));
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("组长通过 → ADMIN_REVIEW 并设终审期限；审计写入")
    void leaderApproveMovesToAdminReview() {
        when(systemConfigService.getIntValue("deletion.adminDeadlineDays", 2)).thenReturn(2);
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        // W5-E-2.2：目标项目 100 主组 20，本组组长（groupId=20）匹配
        when(projectMapper.selectById(100L)).thenReturn(projectOfGroup(20L));

        DeletionRequest after = service.leaderDecision(ACTOR_LEADER, 9L, true, "同意");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_ADMIN_REVIEW);
        assertThat(after.getLeaderDecision()).isEqualTo("APPROVE");
        assertThat(after.getAdminDueAt()).isNotNull();
    }

    @Test
    @DisplayName("组长否决 → 终态 REJECTED，不设终审期限")
    void leaderRejectIsTerminal() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        // W5-E-2.2：本组组长（groupId=20）匹配目标项目主组
        when(projectMapper.selectById(100L)).thenReturn(projectOfGroup(20L));

        DeletionRequest after = service.leaderDecision(ACTOR_LEADER, 9L, false, "不同意");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_REJECTED);
        assertThat(after.getAdminDueAt()).isNull();
    }

    @Test
    @DisplayName("状态机不匹配：REJECTED 单不可再终审")
    void stateMachineGuardsWrongTransition() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_REJECTED, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);

        assertThatThrownBy(() -> service.adminDecision(ACTOR_ADMIN, 9L, true, "x"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("状态机不匹配");
    }

    @Test
    @DisplayName("超管通过 → 委托 DeleteAuditService.approveAndExecute（P0-6.2 原子软删）")
    void adminApproveExecutes() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_ADMIN_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        DeletionRequest executed = saved(9L, DeletionRequestService.ST_DELETED, new Date(), null);
        executed.setExecutedAt(new Date());
        executed.setAdminDecision("APPROVE");
        when(deleteAuditService.approveAndExecute(9L, 2L)).thenReturn(executed);

        DeletionRequest after = service.adminDecision(ACTOR_ADMIN, 9L, true, "同意删除");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_DELETED);
        assertThat(after.getExecutedAt()).isNotNull();
        assertThat(after.getAdminDecision()).isEqualTo("APPROVE");
        verify(deleteAuditService).approveAndExecute(9L, 2L);
    }

    @Test
    @DisplayName("PERF-P0-1 逾期升级：单 SQL 条件 UPDATE + 补逐条审计（不被逐条 updateById）")
    void escalateOverdue() {
        DeletionRequest overdue = saved(11L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), new Date(System.currentTimeMillis() - 86400_000L));
        when(deletionRequestMapper.selectList(any())).thenReturn(List.of(overdue));
        when(deletionRequestMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(systemConfigService.getIntValue("deletion.adminDeadlineDays", 2)).thenReturn(2);

        int count = service.escalateOverdueLeaderReview();

        assertThat(count).isEqualTo(1);
        // 步骤 ②：单 SQL 条件批量 UPDATE 被调一次（带 LambdaUpdateWrapper）
        verify(deletionRequestMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
        // PERF-P0-1：禁止逐条 updateById（消除 N+1 写放大）
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        // 步骤 ③：受影响行逐条补审计（G-02 语义不变）
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("PERF-P0-1 affected=0 短路：无逾期时零 SQL 额外开销（不调 UPDATE、不调 audit）")
    void escalateOverdueShortCircuitOnNoOverdue() {
        // selectList 返回空 → 直接返回 0，UPDATE 与 audit 都不被调用
        when(deletionRequestMapper.selectList(any())).thenReturn(List.of());

        int count = service.escalateOverdueLeaderReview();

        assertThat(count).isEqualTo(0);
        verify(deletionRequestMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("撤回时限：24h 内可撤回，非申请人不可撤")
    void withdrawGuard() {
        DeletionRequest recent = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(recent);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdraw(9L, 99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅申请人");

        DeletionRequest after = service.withdraw(9L, 1L);
        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_WITHDRAWN);
    }

    @Test
    @DisplayName("撤回时限：超 24h 拒绝")
    void withdrawAfterDeadlineRejected() {
        Date old = new Date(System.currentTimeMillis() - 25 * 3600_000L);
        DeletionRequest oldRequest = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, old, null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(oldRequest);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdraw(9L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("撤回时限");
    }

    // ===== W5-E-2.2（P0 #2）IDOR 修复测试：submit / leaderDecision / adminDecision actor 校验 =====

    @Test
    @DisplayName("IDOR-S1：submit actor=null → UNAUTHORIZED，零 DB 交互")
    void submitNullActorRejected() {
        assertThatThrownBy(() -> service.submit(null, "projects", 100L, "{}", "理由"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
        verifyNoInteractions(projectMapper, projectMemberMapper, gateMapper, productMapper, personMapper);
    }

    @Test
    @DisplayName("IDOR-S2：submit actor.id=null → UNAUTHORIZED（会话不完整）")
    void submitNullActorIdRejected() {
        IpdActor anonymous = new IpdActor(null, "匿名", "MARKET_PM", null);
        assertThatThrownBy(() -> service.submit(anonymous, "projects", 100L, "{}", "理由"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
    }

    @Test
    @DisplayName("IDOR-S3：submit entityType 缺失/未知 → PARAM_INVALID（fail-closed 白名单）")
    void submitUnknownEntityTypeRejected() {
        assertThatThrownBy(() -> service.submit(ACTOR_REQUESTER, null, 100L, "{}", "理由"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("entityType 不能为空")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        assertThatThrownBy(() -> service.submit(ACTOR_REQUESTER, "unknown_type", 100L, "{}", "理由"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("不支持的 entity_type")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
    }

    @Test
    @DisplayName("IDOR-S4：submit 非成员非组长 MARKET_PM 对他人项目 → FORBIDDEN（修复前可对任意资源发起）")
    void submitNonMemberOutsiderForbidden() {
        when(projectMapper.selectById(100L)).thenReturn(projectOfGroup(20L));
        when(projectMemberMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.submit(ACTOR_PM_OUTSIDER, "projects", 100L, "{}", "越权发起"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅资源 owner / 在职项目成员 / 所属组组长可发起删除申请")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("IDOR-S5：submit SUPER_ADMIN 对 cert_templates 豁免（全局参考数据仅超管，归属 mapper 零交互）")
    void submitSuperAdminBypassesScope() {
        DeletionRequest request = service.submit(ACTOR_ADMIN, "cert_templates", 500L, "{}", "清理废弃模板");
        assertThat(request.getStatus()).isEqualTo(DeletionRequestService.ST_LEADER_REVIEW);
        assertThat(request.getRequesterId()).isEqualTo(2L);
        verify(deletionRequestMapper).insert(any(DeletionRequest.class));
        verifyNoInteractions(projectMapper, projectMemberMapper, gateMapper, productMapper, personMapper);
    }

    @Test
    @DisplayName("IDOR-S6：submit 本组组长对本组成员（persons）发起 → 通过（PersonService 本组员工口径）")
    void submitGroupLeaderSameGroupPersonAllowed() {
        Person member = new Person();
        member.setGroupId(20L);
        when(personMapper.selectById(300L)).thenReturn(member);
        DeletionRequest request = service.submit(ACTOR_LEADER, "persons", 300L, "{}", "离职清理");
        assertThat(request.getRequesterId()).isEqualTo(5L);
        assertThat(request.getStatus()).isEqualTo(DeletionRequestService.ST_LEADER_REVIEW);
        verify(deletionRequestMapper).insert(any(DeletionRequest.class));
        // persons 无项目维度：不走成员判定（selectCount 零调用）
        verify(projectMemberMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("IDOR-S7：submit RD_PM 对他组产品（products，非成员+组不匹配）→ FORBIDDEN（fail-closed）")
    void submitProductOutsiderForbidden() {
        Product product = new Product();
        product.setGroupId(20L); // 产品属 20 组；actor 属 99 组且非项目成员
        when(productMapper.selectById(200L)).thenReturn(product);
        assertThatThrownBy(() -> service.submit(ACTOR_RD_OUTSIDER, "products", 200L, "{}", "越权发起"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅资源 owner / 在职项目成员 / 所属组组长可发起删除申请")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
        // product.projectId 为 null → 成员判定短路（selectCount 零调用）
        verify(projectMemberMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("IDOR-L1：leaderDecision actor=null → UNAUTHORIZED")
    void leaderDecisionNullActorRejected() {
        assertThatThrownBy(() -> service.leaderDecision(null, 9L, true, "意见"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(deletionRequestMapper);
    }

    @Test
    @DisplayName("IDOR-L2：leaderDecision 非组长非超管（MARKET_PM）→ FORBIDDEN，角色校验先于任何 DB 读（防冒充组长）")
    void leaderDecisionNonLeaderForbidden() {
        assertThatThrownBy(() -> service.leaderDecision(ACTOR_PM_OUTSIDER, 9L, true, "同意"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅组长或超管可初审删除申请")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(deletionRequestMapper, never()).selectById(any());
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("IDOR-L3：leaderDecision 外组组长（组 99 ≠ 目标主组 20）→ FORBIDDEN，零写入")
    void leaderDecisionForeignGroupLeaderForbidden() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        when(projectMapper.selectById(100L)).thenReturn(projectOfGroup(20L));
        assertThatThrownBy(() -> service.leaderDecision(ACTOR_FOREIGN_LEADER, 9L, true, "越权初审"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅目标所属组组长可初审删除申请")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("IDOR-L4：leaderDecision SUPER_ADMIN 跳过组匹配直批 → ADMIN_REVIEW（无需目标行 stub）")
    void leaderDecisionSuperAdminSkipsGroupCheck() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        DeletionRequest after = service.leaderDecision(ACTOR_ADMIN, 9L, true, "超管直批");
        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_ADMIN_REVIEW);
        assertThat(after.getLeaderId()).isEqualTo(2L);
        verifyNoInteractions(projectMapper);
    }

    @Test
    @DisplayName("IDOR-A1：adminDecision actor=null → UNAUTHORIZED")
    void adminDecisionNullActorRejected() {
        assertThatThrownBy(() -> service.adminDecision(null, 9L, true, "意见"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(deletionRequestMapper, deleteAuditService);
    }

    @Test
    @DisplayName("IDOR-A2：adminDecision 组长冒充超管 → FORBIDDEN，角色校验先于任何 DB 读，软删不可达")
    void adminDecisionNonSuperAdminForbidden() {
        assertThatThrownBy(() -> service.adminDecision(ACTOR_LEADER, 9L, true, "冒充终审"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅超管可终审删除申请")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        // P0 核心：冒充审批在任何 DB 读之前被拒，原子软删/状态写入/审计全部不可达
        verify(deletionRequestMapper, never()).selectById(any());
        verify(deleteAuditService, never()).approveAndExecute(any(), any());
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    private static Date date(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, 10, 0, 0);
        return c.getTime();
    }
}