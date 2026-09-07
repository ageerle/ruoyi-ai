package org.ruoyi.ipd.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.controller.IpdAuthController;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditAttemptService;
import org.ruoyi.ipd.service.IpdAuthInputException;
import org.ruoyi.ipd.service.IpdAuthService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-03 验收：IpdAuthInputException 在<b>真实 HTTP 层</b>（MockMvc dispatch）被
 * {@link IpdServiceExceptionAdvice} 映射为 HTTP 400 + code 10001（PARAM_INVALID），
 * 而非落入 Exception 兜底的 500 + code 90001（INTERNAL_ERROR）。
 *
 * <p>卡片要求「补一条 MockMvc 真实 HTTP 反例证明状态码与 code，不得仅靠服务层断言关闭」。
 * 与 {@link Api01AcceptanceTest}（直接 new advice 调 handler）互补：本测试经 standaloneSetup
 * 走完整 Spring MVC 请求分发 → 控制器 → 全局异常处理链 → JSON 响应体，断言客户端真正收到的
 * 状态码与业务码，堵住「handler 就位即认为关卡通过」的假绿。
 *
 * <p>改密三类可纠正输入错误（消息只来自固定枚举，不携带凭据）：
 * 原密码错（CURRENT_PASSWORD_INCORRECT）、新密码不足 8 位（PASSWORD_LENGTH / @Valid @Size）、
 * 新旧相同（PASSWORD_UNCHANGED）。约定沿用 {@code P064AcceptanceTest}：plain {@code mock()}
 * 无 MockitoExtension，故 setup 内共享桩在个别用例未触达时不会报 UnnecessaryStubbing。
 */
@Tag("dev")
class Api03AcceptanceTest {

    private static final String CHANGE_PWD = "/api/v1/auth/change-password";
    private static final long PERSON_ID = 1L;
    /** PARAM_INVALID 业务码；断言用字面量对齐 P064AcceptanceTest 风格。 */
    private static final int CODE_PARAM_INVALID = 10001;
    private static final int CODE_OK = 0;

    private final ObjectMapper json = new ObjectMapper();
    private IpdAuthService authService;
    private IpdAuthSession session;
    private AuditAttemptService auditAttempt;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        authService = mock(IpdAuthService.class);
        session = mock(IpdAuthSession.class);
        auditAttempt = mock(AuditAttemptService.class);
        Person actor = Person.builder()
            .id(PERSON_ID).name("张三").personType("MARKET_PM").groupId(9L).build();
        when(session.currentPerson()).thenReturn(actor);
        // W5-E-2.1 IDOR 修复：Controller 注入 IpdPermission；mock requireInternal 返回 actor
        IpdPermission permission = mock(IpdPermission.class);
        when(permission.requireInternal()).thenReturn(
            new IpdActor(actor.getId(), actor.getName(), actor.getPersonType(), actor.getGroupId()));
        // 注入真实生产 advice（非本地替身），才能证明 IpdAuthInputException 专用 handler 真的生效。
        mvc = MockMvcBuilders
            .standaloneSetup(new IpdAuthController(authService, session, auditAttempt, permission))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .setControllerAdvice(new IpdServiceExceptionAdvice())
            .build();
    }

    /** 构造一次改密 POST：请求体经 Jackson 序列化为 PasswordRequest record 的 JSON。 */
    private MockHttpServletRequestBuilder changePwd(String currentPassword, String newPassword) throws Exception {
        return post(CHANGE_PWD)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(
                new IpdAuthController.PasswordRequest(currentPassword, newPassword)));
    }

    @Test
    @DisplayName("反例①原密码错误 → HTTP 400 + code 10001，枚举消息保留（非 500/90001 兜底）")
    void currentPasswordIncorrectMapsTo400ParamInvalid() throws Exception {
        doThrow(new IpdAuthInputException(IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT))
            .when(authService).changePassword(any(IpdActor.class), anyLong(), anyString(), anyString());

        mvc.perform(changePwd("wrongOldPwd", "brandNewPwd1"))
            .andExpect(status().isBadRequest())                       // 修复前会是 500
            .andExpect(jsonPath("$.code").value(CODE_PARAM_INVALID))  // 修复前会是 90001
            .andExpect(jsonPath("$.message").value("原密码错误"));      // 真实枚举消息未被兜底吞成「系统内部错误」
    }

    @Test
    @DisplayName("反例②新旧密码相同 → HTTP 400 + code 10001")
    void passwordUnchangedMapsTo400ParamInvalid() throws Exception {
        doThrow(new IpdAuthInputException(IpdAuthInputException.Reason.PASSWORD_UNCHANGED))
            .when(authService).changePassword(any(IpdActor.class), anyLong(), anyString(), anyString());

        mvc.perform(changePwd("samePwd123", "samePwd123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_INVALID))
            .andExpect(jsonPath("$.message").value("新密码不能与当前密码相同"));
    }

    @Test
    @DisplayName("反例③服务层 PASSWORD_LENGTH（多字节超 72B 但字符数过 @Size）→ HTTP 400 + code 10001")
    void passwordLengthFromServiceMapsTo400ParamInvalid() throws Exception {
        // 30 个汉字：字符数 30 通过 @Size(min=8,max=72)，但 UTF-8 达 90 字节 > 72，服务层按字节上限拒绝。
        // 证明「新密码不足/超限」即便绕过 Bean Validation，由服务层抛出，仍映射 4xx 而非 500。
        doThrow(new IpdAuthInputException(IpdAuthInputException.Reason.PASSWORD_LENGTH))
            .when(authService).changePassword(any(IpdActor.class), anyLong(), anyString(), anyString());

        mvc.perform(changePwd("oldPwd123", "新".repeat(30)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_INVALID))
            .andExpect(jsonPath("$.message").value("新密码长度至少 8 位，UTF-8编码不得超过72字节"));
    }

    @Test
    @DisplayName("反例④新密码不足 8 位 → @Valid @Size 在 HTTP 边界拦截 → HTTP 400 + code 10001")
    void shortPasswordRejectedByBeanValidationAt400() throws Exception {
        // newPassword 长度 5 < 8，触发 MethodArgumentNotValidException → handleValidation → 400/10001，不进服务层。
        mvc.perform(changePwd("oldPwd123", "short"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(CODE_PARAM_INVALID));
    }

    @Test
    @DisplayName("正例：合法改密 → HTTP 200 + code 0，并轮换撤销该人全部会话")
    void successfulChangePasswordReturns200AndRevokesSessions() throws Exception {
        // changePassword 为 void，mock 默认不抛异常即成功路径；controller 成功后调 session.revokeAll。
        mvc.perform(changePwd("oldPwd123", "brandNewPwd1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_OK));
        verify(session, times(1)).revokeAll(PERSON_ID);
    }
}
