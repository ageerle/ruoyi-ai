package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-共识 闭环验收：scanOverdueDrafts 跨租户隔离守卫（4 路专家共识 2026-09-08）。
 *
 * <p>修复前：HandoverService.scanOverdueDrafts L626-633（原行号）的升级/提醒循环无 tenant 过滤，
 * 跨租户扫描泄漏风险（4 路专家共识：架构/安全/性能/代码质量独立识别同一 P0）。
 *
 * <p>修复后：
 * <ul>
 *   <li>LambdaQueryWrapper 加 .eq(HandoverRecord::getTenantId, currentTenant) — 非 SUPER_ADMIN 必加</li>
 *   <li>SUPER_ADMIN 走 all-tenant 分支（守卫 7 同款）</li>
 *   <li>循环内对每条记录做 tenant 断言（非 SUPER_ADMIN 不匹配 ⇒ skip）</li>
 *   <li>审计 tenantId 与实体 tenantId 一致</li>
 * </ul>
 *
 * <p>4 个测试：
 * <ol>
 *   <li>非 SUPER_ADMIN + currentTenant="000000"：selectList 调用含 tenantId 过滤（核心守卫）</li>
 *   <li>SUPER_ADMIN + currentTenant=any：selectList 调用不含 tenantId 过滤（全租户豁免）</li>
 *   <li>跨 tenant 记录被 skip：循环内断言拦截（record.tenantId != currentTenant ⇒ 不升级）</li>
 *   <li>审计 tenantId 一致：非默认 tenant 写入时，AuditLog.tenantId == record.tenantId</li>
 * </ol>
 *
 * <p>纯 JVM 单测 + LoginHelper.mockStatic（Mockito 5.17 内置），不依赖 Sa-Token；
 * 端到端真 HTTP 真 DB 验收另走 curl 阶段。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P0-共识 闭环：scanOverdueDrafts 跨租户隔离守卫")
class ScanOverdueTenantGuardAcceptanceTest {

    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private HandoverMapper handoverMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMemberService projectMemberService;
    @Mock private IpdAuthSession ipdAuthSession;
    @Mock private NotificationService notificationService;

