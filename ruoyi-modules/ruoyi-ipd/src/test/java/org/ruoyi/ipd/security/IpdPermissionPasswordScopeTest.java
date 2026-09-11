package org.ruoyi.ipd.security;

import cn.dev33.satoken.exception.NotLoginException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.service.IpdAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * [SEC-AUTH-DEADLOCK] 2026-09-07 真端到端发现：
 * 原 requireInternal() 在 scope=PASSWORD_CHANGE_REQUIRED 一律抛 20003，
 * 形成 first-login 账号无法自助解锁的死锁。本类验证新增
 * {@link IpdPermission#requireInternalEvenIfPasswordScope()}：
 * 仅验 token + 内部角色，scope 放宽到 PASSWORD_CHANGE_REQUIRED / HANDOVER_ONLY，
 * IDOR 防御交给 service 层 actor.id == personId 守卫。
 */
@Tag("dev")
class IpdPermissionPasswordScopeTest {

    private final IpdAuthSession session = mock(IpdAuthSession.class);
    private final IpdAuthService authService = mock(IpdAuthService.class);
    private final IpdPermission permission = new IpdPermission(session, authService);

    private static Person personWithRoleAndStatus(String role, String mustChangePwd, String accountStatus) {
        return Person.builder().id(42L).name("x").username("x")
            .personType(role).groupId(1L).accountStatus(accountStatus)
            .mustChangePwd(mustChangePwd).build();
    }

    @Test
    @DisplayName("PASSWORD_CHANGE_REQUIRED + MARKET_PM：通过（不死锁），scope 验证留给 service")
    void passwordChangeRequiredPasses() {
        Person p = personWithRoleAndStatus("MARKET_PM", "1", "ACTIVE");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.PASSWORD_CHANGE_REQUIRED);
        IpdActor actor = permission.requireInternalEvenIfPasswordScope();
        assertThat(actor.id()).isEqualTo(42L);
        assertThat(actor.role()).isEqualTo("MARKET_PM");
    }

    @Test
    @DisplayName("HANDOVER_ONLY + RD_PM：通过（兼容冻结待移交态改密）")
    void handoverOnlyPasses() {
        Person p = personWithRoleAndStatus("RD_PM", "0", "FROZEN_PENDING_HANDOVER");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.HANDOVER_ONLY);
        IpdActor actor = permission.requireInternalEvenIfPasswordScope();
        assertThat(actor.id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("FULL + SUPER_ADMIN：通过")
    void fullAdminPasses() {
        Person p = personWithRoleAndStatus("SUPER_ADMIN", "0", "ACTIVE");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.FULL);
        IpdActor actor = permission.requireInternalEvenIfPasswordScope();
        assertThat(actor.role()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    @DisplayName("NONE：401 UNAUTHORIZED（与 requireInternal 一致：账户已禁用不可改密）")
    void noneStillRejected() {
        Person p = personWithRoleAndStatus("MARKET_PM", "0", "DISABLED");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.NONE);
        assertThatThrownBy(() -> permission.requireInternalEvenIfPasswordScope())
            .isInstanceOf(IpdPermissionException.class)
            .satisfies(e -> assertThat(((IpdPermissionException) e).getErrorCode())
                .isEqualTo(org.ruoyi.ipd.common.ApiV1ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("未登录（NotLoginException）：401 UNAUTHORIZED")
    void notLoginStillRejected() {
        when(session.currentPerson()).thenThrow(new NotLoginException("x", "NOT_LOGIN", "session expired"));
        assertThatThrownBy(() -> permission.requireInternalEvenIfPasswordScope())
            .isInstanceOf(IpdPermissionException.class);
    }

    @Test
    @DisplayName("非内部角色（如 EXTERNAL_GUEST）：403 denied——内部账号改密专属")
    void externalRoleDenied() {
        Person p = personWithRoleAndStatus("EXTERNAL_GUEST", "0", "ACTIVE");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.FULL);
        assertThatThrownBy(() -> permission.requireInternalEvenIfPasswordScope())
            .isInstanceOf(IpdPermissionException.class);
    }

    @Test
    @DisplayName("对比验证：原 requireInternal 在 PASSWORD_CHANGE_REQUIRED 抛 20003（死锁根因）")
    void originalRequireInternalRejectsPasswordScope() {
        Person p = personWithRoleAndStatus("MARKET_PM", "1", "ACTIVE");
        when(session.currentPerson()).thenReturn(p);
        when(authService.scopeOf(any(Person.class))).thenReturn(IpdAuthService.Scope.PASSWORD_CHANGE_REQUIRED);
        assertThatThrownBy(() -> permission.requireInternal())
            .isInstanceOf(IpdPermissionException.class)
            .satisfies(e -> assertThat(((IpdPermissionException) e).getErrorCode())
                .isEqualTo(org.ruoyi.ipd.common.ApiV1ErrorCode.ACCOUNT_PASSWORD_CHANGE_REQUIRED));
    }
}
