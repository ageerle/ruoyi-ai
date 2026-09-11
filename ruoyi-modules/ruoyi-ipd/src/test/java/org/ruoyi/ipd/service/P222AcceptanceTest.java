package org.ruoyi.ipd.service;

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
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * P2-2.2 离职冻结、复职与绑定解绑联动 验收测试（AC-AUTH-06 / AC-HAND-01）。
 *
 * <p>9 测矩阵：
 * <ol>
 *   <li>AC-AUTH-06 正例：mark-resigned → status=FROZEN_PENDING_HANDOVER + employment=RESIGNED</li>
 *   <li>AC-AUTH-06 反例：FROZEN_PENDING_HANDOVER 账号调 login → 拒绝（scopeOf→HANDOVER_ONLY，
 *       /api/projects 仅 ADMIN/GROUP_LEADER 可写）</li>
 *   <li>企微绑定自动解除：wecom_user_id 清空 + accountStatus 仍 FROZEN_PENDING_HANDOVER</li>
 *   <li>解绑审计：audit_logs 至少 1 条 action=UNBIND_WECHAT</li>
 *   <li>AC-HAND-01 保留移交权：FROZEN 账号可调 /handovers/*（scopeOf→HANDOVER_ONLY + permittedPaths 包含 /api/handovers/**）</li>
 *   <li>AC-HAND-01 反例：FROZEN 账号非移交路径应被拒（permittedPaths 不含 /api/projects）</li>
 *   <li>重复事件幂等：同一 person 二次 mark-resigned → idempotent=true + 不重复转移</li>
 *   <li>通知发送：notifications 表新增 ≥ 4 条（双方组长 + 本人 + 超管）</li>
 *   <li>15 日倒计时升级：HrSyncService.escalateStaleResignations 命中 stale 项 → 超管收到升级通知</li>
 * </ol>
 *
 * <p>本测试为 mockito 单元测试，专注 service 层联动行为；HTTP 端到端验证由 main session 走
 * 真 server / curl 路径（兄弟会话在途）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-2.2 离职冻结、复职与绑定解绑联动")
class P222AcceptanceTest {

    @Mock PersonMapper personMapper;
    @Mock ProjectMemberMapper memberMapper;
    @Mock AuditLogService auditLogService;
    @Mock IpdAuthSession ipdAuthSession;
    @Mock NotificationService notificationService;

    @InjectMocks PersonService personService;
    /** HrSyncService 依赖 PersonService（@InjectMocks 仅注入一个 — HrSyncService 手构） */
    private HrSyncService hrSyncService;

    private IpdActor admin;
    private IpdActor groupLeaderA;
    private IpdActor groupLeaderB;
    private IpdActor selfActor;

    @BeforeEach
    void setUp() {
        admin = new IpdActor(1L, "系统管理员", "SUPER_ADMIN", null);
        groupLeaderA = new IpdActor(10L, "王组长", "GROUP_LEADER", 100L);
        groupLeaderB = new IpdActor(11L, "李组长", "GROUP_LEADER", 200L);
        selfActor = new IpdActor(900L, "陈市场", "MARKET_PM", 100L);
        // HrSyncService 需手构（依赖 PersonService + 4 mock）
        hrSyncService = new HrSyncService(personService, notificationService, personMapper,
            memberMapper);
        // 默认 stub：updateById 返 1（保证 happy path 不抛 ServiceException）
        lenient().when(personMapper.updateById(any(Person.class))).thenReturn(1);
        // 默认 stub：selectCount 返 0L（无活跃项目）
        lenient().when(memberMapper.selectCount(any())).thenReturn(0L);
        // 默认 stub：通知派发返非 null（避免 NPE；测试具体场景时显式 stub 列表）
        lenient().when(notificationService.publish(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(NotificationEvent.builder().id(1L).build());
    }

    private Person person(Long id, String emp, String acc, String wecom, Long groupId, String type) {
        return Person.builder()
            .id(id)
            .name("Test-" + id)
            .employeeNo("E" + id)
            .personType(type == null ? "MARKET_PM" : type)
            .groupId(groupId == null ? 100L : groupId)
            .level("L3")
            .employmentStatus(emp)
            .accountStatus(acc)
            .wecomUserId(wecom)
            .wecomBoundAt(wecom == null ? null : new Date())
            .username("u" + id)
            .delFlag("0")
            .build();
    }

    // ===== 测 1: AC-AUTH-06 正例 =====
    @Test
    @DisplayName("1. AC-AUTH-06 正例: mark-resigned → FROZEN_PENDING_HANDOVER + RESIGNED")
    void markResigned_freezesToPendingHandover() {
        Person p = person(900L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(900L)).thenReturn(p);

        var result = hrSyncService.markResignedByHr(900L, "HR 系统推送", admin);

        assertThat(result.idempotent()).isFalse();
        assertThat(result.message()).contains("冻结成功");
        ArgumentCaptor<Person> saved = ArgumentCaptor.forClass(Person.class);
        verify(personMapper).updateById(saved.capture());
        assertThat(saved.getValue().getEmploymentStatus()).isEqualTo("RESIGNED");
        assertThat(saved.getValue().getAccountStatus()).isEqualTo("FROZEN_PENDING_HANDOVER");
    }

    // ===== 测 2: AC-AUTH-06 反例: FROZEN 账号权限收窄 =====
    @Test
    @DisplayName("2. AC-AUTH-06 反例: FROZEN_PENDING_HANDOVER 账号 scope=HANDOVER_ONLY（拒绝普通业务口）")
    void frozenAccount_hasHandoverOnlyScope() {
        Person p = person(900L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 100L, "MARKET_PM");
        // 模拟登录判定：FROZEN_PENDING_HANDOVER → HANDOVER_ONLY
        // 由 IpdAuthService.scopeOf 走相同逻辑，本处直验其外部契约
        boolean full = new IpdAuthService(personMapper, auditLogService).canAccessFull(p);
        assertThat(full).isFalse();
        // permittedPaths 不含 /api/projects（仅移交 + 改密 + refresh）
        List<String> paths = new IpdAuthService(personMapper, auditLogService).permittedPaths(p);
        assertThat(paths).doesNotContain("/api/projects");
    }

    // ===== 测 3: 企微绑定自动解除 =====
    @Test
    @DisplayName("3. 企微绑定自动解除: wecom 清空 + accountStatus 仍 FROZEN（非 DISABLED）")
    void autoUnbindWecom_keepsFrozenStatus() {
        Person p = person(901L, "ACTIVE", "ACTIVE", "wc_abc_999", 100L, "MARKET_PM");
        when(personMapper.selectById(901L)).thenReturn(p);

        var result = hrSyncService.markResignedByHr(901L, "个人原因", admin);

        assertThat(result.wecomUnbound()).isTrue();
        ArgumentCaptor<Person> saved = ArgumentCaptor.forClass(Person.class);
        verify(personMapper, atLeastOnce()).updateById(saved.capture());
        // 末次保存必须保留 FROZEN_PENDING_HANDOVER，不可降为 DISABLED
        Person lastSaved = saved.getAllValues().get(saved.getAllValues().size() - 1);
        assertThat(lastSaved.getWecomUserId()).isNull();
        assertThat(lastSaved.getAccountStatus()).isEqualTo("FROZEN_PENDING_HANDOVER");
    }

    // ===== 测 4: 解绑审计 =====
    @Test
    @DisplayName("4. 解绑审计: audit_logs 至少 1 条 action=UNBIND_WECHAT + 1 条 RESIGN")
    void unbindWecom_writesAudit() {
        Person p = person(902L, "ACTIVE", "ACTIVE", "wc_xyz_111", 100L, "MARKET_PM");
        when(personMapper.selectById(902L)).thenReturn(p);

        hrSyncService.markResignedByHr(902L, "调岗", admin);

        ArgumentCaptor<AuditLog> audits = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(audits.capture());
        List<AuditLog> all = audits.getAllValues();
        // 必须有 RESIGN + UNBIND_WECHAT + REVOKE_SESSIONS 至少三条
        assertThat(all).extracting(AuditLog::getAction)
            .contains("RESIGN", "UNBIND_WECHAT", "REVOKE_SESSIONS");
        AuditLog unbindAudit = all.stream()
            .filter(a -> "UNBIND_WECHAT".equals(a.getAction())).findFirst().orElseThrow();
        assertThat(unbindAudit.getEntityId()).isEqualTo(902L);
        assertThat(unbindAudit.getReason()).contains("AC-AUTH-06");
    }

    // ===== 测 5: AC-HAND-01 保留移交权 =====
    @Test
    @DisplayName("5. AC-HAND-01 保留移交权: FROZEN 账号 permittedPaths 含 /api/handovers/**")
    void frozenAccount_canAccessHandoverPaths() {
        Person p = person(903L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 100L, "MARKET_PM");
        List<String> paths = new IpdAuthService(personMapper, auditLogService).permittedPaths(p);
        assertThat(paths).contains("/api/handovers/**");
        assertThat(paths).contains("/api/auth/password");
        assertThat(paths).contains("/api/auth/refresh");
    }

    // ===== 测 6: AC-HAND-01 反例: FROZEN 非移交路径被拒 =====
    @Test
    @DisplayName("6. AC-HAND-01 反例: FROZEN 账号 permittedPaths 不含 /api/projects")
    void frozenAccount_cannotAccessBusinessPaths() {
        Person p = person(904L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 100L, "MARKET_PM");
        List<String> paths = new IpdAuthService(personMapper, auditLogService).permittedPaths(p);
        assertThat(paths).doesNotContain("/api/projects/**");
        assertThat(paths).doesNotContain("/api/persons/*");
        assertThat(paths).doesNotContain("*");
    }

    // ===== 测 7: 重复事件幂等 =====
    @Test
    @DisplayName("7. 重复事件幂等: 二次 mark-resigned → idempotent=true + 不重复 audit/notify/revoke")
    void idempotent_repeatedMarkResigned() {
        // P2-2.2 改造：wecom=null 避免触发自动解绑的二次 updateById；幂等本测聚焦二次不重写
        Person p = person(905L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(905L)).thenReturn(p);

        var first = hrSyncService.markResignedByHr(905L, "首次", admin);
        var second = hrSyncService.markResignedByHr(905L, "二次", admin);

        assertThat(first.idempotent()).isFalse();
        assertThat(second.idempotent()).isTrue();
        assertThat(second.message()).contains("幂等");
        // 第二次不应再触发 updateById / audit / 通知
        verify(personMapper, times(1)).updateById(any(Person.class));
        // audit 仅首次 RESIGN + REVOKE_SESSIONS 二条（wecom 已空无 UNBIND_WECHAT），二次不重写
        verify(auditLogService, times(2)).append(any(AuditLog.class));
        // publish 仅首次发本人通知 1 条（无本组/对方/超管 stub 均返空 list），二次不重写
        verify(notificationService, times(1)).publish(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString());
        verify(ipdAuthSession, times(1)).revokeAll(anyLong());
    }

    // ===== 测 8: 通知派发 4 类收件人 =====
    @Test
    @DisplayName("8. 通知派发: 本人 + 双方组长 + 全部超管 ≥ 4 条")
    void notifications_dispatchedToFourGroups() {
        Person p = person(906L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(906L)).thenReturn(p);
        // 本组组长（100 组）— ACTIVE GROUP_LEADER
        Person leaderA = person(10L, "ACTIVE", "ACTIVE", null, 100L, "GROUP_LEADER");
        // 对方组长（200 组）— ACTIVE GROUP_LEADER
        Person leaderB = person(11L, "ACTIVE", "ACTIVE", null, 200L, "GROUP_LEADER");
        // 超管
        Person adminP = person(1L, "ACTIVE", "ACTIVE", null, null, "SUPER_ADMIN");
        when(personMapper.selectList(any())).thenReturn(List.of(leaderA), List.of(leaderB), List.of(adminP));

        var result = hrSyncService.markResignedByHr(906L, "组织调整", admin);

        // 1 (本人) + 1 (本组组长) + 1 (对方组长) + 1 (超管) = 4（actor=admin 不是本人所以本人也算）
        // 注：admin.operator=1 ≠ 906 所以本人通知仍发
        assertThat(result.notificationsSent()).isEqualTo(4);
        ArgumentCaptor<String> kinds = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> receivers = ArgumentCaptor.forClass(Long.class);
        verify(notificationService, times(4)).publish(receivers.capture(), anyString(),
            kinds.capture(), anyString(), anyLong(), anyString(), anyString(),
            anyString());
        assertThat(receivers.getAllValues()).containsExactlyInAnyOrder(906L, 10L, 11L, 1L);
        // 本组组长 ACTION（有项目时）/ FYI（无项目时）；本测无项目 → FYI
        // 超管 ACTION（升级兜底）
        assertThat(kinds.getAllValues()).contains(NotificationService.KIND_ACTION); // 超管
    }

    // ===== 测 8b: 通知派发 actor=本人时跳过本人收件箱 =====
    @Test
    @DisplayName("8b. 通知派发: actor=本人时跳过自收件箱（不冗余发）")
    void notifications_skipSelfWhenActorIsSelf() {
        Person p = person(907L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(907L)).thenReturn(p);
        // 关键：selfActor.id() 必须 == person.id() 才走"本人跳过"分支
        IpdActor selfOfThisPerson = new IpdActor(907L, "陈市场", "MARKET_PM", 100L);
        // 仅本组组长 + 超管，无对方组长
        Person leaderA = person(10L, "ACTIVE", "ACTIVE", null, 100L, "GROUP_LEADER");
        Person adminP = person(1L, "ACTIVE", "ACTIVE", null, null, "SUPER_ADMIN");
        when(personMapper.selectList(any())).thenReturn(List.of(leaderA), List.of(), List.of(adminP));

        var result = hrSyncService.markResignedByHr(907L, "自愿", selfOfThisPerson);

        // actor=本人 → 本人收件箱跳过；1 (本组组长) + 0 (对方) + 1 (超管) = 2
        assertThat(result.notificationsSent()).isEqualTo(2);
    }

    // ===== 测 9: 15 日倒计时升级 =====
    @Test
    @DisplayName("9. 15 日倒计时升级: escalateStaleResignations 命中 stale 项 → 超管收通知")
    void escalator_notifiesSuperAdminsForStaleFrozen() {
        // 模拟一个 20 天前冻结的人员（setUpdateTime 而非 builder.updateTime — Person extends BaseEntity）
        Person stale = Person.builder()
            .id(908L)
            .name("Old-Frozen")
            .employeeNo("E908")
            .personType("MARKET_PM")
            .groupId(100L)
            .level("L3")
            .employmentStatus("RESIGNED")
            .accountStatus("FROZEN_PENDING_HANDOVER")
            .delFlag("0")
            .build();
        stale.setUpdateTime(new Date(System.currentTimeMillis() - 20L * 24L * 60L * 60L * 1000L));
        Person adminP = person(1L, "ACTIVE", "ACTIVE", null, null, "SUPER_ADMIN");
        when(personMapper.selectList(any())).thenReturn(List.of(stale), List.of(adminP));
        // active projects = 2（仍待移交）
        when(memberMapper.selectCount(any())).thenReturn(2L);

        int escalated = hrSyncService.escalateStaleResignations(15, admin);

        assertThat(escalated).isEqualTo(1);
        ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> kind = ArgumentCaptor.forClass(String.class);
        verify(notificationService).publish(eq(1L), eventType.capture(), kind.capture(),
            anyString(), eq(908L), anyString(), anyString(), anyString());
        assertThat(eventType.getValue()).isEqualTo(PersonService.EVT_RESIGN_ESCALATION);
        assertThat(kind.getValue()).isEqualTo(NotificationService.KIND_ACTION);
    }

    @Test
    @DisplayName("9b. 15 日倒计时升级: 阈值内（ageDays < 15）不升级")
    void escalator_skipsFreshFrozen() {
        Person fresh = Person.builder()
            .id(909L)
            .name("Fresh-Frozen")
            .employeeNo("E909")
            .personType("MARKET_PM")
            .groupId(100L)
            .level("L3")
            .employmentStatus("RESIGNED")
            .accountStatus("FROZEN_PENDING_HANDOVER")
            .delFlag("0")
            .build();
        fresh.setUpdateTime(new Date(System.currentTimeMillis() - 5L * 24L * 60L * 60L * 1000L));
        when(personMapper.selectList(any())).thenReturn(List.of(fresh));

        int escalated = hrSyncService.escalateStaleResignations(15, admin);

        assertThat(escalated).isZero();
        verify(notificationService, never()).publish(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString());
    }

    // ===== 复职路径: rehire 仅 RESIGNED → ACTIVE（明确允许分支） =====
    @Test
    @DisplayName("复职: RESIGNED + FROZEN → ACTIVE（明确允许分支）")
    void rehire_explicitAllowedBranch() {
        Person p = person(910L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 100L, "MARKET_PM");
        when(personMapper.selectById(910L)).thenReturn(p);

        Person result = personService.rehire(910L, "返岗", admin);

        assertThat(result.getEmploymentStatus()).isEqualTo("ACTIVE");
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
        // 复职不动 wecom（已被离职冻结时清空）；不动 updateTime 外的其他字段
    }

    @Test
    @DisplayName("复职: ACTIVE 状态拒绝（非明确允许分支 — 状态机守恒）")
    void rehire_rejectsAlreadyActive() {
        Person p = person(911L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(911L)).thenReturn(p);

        assertThatThrownBy(() -> personService.rehire(911L, "x", admin))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("复职: DISABLED 拒绝（需先解禁 — 仍为唯一复职前置路径）")
    void rehire_rejectsDisabled() {
        Person p = person(912L, "RESIGNED", "DISABLED", null, 100L, "MARKET_PM");
        when(personMapper.selectById(912L)).thenReturn(p);

        assertThatThrownBy(() -> personService.rehire(912L, "x", admin))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("禁用");
    }

    // ===== listPendingHandoverEscalations 视图 =====
    @Test
    @DisplayName("pendingHandovers 视图: 含 ageDays + escalate + activeProjects 字段")
    void pendingHandovers_viewIncludesAgeAndEscalate() {
        Person frozen = Person.builder()
            .id(913L)
            .name("Pending")
            .employeeNo("E913")
            .personType("MARKET_PM")
            .groupId(100L)
            .level("L3")
            .employmentStatus("RESIGNED")
            .accountStatus("FROZEN_PENDING_HANDOVER")
            .delFlag("0")
            .build();
        frozen.setUpdateTime(new Date(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L));
        when(personMapper.selectList(any())).thenReturn(List.of(frozen));
        when(memberMapper.selectCount(any())).thenReturn(3L);

        List<HrSyncService.PendingHandover> result =
            hrSyncService.listPendingHandoverEscalations(15);

        assertThat(result).hasSize(1);
        HrSyncService.PendingHandover ph = result.get(0);
        assertThat(ph.personId()).isEqualTo(913L);
        assertThat(ph.name()).isEqualTo("Pending");
        assertThat(ph.activeProjects()).isEqualTo(3L);
        assertThat(ph.ageDays()).isGreaterThanOrEqualTo(29L).isLessThanOrEqualTo(31L);
        assertThat(ph.escalate()).isTrue();
    }

    // ===== Session revoke 副作用验证 =====
    @Test
    @DisplayName("session revoke: ipdAuthSession.revokeAll(personId) 被调用")
    void sessionRevoke_calledOnResign() {
        Person p = person(914L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(914L)).thenReturn(p);

        hrSyncService.markResignedByHr(914L, "x", admin);

        verify(ipdAuthSession, times(1)).revokeAll(914L);
    }

    @Test
    @DisplayName("session revoke 失败不阻塞主链路: revokeAll 抛异常 → resign 仍成功")
    void sessionRevoke_failureDoesNotBlockResign() {
        Person p = person(915L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(915L)).thenReturn(p);
        org.mockito.Mockito.doThrow(new RuntimeException("Sa-Token 故障"))
            .when(ipdAuthSession).revokeAll(915L);

        var result = hrSyncService.markResignedByHr(915L, "x", admin);

        assertThat(result.idempotent()).isFalse();
        assertThat(result.sessionsRevoked()).isFalse();
        assertThat(result.message()).contains("冻结成功");
        // 主路径 status 仍写入
        ArgumentCaptor<Person> saved = ArgumentCaptor.forClass(Person.class);
        verify(personMapper).updateById(saved.capture());
        assertThat(saved.getValue().getAccountStatus()).isEqualTo("FROZEN_PENDING_HANDOVER");
    }

    // ===== 企微已空幂等：不重复解绑审计 =====
    @Test
    @DisplayName("企微已空: 二次 mark-resigned 不重复写 UNBIND_WECHAT 审计")
    void idempotentUnbind_whenWecomAlreadyEmpty() {
        // 二次 mark-resigned 走幂等返回路径，根本不进 autoUnbind — 不会写 UNBIND_WECHAT
        Person p = person(916L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 100L, "MARKET_PM");
        when(personMapper.selectById(916L)).thenReturn(p);

        var result = hrSyncService.markResignedByHr(916L, "二次", admin);

        assertThat(result.idempotent()).isTrue();
        // audit 完全不调（幂等路径）
        verify(auditLogService, never()).append(any(AuditLog.class));
        verify(ipdAuthSession, never()).revokeAll(anyLong());
    }

    // ===== 联调空对照：纯粹 ACTIVE 无 wecom 路径通知派发计数 =====
    @Test
    @DisplayName("联动副作用计数: ACTIVE 无 wecom 无项目 → wecomUnbound=false sessionsRevoked=true notificationsSent=本组组长+对方组长+超管")
    void linkCounters_correctForCleanResign() {
        Person p = person(917L, "ACTIVE", "ACTIVE", null, 100L, "MARKET_PM");
        when(personMapper.selectById(917L)).thenReturn(p);
        Person leaderA = person(10L, "ACTIVE", "ACTIVE", null, 100L, "GROUP_LEADER");
        Person leaderB = person(11L, "ACTIVE", "ACTIVE", null, 200L, "GROUP_LEADER");
        Person adminP = person(1L, "ACTIVE", "ACTIVE", null, null, "SUPER_ADMIN");
        when(personMapper.selectList(any())).thenReturn(List.of(leaderA), List.of(leaderB), List.of(adminP));

        var result = hrSyncService.markResignedByHr(917L, "x", admin);

        assertThat(result.wecomUnbound()).isFalse();
        assertThat(result.sessionsRevoked()).isTrue();
        // actor=admin ≠ 917 本人 → 本人也收 → 1+1+1+1=4
        assertThat(result.notificationsSent()).isEqualTo(4);
    }
}