    private HandoverService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
    }

    private static final IpdActor SUPER_ADMIN =
        new IpdActor(900101L, "ipd-admin", "SUPER_ADMIN", null);
    private static final IpdActor MARKET_PM_ACTOR =
        new IpdActor(900103L, "ipd-market", "MARKET_PM", null);

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            ipdAuthSession, notificationService);
        // 单超管不变式
        Person superAdmin = new Person();
        superAdmin.setId(900101L);
        superAdmin.setPersonType("SUPER_ADMIN");
        superAdmin.setAccountStatus("ACTIVE");
        Mockito.lenient().when(personMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(superAdmin));
    }

    private HandoverRecord overdueDraft(long id, long projectId, String tenantId) {
        HandoverRecord r = new HandoverRecord();
        r.setId(id);
        r.setProjectId(projectId);
        r.setFromPersonId(301L);
        r.setToPersonId(401L);
        r.setHandoverRole("MARKET_PM");
        r.setStatus("DRAFT");
        r.setDeadlineAt(new Date(System.currentTimeMillis() - 86_400_000L));
        r.setCreateTime(new Date(System.currentTimeMillis() - 30L * 86_400_000L));
        r.setTenantId(tenantId);
        return r;
    }

    // ============== T1: 非 SUPER_ADMIN 必加 tenantId 过滤 ==============

    @Test
    @DisplayName("T1 非 SUPER_ADMIN + currentTenant=000000：selectList 入参含 tenant_id 等值过滤（核心守卫）")
    void nonSuperAdminShouldAddTenantFilter() {
        try (MockedStatic<LoginHelper> mocked = Mockito.mockStatic(LoginHelper.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(LoginHelper::getTenantId).thenReturn("000000");
            when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

            HandoverService.OverdueScanResult result = service.scanOverdueDrafts(MARKET_PM_ACTOR);

            assertThat(result.escalated()).isZero();
            assertThat(result.reminded()).isZero();

            // 验证两次 selectList 入参都含 tenant_id 过滤（SQL 段含 "tenant_id = " 占位符）
            ArgumentCaptor<LambdaQueryWrapper<HandoverRecord>> wrapCap =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(handoverMapper, times(2)).selectList(wrapCap.capture());
            String firstCondition = wrapCap.getAllValues().get(0).getExpression().getNormal().getSqlSegment();
            String secondCondition = wrapCap.getAllValues().get(1).getExpression().getNormal().getSqlSegment();
            assertThat(firstCondition).contains("tenant_id =");
            assertThat(firstCondition).contains("escalated_at IS NULL"); // 升级路径守卫仍在
            assertThat(secondCondition).contains("tenant_id =");
        }
    }

    // ============== T2: SUPER_ADMIN 也按 session tenant 过滤（更严格） ==============

    @Test
    @DisplayName("T2 SUPER_ADMIN + currentTenant=000000：selectList 入参同样含 tenant_id 过滤（P0 更严格语义）")
    void superAdminStillFiltersBySessionTenant() {
        try (MockedStatic<LoginHelper> mocked = Mockito.mockStatic(LoginHelper.class, Mockito.CALLS_REAL_METHODS)) {
            // 即便 SUPER_ADMIN，session tenant 非空时同样按 tenant 过滤（避免跨租户扫描泄漏）
            mocked.when(LoginHelper::getTenantId).thenReturn("000000");
            when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

            HandoverService.OverdueScanResult result = service.scanOverdueDrafts(SUPER_ADMIN);

            assertThat(result.escalated()).isZero();
            assertThat(result.reminded()).isZero();

            // 验证两次 selectList 入参都含 tenant_id 过滤
            ArgumentCaptor<LambdaQueryWrapper<HandoverRecord>> wrapCap =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(handoverMapper, times(2)).selectList(wrapCap.capture());
            String firstCondition = wrapCap.getAllValues().get(0).getExpression().getNormal().getSqlSegment();
            String secondCondition = wrapCap.getAllValues().get(1).getExpression().getNormal().getSqlSegment();
            assertThat(firstCondition).contains("tenant_id =");
            assertThat(secondCondition).contains("tenant_id =");
        }
    }

    // ============== T3: 跨 tenant 记录被循环内断言拦截 ==============

    @Test
    @DisplayName("T3 跨 tenant 记录（tenant=000001 vs actor=000000）被 skip：escalated/reminded 不递增")
    void crossTenantRecordShouldBeSkipped() {
        try (MockedStatic<LoginHelper> mocked = Mockito.mockStatic(LoginHelper.class, Mockito.CALLS_REAL_METHODS)) {
            // actor=MARKET_PM_ACTOR + currentTenant=000000
            // query 结果含一条跨租户记录 tenant=000001（防御性兜底场景：即便 query wrapper 有 bug，
            // 循环内断言仍兜底拦截；这里 simulate wrapper 不带 tenant filter 时返回跨租户数据）
            mocked.when(LoginHelper::getTenantId).thenReturn("000000");
            HandoverRecord crossTenant = overdueDraft(7001L, 7L, "000001"); // 不同 tenant
            // 让 toEscalate 返回跨租户记录（绕开 wrapper 过滤、验证 loop 内断言）
            when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(crossTenant))
                .thenReturn(List.of(crossTenant));
            // 注意：update 在本测试不应被调用（loop 内 skip 拦截），不要 stub 强制返回 1，
            // 避免 Mockito 误判为"未使用 stub"——直接让 Mockito 在未调用时返回 0

            HandoverService.OverdueScanResult result = service.scanOverdueDrafts(MARKET_PM_ACTOR);

            // 跨租户被 loop 内断言拦截 ⇒ escalated=0, reminded=0
            assertThat(result.escalated()).isZero();
            assertThat(result.reminded()).isZero();

            // 不发升级通知 / 不发提醒 / 不写 update
            verify(notificationService, never()).publish(anyLong(), eq("HANDOVER_OVERDUE_ESCALATION"),
                any(), any(), any(), any(), any(), any());
            verify(notificationService, never()).publishDaily(anyLong(), eq("HANDOVER_DAILY_REMINDER"),
                any(), any(), any(), any(), any(), any(), any());
            verify(handoverMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        }
    }

    // ============== T4: 审计 tenantId 与实体 tenantId 一致 ==============

    @Test
    @DisplayName("T4 session tenant=000001 的 actor：审计 AuditLog.tenantId == record.tenantId=000001（避免审计跨租户漂移）")
    void auditTenantIdShouldMatchRecord() {
        try (MockedStatic<LoginHelper> mocked = Mockito.mockStatic(LoginHelper.class, Mockito.CALLS_REAL_METHODS)) {
            // session tenant = 000001，actor 为 GROUP_LEADER（非 SUPER_ADMIN）
            // 注意：query wrapper 加 tenant_id='000001' 过滤；返回 tenant=000001 记录 ⇒ loop 不 skip
            IpdActor groupLeader = new IpdActor(900102L, "ipd-leader", "GROUP_LEADER", null);
            mocked.when(LoginHelper::getTenantId).thenReturn("000001");
            HandoverRecord rec = overdueDraft(8001L, 7L, "000001"); // 与 actor tenant 一致
            when(handoverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(rec))
                .thenReturn(List.of(rec));
            when(handoverMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            service.scanOverdueDrafts(groupLeader);

            ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogService, atLeastOnce()).append(auditCap.capture());
            // 至少 1 条审计 tenantId == "000001"（与 record 一致）
            assertThat(auditCap.getAllValues())
                .extracting(AuditLog::getTenantId)
                .contains("000001");
        }
    }
}
