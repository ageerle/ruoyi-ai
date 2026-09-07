package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.IpdAuthController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdAuthSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-7.4 企微 Mock 扫码登录 + 解绑闭环验收测试（AC-AUTH-04 / AC-AUTH-05 + AC-USER-10 + Mock 不冒充约束）。
 *
 * <p>覆盖（7 个测试，不重不漏）：
 * <ul>
 *   <li>1. 已绑定 wecomUserId → login() 路径成功 → JWT 签发 + lastLoginAt 更新 + audit mock=true</li>
 *   <li>2. 未绑定 wecomUserId → NOT_FOUND + "账号未绑定，请联系管理员" + audit FAILURE</li>
 *   <li>3. 空 wecomUserId → PARAM_INVALID（不查 DB、不写审计）</li>
 *   <li>4. 绑定但 account=DISABLED → NOT_FOUND（同错误信息，避免泄露越权尝试）</li>
 *   <li>5. 绑定但 employment=RESIGNED → NOT_FOUND（同上）</li>
 *   <li>6. Controller wecomQrLogin 已绑定 → 200 + token 签发（直接调用，不走 MockMvc）</li>
 *   <li>7. Controller wecomQrLogin 未绑定 → 404 NOT_FOUND + message</li>
 * </ul>
 *
 * <p>解绑审计约束（AC-USER-10）：{@code PersonService.unbindWecom} 已写 audit action=WECOM_UNBIND，
 * 由 {@code PersonRehireWecomGroupLimitAcceptanceTest} 覆盖；本卡不重复。
 *
 * <p>Mock 阶段明示约束：审计 reason 字段必含 {@code mock=true} 标记，便于运营/法务事后甄别
 * Mock 阶段记录——未来真实企微接入替换此端点后仍可历史追溯。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P0-7.4 企微 Mock 扫码登录")
class P074AcceptanceTest {

    @Mock PersonMapper personMapper;
    @Mock AuditLogService auditLogService;
    @Mock IpdAuthSession session;

    private IpdAuthService authService;
    private IpdAuthController controller;

    private static final String HASH = "$2a$04$dummyhashfordeterministicmapping";

    @BeforeEach
    void setUp() {
        authService = new IpdAuthService(personMapper, auditLogService);
        controller = new IpdAuthController(authService, session, null, null);
    }

    /** 构造 Person：避免 lastLoginAt 在构造期被 Mockito 默认 null 干扰。 */
    private Person person(Long id, String accountStatus, String employmentStatus,
                          boolean mustChangePwd, String wecomUserId) {
        return Person.builder()
            .id(id).name("Test-" + id).username("u" + id)
            .personType("MARKET_PM").groupId(1L).level("L3")
            .passwordHash(HASH)
            .accountStatus(accountStatus).employmentStatus(employmentStatus)
            .mustChangePwd(mustChangePwd ? "1" : "0")
            .wecomUserId(wecomUserId)
            .delFlag("0")
            .build();
    }

    // ─────────────── Service: wecomMockLogin ───────────────

