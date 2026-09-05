package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-AUD-01 (S2 缩范围)：login 失败分支必须写审计。
 *
 * <p>验证 4 类失败分支都落审计行（action=LOGIN_FAIL）：
 * <ul>
 *   <li>用户名不存在（person=null）</li>
 *   <li>密码错误（person 存在但 BCrypt 不匹配）</li>
 *   <li>RESIGNED 离职账号</li>
 *   <li>DISABLED 账号</li>
 * </ul>
 * 修复前：失败分支仅 throw ServiceException，auditLogService.append 0 次调用。
 * 修复后：失败分支走 auditLogService.append（AuditLogService 内部 REQUIRES_NEW 提交）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class IpdAuthLoginAuditTest {

    @Mock
    private PersonMapper personMapper;
    @Mock
    private AuditLogService auditLogService;

    private IpdAuthService service;

    @BeforeEach
    void setUp() {
        service = new IpdAuthService(personMapper, auditLogService);
    }

    private Person activePerson(String username, String password) {
        Person p = new Person();
        p.setId(1L);
        p.setName("测试 PM");
        p.setUsername(username);
        p.setPasswordHash(password);
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setMustChangePwd("0");
        p.setDelFlag("0");
        return p;
    }

    @Test
    @DisplayName("login 失败：用户名不存在 → 写 audit (LOGIN_FAIL, personId=null)")
    void login_userNotFound_writesAudit() {
        when(personMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.login("ghost", "any"))
            .isInstanceOf(ServiceException.class);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("LOGIN_FAIL");
        assertThat(log.getEntityType()).isEqualTo("persons");
        assertThat(log.getEntityId()).as("personId must be null when username not found").isNull();
    }

    @Test
    @DisplayName("login 失败：密码错误 → 写 audit (LOGIN_FAIL, personId=1)")
    void login_wrongPassword_writesAudit() {
        Person p = activePerson("alice", cn.hutool.crypto.digest.BCrypt.hashpw("right-pwd", cn.hutool.crypto.digest.BCrypt.gensalt(10)));
        when(personMapper.selectOne(any())).thenReturn(p);

        assertThatThrownBy(() -> service.login("alice", "wrong-pwd"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("用户名或密码错误");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("LOGIN_FAIL");
        assertThat(log.getEntityId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("login 失败：RESIGNED 离职 → 写 audit (LOGIN_FAIL + reason=RESIGNED)")
    void login_resigned_writesAudit() {
        Person p = activePerson("bob", cn.hutool.crypto.digest.BCrypt.hashpw("any", cn.hutool.crypto.digest.BCrypt.gensalt(10)));
        p.setEmploymentStatus("RESIGNED");
        when(personMapper.selectOne(any())).thenReturn(p);

        assertThatThrownBy(() -> service.login("bob", "any"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("离职");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("LOGIN_FAIL");
    }

    @Test
    @DisplayName("login 失败：DISABLED 账号 → 写 audit (LOGIN_FAIL + reason=DISABLED)")
    void login_disabled_writesAudit() {
        Person p = activePerson("carol", cn.hutool.crypto.digest.BCrypt.hashpw("any", cn.hutool.crypto.digest.BCrypt.gensalt(10)));
        p.setAccountStatus("DISABLED");
        when(personMapper.selectOne(any())).thenReturn(p);

        assertThatThrownBy(() -> service.login("carol", "any"))
            .isInstanceOf(ServiceException.class);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("LOGIN_FAIL");
    }

    @Test
    @DisplayName("login 成功 → 不写 LOGIN_FAIL 审计（仍写 LOGIN 成功审计）")
    void login_success_writesLoginNotLoginFail() {
        Person p = activePerson("dave", cn.hutool.crypto.digest.BCrypt.hashpw("pwd", cn.hutool.crypto.digest.BCrypt.gensalt(10)));
        when(personMapper.selectOne(any())).thenReturn(p);

        IpdAuthService.LoginResult result = service.login("dave", "pwd");
        assertThat(result).isNotNull();

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        assertThat(cap.getValue().getAction())
            .as("success must write LOGIN not LOGIN_FAIL")
            .isEqualTo("LOGIN");
    }
}
