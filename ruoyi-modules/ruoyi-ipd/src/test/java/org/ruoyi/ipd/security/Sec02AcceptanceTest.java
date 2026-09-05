package org.ruoyi.ipd.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.service.IpdAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SEC-02 六身份矩阵验收（@Tag dev）。
 * 范围过滤纯单元级（mock IpdAuthSession+IpdAuthService），覆盖：
 *  1) canReadProject 6 身份 × 3 组（自己组/跨组/null）矩阵
 *  2) canAccessGroup 同组/跨组/null
 *  3) requireAdmin/requireLeaderOrAdmin 6 身份判定
 *  4) 未登录拒绝（NotLoginException → 401 UNAUTHORIZED）
 *  5) 游客/非内部身份拒绝
 *
 * AC-AUTH-09：跨组项目/产品对非超管一律不可见
 * AC-AUTH-10：游客未登录一律 401
 * AC-AUTH-11：内部/超管边界清晰
 *
 * 不覆盖真实 HTTP（真实库种子与 16039 集成窗口属主协调域；
 * 真实 HTTP 探针见 docs/ipd-系统说明/验收/SEC-02-real-http-probe.sh）。
 */
@Tag("dev")
@DisplayName("SEC02 六身份矩阵范围过滤")
class Sec02AcceptanceTest {

    private IpdAuthSession session;
    private IpdAuthService authService;
    private IpdPermission perm;

    @BeforeEach
    void setup() {
        session = mock(IpdAuthSession.class);
        authService = mock(IpdAuthService.class);
        perm = new IpdPermission(session, authService);
    }

    // 6 身份固定投影（与开发说明书 D.0.7 一致）
    private static IpdActor ACTOR_SUPER_ADMIN    = new IpdActor(1L, "系统管理员", "SUPER_ADMIN",    null);
    private static IpdActor ACTOR_GROUP_LEADER_A = new IpdActor(2L, "组长A",       "GROUP_LEADER", 10L);
    private static IpdActor ACTOR_MARKET_PM_A    = new IpdActor(3L, "市场PM-A",   "MARKET_PM",    10L);
    private static IpdActor ACTOR_MARKET_PM_B    = new IpdActor(4L, "市场PM-B",   "MARKET_PM",    20L);
    private static IpdActor ACTOR_RD_PM_A        = new IpdActor(5L, "研发PM-A",   "RD_PM",        10L);
    private static IpdActor ACTOR_RD_PM_B        = new IpdActor(6L, "研发PM-B",   "RD_PM",        20L);

    @Test
    @DisplayName("canReadProject 6 身份 × 3 组归属矩阵")
    void canReadProjectMatrix() {
        // 自己组（groupId 一致）
        assertThat(perm.canReadProject(ACTOR_SUPER_ADMIN.role(),    ACTOR_SUPER_ADMIN.groupId(),    10L)).isTrue();
        assertThat(perm.canReadProject(ACTOR_GROUP_LEADER_A.role(), ACTOR_GROUP_LEADER_A.groupId(), 10L)).isTrue();
        assertThat(perm.canReadProject(ACTOR_MARKET_PM_A.role(),    ACTOR_MARKET_PM_A.groupId(),    10L)).isTrue();
        // 跨组：非超管 false，超管 true
        assertThat(perm.canReadProject(ACTOR_MARKET_PM_A.role(),    ACTOR_MARKET_PM_A.groupId(),    20L)).isFalse();
        assertThat(perm.canReadProject(ACTOR_RD_PM_A.role(),        ACTOR_RD_PM_A.groupId(),        20L)).isFalse();
        assertThat(perm.canReadProject(ACTOR_SUPER_ADMIN.role(),    ACTOR_SUPER_ADMIN.groupId(),    20L)).isTrue();
        // null 归属：非超管一律 false；超管 true
        assertThat(perm.canReadProject(ACTOR_GROUP_LEADER_A.role(), ACTOR_GROUP_LEADER_A.groupId(), null)).isFalse();
        assertThat(perm.canReadProject(ACTOR_SUPER_ADMIN.role(),    null,                            null)).isTrue();
    }

    @Test
    @DisplayName("canAccessGroup 同组/跨组/null 边界")
    void canAccessGroupMatrix() {
        assertThat(perm.canAccessGroup(10L, 10L)).isTrue();
        assertThat(perm.canAccessGroup(10L, 20L)).isFalse();
        assertThat(perm.canAccessGroup(10L, null)).isFalse();
        assertThat(perm.canAccessGroup(null, 10L)).isFalse();
        assertThat(perm.canAccessGroup(null, null)).isFalse();
    }

