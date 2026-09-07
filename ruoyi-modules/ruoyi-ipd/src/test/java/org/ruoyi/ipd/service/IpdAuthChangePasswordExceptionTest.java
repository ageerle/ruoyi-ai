package org.ruoyi.ipd.service;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-7.2 验收：IpdAuthService.changePassword 必须抛 IpdAuthInputException（被 IpdAuthController 审计），
 * 不能抛裸 ServiceException（不会触发审计）。
 * <p>W5-E-2.1 IDOR 修复后签名：{@code changePassword(IpdActor, Long, String, String)}，
 * 必须传 actor（防御性兜底 + 越权拦截），本测试全部沿用 SELF_ACTOR=id=1。
 */
@Tag("dev")
class IpdAuthChangePasswordExceptionTest {

    private final PersonMapper personMapper = mock(PersonMapper.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final IpdAuthService service = new IpdAuthService(personMapper, auditLogService);

    /** W5-E-2.1：自改密码场景 actor=自己=personId=1，本测试专注异常分支，全部走自改路径 */
    private static final IpdActor SELF_ACTOR = new IpdActor(1L, "x", "MARKET_PM", 1L);

    @Test
    @DisplayName("新密码 < 8 位 → IpdAuthInputException(PASSWORD_LENGTH)，不写 audit")
    void shortPasswordThrowsAuthInputException() {
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 1L, "old12345", "abc"))
            .isInstanceOf(IpdAuthInputException.class)
            .extracting("reason").isEqualTo(IpdAuthInputException.Reason.PASSWORD_LENGTH);
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("新密码 null → IpdAuthInputException(PASSWORD_LENGTH)")
    void nullPasswordThrowsAuthInputException() {
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 1L, "old12345", null))
            .isInstanceOf(IpdAuthInputException.class)
            .extracting("reason").isEqualTo(IpdAuthInputException.Reason.PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("原密码错 → IpdAuthInputException(CURRENT_PASSWORD_INCORRECT)，不写 audit 不 update")
    void wrongOldPasswordThrowsAuthInputException() {
        Person p = Person.builder().id(1L).name("x").passwordHash(BCrypt.hashpw("rightOld"))
            .mustChangePwd("1").accountStatus("ACTIVE").build();
        when(personMapper.selectById(1L)).thenReturn(p);
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 1L, "wrongOld", "newPassword123"))
            .isInstanceOf(IpdAuthInputException.class)
            .extracting("reason").isEqualTo(IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("新密码 = 当前密码 → IpdAuthInputException(PASSWORD_UNCHANGED)")
    void samePasswordThrowsAuthInputException() {
        String pwd = "samePassword123";
        Person p = Person.builder().id(1L).name("x").passwordHash(BCrypt.hashpw(pwd))
            .mustChangePwd("1").accountStatus("ACTIVE").build();
        when(personMapper.selectById(1L)).thenReturn(p);
        assertThatThrownBy(() -> service.changePassword(SELF_ACTOR, 1L, pwd, pwd))
            .isInstanceOf(IpdAuthInputException.class)
            .extracting("reason").isEqualTo(IpdAuthInputException.Reason.PASSWORD_UNCHANGED);
        verify(personMapper, never()).updateById(any(Person.class));
    }

    @Test
    @DisplayName("改密成功 → 清 must_change_pwd='0' + 写 audit PASSWORD_CHANGE")
    void successClearsMustChangeAndAudits() {
        String oldPwd = "oldPwd12345";
        String newPwd = "newPwd67890";
        Person p = Person.builder().id(1L).name("x").passwordHash(BCrypt.hashpw(oldPwd))
            .mustChangePwd("1").accountStatus("ACTIVE").build();
        when(personMapper.selectById(1L)).thenReturn(p);
        when(personMapper.updateById(any(Person.class))).thenReturn(1);
        service.changePassword(SELF_ACTOR, 1L, oldPwd, newPwd);
        // 核心：必须写 PASSWORD_CHANGE 审计
        verify(auditLogService, times(1)).append(any(AuditLog.class));
        // mustChangePwd 已清 0
        assertThat(p.getMustChangePwd()).isEqualTo("0");
    }

    @Test
    @DisplayName("反例：绝对不允许抛裸 ServiceException（P0-7.2 卡要求）")
    void neverThrowsBareServiceException() {
        Person p = Person.builder().id(1L).name("x").passwordHash(BCrypt.hashpw("rightPwd"))
            .mustChangePwd("1").accountStatus("ACTIVE").build();
        when(personMapper.selectById(1L)).thenReturn(p);
        // 三个失败分支都必须是 IpdAuthInputException
        try {
            service.changePassword(SELF_ACTOR, 1L, "wrongPwd", "newPwd12345");
            org.junit.jupiter.api.Assertions.fail("expected exception");
        } catch (ServiceException e) {
            org.junit.jupiter.api.Assertions.fail("must NOT throw bare ServiceException, was: " + e.getClass());
        } catch (IpdAuthInputException e) {
            // 期望路径
        }
    }
}