    @Test
    @DisplayName("AC-AUTH-04 已绑定 wecomUserId → FULL scope + lastLoginAt 更新 + audit(WECOM_MOCK_LOGIN, mock=true)")
    void wecomMockLogin_bound_success() {
        Person p = person(7L, "ACTIVE", "ACTIVE", false, "wc_001");
        when(personMapper.selectOne(any())).thenReturn(p);

        IpdAuthService.LoginResult result = authService.wecomMockLogin("wc_001");

        assertThat(result.scope()).isEqualTo(IpdAuthService.Scope.FULL);
        assertThat(result.mustChangePwd()).isFalse();
        assertThat(p.getLastLoginAt()).isNotNull();
        verify(personMapper).updateById(p);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("WECOM_MOCK_LOGIN");
        assertThat(audit.getEntityType()).isEqualTo("persons");
        assertThat(audit.getEntityId()).isEqualTo(7L);
        assertThat(audit.getReason()).isEqualTo("mock=true");
        assertThat(audit.getOperatorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("AC-AUTH-05 未绑定 wecomUserId → NOT_FOUND + audit(WECOM_MOCK_LOGIN_FAIL)")
    void wecomMockLogin_notBound_notFound() {
        when(personMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.wecomMockLogin("wc_unknown"))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(e -> {
                IpdBusinessException ibe = (IpdBusinessException) e;
                assertThat(ibe.getErrorCode()).isEqualTo(ApiV1ErrorCode.NOT_FOUND);
                assertThat(ibe.getMessage()).isEqualTo("账号未绑定，请联系管理员");
            });

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("WECOM_MOCK_LOGIN_FAIL");
        assertThat(audit.getEntityId()).isNull();
        assertThat(audit.getOperatorName()).isEqualTo("wc_unknown");
        assertThat(audit.getReason()).contains("mock=true").contains("NOT_BOUND");
    }

    @Test
    @DisplayName("空 wecomUserId → PARAM_INVALID（前置校验，不查 DB、不写审计）")
    void wecomMockLogin_empty_paramInvalid() {
        // null
        assertThatThrownBy(() -> authService.wecomMockLogin(null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        // blank
        assertThatThrownBy(() -> authService.wecomMockLogin("   "))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // 前置校验：DB / 审计都不应被触达
        verify(personMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("绑定但 account=DISABLED → NOT_FOUND（同错误信息，避免越权尝试泄露）")
    void wecomMockLogin_disabled_notFound() {
        Person p = person(8L, "DISABLED", "ACTIVE", false, "wc_002");
        when(personMapper.selectOne(any())).thenReturn(p);

        assertThatThrownBy(() -> authService.wecomMockLogin("wc_002"))
            .isInstanceOf(IpdBusinessException.class)
            .satisfies(e -> {
                IpdBusinessException ibe = (IpdBusinessException) e;
                assertThat(ibe.getErrorCode()).isEqualTo(ApiV1ErrorCode.NOT_FOUND);
                assertThat(ibe.getMessage()).isEqualTo("账号未绑定，请联系管理员");
            });
        verify(personMapper, never()).updateById(any(Person.class));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        assertThat(captor.getValue().getReason()).contains("DISABLED");
    }

    @Test
    @DisplayName("绑定但 employment=RESIGNED → NOT_FOUND（同错误信息，避免越权尝试泄露）")
    void wecomMockLogin_resigned_notFound() {
        Person p = person(9L, "ACTIVE", "RESIGNED", false, "wc_003");
        when(personMapper.selectOne(any())).thenReturn(p);

        assertThatThrownBy(() -> authService.wecomMockLogin("wc_003"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        assertThat(captor.getValue().getReason()).contains("RESIGNED");
    }

    // ─────────────── Controller: wecomQrLogin ───────────────

    @Test
    @DisplayName("Controller 已绑定 → 200 + LoginView(token, Bearer, scope=FULL)")
    void wecomQrLogin_bound_returnsToken() {
        Person p = person(10L, "ACTIVE", "ACTIVE", true, "wc_004");
        when(personMapper.selectOne(any())).thenReturn(p);
        when(session.login(p)).thenReturn("token-jwt-xyz");
        when(session.timeout()).thenReturn(1800L);

        org.ruoyi.ipd.common.ApiV1Response<IpdAuthController.LoginView> resp =
            controller.wecomQrLogin(new IpdAuthController.WecomLoginRequest("wc_004"));

        assertThat(resp).isNotNull();
        assertThat(resp.getCode()).isEqualTo(0);
        IpdAuthController.LoginView view = resp.getData();
        assertThat(view.token()).isEqualTo("token-jwt-xyz");
        assertThat(view.tokenType()).isEqualTo("Bearer");
        assertThat(view.expiresIn()).isEqualTo(1800L);
        assertThat(view.scope()).isEqualTo("FULL");
        assertThat(view.mustChangePwd()).isTrue();
        assertThat(view.person().id()).isEqualTo("10");
    }

    @Test
    @DisplayName("Controller 未绑定 → service 抛 IpdBusinessException(NOT_FOUND)，全局异常处理映射 404")
    void wecomQrLogin_notBound_notFound() {
        when(personMapper.selectOne(any())).thenReturn(null);

        // Controller 层透传 service 异常（全局异常处理器统一映射 ApiV1Response → HTTP 404）
        assertThatThrownBy(() ->
            controller.wecomQrLogin(new IpdAuthController.WecomLoginRequest("wc_unknown")))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }
}