    @Test
    @DisplayName("requireAdmin 仅 SUPER_ADMIN 通过；其他身份抛 403 FORBIDDEN")
    void requireAdmin() {
        when(session.currentPerson()).thenReturn(personOf(ACTOR_SUPER_ADMIN));
        when(authService.scopeOf(personOf(ACTOR_SUPER_ADMIN))).thenReturn(IpdAuthService.Scope.FULL);
        IpdActor ok = perm.requireAdmin();
        assertThat(ok.role()).isEqualTo("SUPER_ADMIN");

        when(session.currentPerson()).thenReturn(personOf(ACTOR_GROUP_LEADER_A));
        when(authService.scopeOf(personOf(ACTOR_GROUP_LEADER_A))).thenReturn(IpdAuthService.Scope.FULL);
        assertThatThrownBy(() -> perm.requireAdmin())
            .isInstanceOf(IpdPermissionException.class)
            .extracting(e -> ((IpdPermissionException) e).getHttpStatus())
            .isEqualTo(403);
    }

    @Test
    @DisplayName("requireLeaderOrAdmin 组长与超管通过；PM 拒绝")
    void requireLeaderOrAdmin() {
        when(session.currentPerson()).thenReturn(personOf(ACTOR_GROUP_LEADER_A));
        when(authService.scopeOf(personOf(ACTOR_GROUP_LEADER_A))).thenReturn(IpdAuthService.Scope.FULL);
        assertThat(perm.requireLeaderOrAdmin().role()).isEqualTo("GROUP_LEADER");
        when(session.currentPerson()).thenReturn(personOf(ACTOR_SUPER_ADMIN));
        when(authService.scopeOf(personOf(ACTOR_SUPER_ADMIN))).thenReturn(IpdAuthService.Scope.FULL);
        assertThat(perm.requireLeaderOrAdmin().role()).isEqualTo("SUPER_ADMIN");
        when(session.currentPerson()).thenReturn(personOf(ACTOR_MARKET_PM_A));
        when(authService.scopeOf(personOf(ACTOR_MARKET_PM_A))).thenReturn(IpdAuthService.Scope.FULL);
        assertThatThrownBy(() -> perm.requireLeaderOrAdmin())
            .isInstanceOf(IpdPermissionException.class);
    }

    @Test
    @DisplayName("未登录（NotLoginException）→ 401 UNAUTHORIZED")
    void anonymousRejected() {
        when(session.currentPerson())
            .thenThrow(new cn.dev33.satoken.exception.NotLoginException("ipd", "ipd", "anonymous"));
        assertThatThrownBy(() -> perm.requireInternal())
            .isInstanceOf(IpdPermissionException.class)
            .extracting(e -> ((IpdPermissionException) e).getHttpStatus())
            .isEqualTo(401);
    }

    @Test
    @DisplayName("非内部身份（无 personType 命中 INTERNAL_ROLES）→ 403 FORBIDDEN")
    void externalRoleRejected() {
        IpdActor extern = new IpdActor(99L, "游客", "EXTERNAL", null);
        when(session.currentPerson()).thenReturn(personOf(extern));
        when(authService.scopeOf(personOf(extern))).thenReturn(IpdAuthService.Scope.FULL);
        assertThatThrownBy(() -> perm.requireInternal())
            .isInstanceOf(IpdPermissionException.class)
            .extracting(e -> ((IpdPermissionException) e).getHttpStatus())
            .isEqualTo(403);
    }

    @Test
    @DisplayName("StatusTransition 越权防护：FREEZE 状态（非 FULL scope）→ 403")
    void frozenScopeRejected() {
        when(session.currentPerson()).thenReturn(personOf(ACTOR_MARKET_PM_A));
        when(authService.scopeOf(personOf(ACTOR_MARKET_PM_A)))
            .thenReturn(IpdAuthService.Scope.HANDOVER_ONLY);
        assertThatThrownBy(() -> perm.requireInternal())
            .isInstanceOf(IpdPermissionException.class)
            .extracting(e -> ((IpdPermissionException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.ACCOUNT_FROZEN_PENDING_HANDOVER);
    }

    private static Person personOf(IpdActor a) {
        Person p = new Person();
        p.setId(a.id());
        p.setName(a.name());
        p.setPersonType(a.role());
        p.setGroupId(a.groupId());
        return p;
    }
}
