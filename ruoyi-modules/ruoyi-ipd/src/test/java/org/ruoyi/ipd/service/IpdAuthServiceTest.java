package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.hutool.crypto.digest.BCrypt;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证状态机单测（登录判定顺序/冻结移交仅移交权限/先移交后禁用 B8）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class IpdAuthServiceTest {

    @Mock
    private PersonMapper personMapper;
    @Mock
    private AuditLogService auditLogService;

    private IpdAuthService service;

    private static final String HASH = BCrypt.hashpw("plain", BCrypt.gensalt(4));

    /** W5-E-2.1 IDOR 修复：actor 模板（自改用 MARKET_PM=7L；越权测用 99L；超管豁免用 SUPER_ADMIN） */
    private static final IpdActor SELF_ACTOR = new IpdActor(7L, "张三", "MARKET_PM", 1L);
    private static final IpdActor ADMIN_ACTOR = new IpdActor(1L, "超管", "SUPER_ADMIN", 1L);
    private static final IpdActor OTHER_ACTOR = new IpdActor(99L, "李四", "MARKET_PM", 1L);

    @BeforeEach
    void setUp() {
        service = new IpdAuthService(personMapper, auditLogService);
    }

    private Person person(String accountStatus, String employmentStatus, boolean mustChangePwd) {
        Person p = Person.builder()
            .id(7L).name("张三").username("张三").personType("MARKET_PM")
            .passwordHash(HASH)
            .accountStatus(accountStatus).employmentStatus(employmentStatus)
            .mustChangePwd(mustChangePwd ? "1" : "0")
            .build();
        return p;
    }

    @Test
    @DisplayName("ACTIVE 登录 → FULL + mustChangePwd 标记透出 + lastLoginAt 更新")
    void activeLoginFullScope() {
        Person p = person("ACTIVE", "ACTIVE", true);
        when(personMapper.selectOne(any())).thenReturn(p);

        IpdAuthService.LoginResult r = service.login("张三", "plain");

        assertThat(r.scope()).isEqualTo(IpdAuthService.Scope.FULL);
        assertThat(r.mustChangePwd()).isTrue();
        assertThat(p.getLastLoginAt()).isNotNull();
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("密码错误/用户不存在 → 同一报错（防枚举），不泄露存在性")
    void genericErrorPreventsEnumeration() {
        when(personMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.login("不存在", "x"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("用户名或密码错误");

        when(personMapper.selectOne(any())).thenReturn(person("ACTIVE", "ACTIVE", false));
        assertThatThrownBy(() -> service.login("张三", "wrong"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("离职账号拒绝登录")
    void resignedDenied() {
        when(personMapper.selectOne(any())).thenReturn(person("ACTIVE", "RESIGNED", false));
        assertThatThrownBy(() -> service.login("张三", "plain"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("离职");
    }

    @Test
    @DisplayName("DISABLED 拒绝登录")
    void disabledDenied() {
        when(personMapper.selectOne(any())).thenReturn(person("DISABLED", "ACTIVE", false));
        assertThatThrownBy(() -> service.login("张三", "plain"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("禁用");
    }

    @Test
    @DisplayName("冻结移交态 → HANDOVER_ONLY，仅放行移交/改密/刷新路径")
    void frozenScopeHandoverOnly() {
        Person p = person("FROZEN_PENDING_HANDOVER", "ACTIVE", false);
        when(personMapper.selectOne(any())).thenReturn(p);

        IpdAuthService.LoginResult r = service.login("张三", "plain");
        assertThat(r.scope()).isEqualTo(IpdAuthService.Scope.HANDOVER_ONLY);
        assertThat(service.permittedPaths(p))
            .contains("/api/handovers/**")
            .noneMatch(path -> path.equals("*"));
    }

    @Test
    @DisplayName("ACTIVE 全量路径 + 离职禁全量")
    void canAccessFullRules() {
        assertThat(service.canAccessFull(person("ACTIVE", "ACTIVE", false))).isTrue();
        assertThat(service.canAccessFull(person("ACTIVE", "RESIGNED", false))).isFalse();
    }

    @Test
    @DisplayName("改密：旧密码错拒绝；成功后 hash 更新且清 mustChangePwd")
    void changePassword() {
        Person p = person("ACTIVE", "ACTIVE", true);
        when(personMapper.selectById(7L)).thenReturn(p);

        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 7L, "bad-old", "newPassword1"))
            .isInstanceOf(IpdAuthInputException.class)
            .hasMessageContaining("原密码");

        service.changePassword(SELF_ACTOR, 7L, "plain", "newPassword1");
        assertThat(p.getPasswordHash()).isNotEqualTo(HASH);
        assertThat(p.getMustChangePwd()).isEqualTo("0");
    }

    @Test
    @DisplayName("新密码最短 8 位")
    void changePasswordMinLength() {
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 7L, "plain", "short1"))
            .isInstanceOf(IpdAuthInputException.class)
            .hasMessageContaining("8 位");
    }

    @Test
    @DisplayName("B8 顺序：ACTIVE→冻结移交→禁用；未冻结直接禁用被拒")
    void freezeThenDisableOnly() {
        Person active = person("ACTIVE", "ACTIVE", false);
        when(personMapper.selectById(7L)).thenReturn(active);
        service.freezeForHandover(7L, 2L);
        assertThat(active.getAccountStatus()).isEqualTo("FROZEN_PENDING_HANDOVER");
        service.disableAfterHandover(7L, 2L);
        assertThat(active.getAccountStatus()).isEqualTo("DISABLED");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, org.mockito.Mockito.atLeast(2)).append(captor.capture());

        // 未冻结的 ACTIVE 直接禁用 → 拒绝
        Person fresh = person("ACTIVE", "ACTIVE", false);
        when(personMapper.selectById(8L)).thenReturn(fresh);
        assertThatThrownBy(() -> service.disableAfterHandover(8L, 2L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("先移交后禁用");
    }

    @Test
    @DisplayName("冻结幂等：重复冻结不抛错")
    void freezeIdempotent() {
        Person frozen = person("FROZEN_PENDING_HANDOVER", "ACTIVE", false);
        when(personMapper.selectById(7L)).thenReturn(frozen);
        service.freezeForHandover(7L, 2L); // 不抛即通过
    }

    // ===== W5-E-2.1 IDOR 修复 新增 5 重校验 =====

    @Test
    @DisplayName("IDOR-1 自己改自己密码正常（旧密码对、新密码合规）")
    void idorChangeOwnPasswordSucceeds() {
        Person p = person("ACTIVE", "ACTIVE", true);
        when(personMapper.selectById(7L)).thenReturn(p);

        service.changePassword(SELF_ACTOR, 7L, "plain", "newPassword1");
        assertThat(p.getPasswordHash()).isNotEqualTo(HASH);
        assertThat(p.getMustChangePwd()).isEqualTo("0");
    }

    @Test
    @DisplayName("IDOR-2 非超管改别人密码 → FORBIDDEN（核心 IDOR 修复，最高危 P0）")
    void idorChangeOtherPasswordForbidden() {
        // 即使旧密码对、新密码合规，actor(99L) 改 personId=7L 也应被 actor 校验前置拒绝（不触达 mapper）
        assertThatThrownBy(() -> service.changePassword(OTHER_ACTOR, 7L, "plain", "newPassword1"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权修改他人密码");
    }

    @Test
    @DisplayName("IDOR-3 SUPER_ADMIN 改别人密码豁免（运维场景）")
    void idorAdminCanChangeOthersPassword() {
        Person p = person("ACTIVE", "ACTIVE", false);
        when(personMapper.selectById(7L)).thenReturn(p);

        service.changePassword(ADMIN_ACTOR, 7L, "plain", "newPassword1");
        assertThat(p.getPasswordHash()).isNotEqualTo(HASH);
        assertThat(p.getMustChangePwd()).isEqualTo("0");
    }

    @Test
    @DisplayName("IDOR-4 actor == null → UNAUTHORIZED（防御性兜底，控制器已守门）")
    void idorNullActorUnauthorized() {
        assertThatThrownBy(() -> service.changePassword(null, 7L, "plain", "newPassword1"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未认证");
    }

    @Test
    @DisplayName("IDOR-5 personId == null → PARAM_INVALID（参数校验）")
    void idorNullPersonIdParamInvalid() {
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, null, "plain", "newPassword1"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("personId 不能为空");
    }
}