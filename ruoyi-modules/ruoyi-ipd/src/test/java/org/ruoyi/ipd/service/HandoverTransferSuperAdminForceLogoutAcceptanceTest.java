package org.ruoyi.ipd.service;

import cn.dev33.satoken.exception.NotLoginException;
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
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIGH-3.2：HandoverService.transferSuperAdmin 移交完成后强制下线旧 session（4 测）。
 *
 * <p>口径：
 * <ul>
 *   <li>超管移交后原超管 token 立即失效（mock IpdAuthSession.revokeAll 被调 1 次，personId=原超管）</li>
 *   <li>接手超管 session 正常（不会调 revokeAll 接手人，仅原超管被踢）</li>
 *   <li>双重 idempotent：再次 transferSuperAdmin（不同 handoverId 同一原超管）不抛错；
 *       revokeAll 至少再被调 1 次（幂等 = 安全可重入）</li>
 *   <li>失败回滚：revokeAll 抛 NotLoginException 不抛业务异常（Sa-Token 故障不阻塞主链路）</li>
 * </ul>
 *
 * <p>注意：本测试只 mock IpdAuthSession.revokeAll（不验 Sa-Token 内部态）；
 * 上线效果由 IpdAuthSession.revokeAll 委托 StpLogic(logout) 已 P0-7.x 验过（本仓 P0 闭环）
 * 保证。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class HandoverTransferSuperAdminForceLogoutAcceptanceTest {

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
    @Mock
    private IpdAuthSession ipdAuthSession;
    @Mock
    private NotificationService notificationService;

    private HandoverService service;

    private static final long OLD_ADMIN_ID = 1L;
    private static final long NEW_ADMIN_ID = 2L;
    private static final String CONFIRM = "确认移交管理员";

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            ipdAuthSession, notificationService);
    }

    private IpdActor oldAdminActor() {
        return new IpdActor(OLD_ADMIN_ID, "old-admin", "SUPER_ADMIN", null);
    }

    private Person oldAdmin() {
        Person p = new Person();
        p.setId(OLD_ADMIN_ID);
        p.setName("old-admin");
        p.setPersonType("SUPER_ADMIN");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    private Person newAdmin() {
        Person p = new Person();
        p.setId(NEW_ADMIN_ID);
        p.setName("new-admin");
        p.setPersonType("MARKET_PM");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    private void stubSuccessPath() {
        when(personMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(oldAdmin()));
        when(personMapper.selectById(NEW_ADMIN_ID)).thenReturn(newAdmin());
    }

    /* ----------------- HIGH-3.2 4 测 ----------------- */

    @Test
    @DisplayName("HIGH-3.2 ① 超管移交后原超管 token 立即失效（revokeAll 被调 1 次，personId=OLD_ADMIN_ID）")
    void revokeOldAdminSessionOnTransferSuccess() {
        stubSuccessPath();

        service.transferSuperAdmin(NEW_ADMIN_ID, "工作交接", CONFIRM, oldAdminActor());

        // 原超管被踢下线 1 次；接手人不踢
        verify(ipdAuthSession, times(1)).revokeAll(eq(OLD_ADMIN_ID));
        verify(ipdAuthSession, never()).revokeAll(eq(NEW_ADMIN_ID));
    }

    @Test
    @DisplayName("HIGH-3.2 ② 接手超管 session 不被踢（仅原超管 revokeAll）")
    void newAdminSessionUntouched() {
        stubSuccessPath();

        service.transferSuperAdmin(NEW_ADMIN_ID, "工作交接", CONFIRM, oldAdminActor());

        // 仅 OLD_ADMIN_ID 一次；NEW_ADMIN_ID 永不调
        verify(ipdAuthSession).revokeAll(OLD_ADMIN_ID);
        verify(ipdAuthSession, never()).revokeAll(NEW_ADMIN_ID);
        // 全局断言：revokeAll 总调用次数=1
        org.mockito.Mockito.verifyNoMoreInteractions(ipdAuthSession);
    }

    @Test
    @DisplayName("HIGH-3.2 ③ 双重 idempotent：再次 transferSuperAdmin（不同动作同一原超管）不抛错，revokeAll 累计 2 次")
    void doubleTransferIdempotentForSameOldAdmin() {
        // 第一次：原超管 OLD_ADMIN_ID 在任
        stubSuccessPath();
        service.transferSuperAdmin(NEW_ADMIN_ID, "第一轮", CONFIRM, oldAdminActor());

        // 第二次：再次同名 super admin 移交（不同 toPersonId / 不同 note；同原超管）
        Person anotherNew = new Person();
        anotherNew.setId(3L);
        anotherNew.setName("another-new-admin");
        anotherNew.setPersonType("MARKET_PM");
        anotherNew.setAccountStatus("ACTIVE");
        anotherNew.setEmploymentStatus("ACTIVE");
        when(personMapper.selectById(3L)).thenReturn(anotherNew);

        // 幂等：不抛业务异常（DB 守卫由 person 侧 AC 守卫 + single-admin 不变式兜底；此处只验业务流可重入）
        assertThatCode(() -> service.transferSuperAdmin(3L, "第二轮", CONFIRM, oldAdminActor()))
            .doesNotThrowAnyException();

        // revokeAll 累计 2 次（每次移交都踢原超管，与原超管是不是同一人无关——只踢当前 admins.get(0)）
        verify(ipdAuthSession, times(2)).revokeAll(OLD_ADMIN_ID);
    }

    @Test
    @DisplayName("HIGH-3.2 ④ 失败回滚：revokeAll 抛 NotLoginException 不抛业务异常（Sa-Token 故障不阻塞主链路）")
    void revokeAllFailureDoesNotBlockMainFlow() {
        stubSuccessPath();
        // mock revokeAll 抛 Sa-Token 异常（典型场景：旧 token 已被踢、Redis 故障等）
        doThrow(new NotLoginException("ipd", NotLoginException.INVALID_TOKEN, "模拟 Sa-Token 故障"))
            .when(ipdAuthSession).revokeAll(OLD_ADMIN_ID);

        // 主链路不应因 Sa-Token 异常而失败（DB 提交已发生，audit 已落）
        assertThatCode(() -> service.transferSuperAdmin(NEW_ADMIN_ID, "故障演练", CONFIRM, oldAdminActor()))
            .doesNotThrowAnyException();

        // 仍尝试踢一次（fail-soft，不 silent skip）
        verify(ipdAuthSession, times(1)).revokeAll(OLD_ADMIN_ID);

        // 业务层：DB 写入 + 审计均成功
        verify(auditLogService, org.mockito.Mockito.atLeastOnce()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("HIGH-3.2 ⑤ 守卫：非超管发起 → 拒绝，且 revokeAll 不调（与失败回滚互不干扰）")
    void nonSuperAdminCannotTriggerRevoke() {
        IpdActor nonAdmin = new IpdActor(99L, "pm", "MARKET_PM", 7L);

        assertThatThrownBy(() -> service.transferSuperAdmin(NEW_ADMIN_ID, "test", CONFIRM, nonAdmin))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("超管");

        // 守卫在入口，未到 revokeAll 调用点
        verify(ipdAuthSession, never()).revokeAll(any());
    }
}
