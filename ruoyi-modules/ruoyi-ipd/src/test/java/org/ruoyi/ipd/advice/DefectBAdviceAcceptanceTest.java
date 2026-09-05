package org.ruoyi.ipd.advice;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.controller.AuditLogController;
import org.ruoyi.ipd.controller.IpdAuthController;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.service.AuditAttemptService;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.IpdAuthService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 缺陷B（QA-03 DEF-3 同源）验收：{@link IpdServiceExceptionAdvice} 在<b>真实 HTTP 层</b>
 * （MockMvc standaloneSetup 走完整 Spring MVC 分发 → 控制器 → 全局异常链 → JSON 响应体）
 * 把「非白名单控制器」的身份/角色拒绝忠实映射为对应 4xx，而非落入 {@code Exception.class} 兜底的
 * 500 + code 90001。
 *
 * <p>根因：{@code IpdPermissionExceptionHandler} 以 {@code assignableTypes} 白名单只覆盖 7 个控制器，
 * AuditLog / SystemConfig / Coefficient / LaunchDate 不在其中；这些控制器的
 * {@code requireInternal()/requireAdmin()} 抛出的 {@link IpdPermissionException}、以及
 * {@code @SaCheckPermission} 触发的 {@link NotPermissionException}/{@link NotRoleException}，
 * 修复前统统落兜底 → 500/90001。实测 live {@code GET /api/v1/audit-logs/scope} 无有效身份 → 90001
 * （见第十三轮第二方 QA 报告 §3 STALE 探针）。
 *
 * <p>本测试以真实 {@link AuditLogController}（非白名单）为受试者、注入真实生产 advice，断言客户端
 * 真正收到的状态码与业务码，堵住「handler 就位即认为关卡通过」的假绿：
 * <ul>
 *   <li>缺陷B① IpdPermissionException(401,UNAUTHORIZED) → HTTP 401 + code 20001</li>
 *   <li>缺陷B② IpdPermissionException(403,FORBIDDEN)    → HTTP 403 + code 30001</li>
 *   <li>缺陷B③ NotPermissionException                    → HTTP 403 + code 30001</li>
 *   <li>缺陷B④ NotRoleException                          → HTTP 403 + code 30001</li>
 *   <li>DEF-2  请求体不可读 HttpMessageNotReadable        → HTTP 400 + code 10001</li>
 * </ul>
 *
 * <p>沿用 {@code Api03AcceptanceTest} 约定：plain {@code mock()} 无 MockitoExtension，故 setup 内
 * 共享桩在个别用例未触达时不会报 UnnecessaryStubbing。仅注册 {@link IpdServiceExceptionAdvice}
 * 一个 advice（不含 IpdPermissionExceptionHandler），隔离验证本次新增全局 handler 自身的映射契约；
 * 生产链中 IpdPermissionExceptionHandler(HIGHEST) 对这些异常无 handler，同样落到本 advice，结果一致。
 */
@Tag("dev")
class DefectBAdviceAcceptanceTest {

    private static final String SCOPE = "/api/v1/audit-logs/scope";
    private static final String LIST = "/api/v1/audit-logs";
    private static final String CHANGE_PWD = "/api/v1/auth/change-password";

    /** UNAUTHORIZED 业务码；断言用字面量对齐 P064/Api03AcceptanceTest 风格。 */
    private static final int CODE_UNAUTHORIZED = 20001;
    private static final int CODE_FORBIDDEN = 30001;
    private static final int CODE_PARAM_INVALID = 10001;

    private IpdPermission ipdPermission;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ipdPermission = mock(IpdPermission.class);
        PersonMapper personMapper = mock(PersonMapper.class);
        // 受试者 = 真实 AuditLogController（不在 IpdPermissionExceptionHandler.assignableTypes 白名单）
        // + 真实生产 advice；证明缺陷B 全局 handler 对非白名单控制器真的生效（非空洞 mock）。
        mvc = MockMvcBuilders
            .standaloneSetup(new AuditLogController(auditLogMapper, auditLogService, ipdPermission, personMapper))
            .setControllerAdvice(new IpdServiceExceptionAdvice())
            .build();
    }

    @Test
    @DisplayName("缺陷B①：/scope requireInternal 抛 IpdPermissionException(401) → HTTP 401 + code 20001（修复前落兜底 500/90001）")
    void scopeUnauthorizedMapsTo401() throws Exception {
        // 忠实复刻 IpdPermission.requireInternal() 在 NotLogin 时抛 IpdPermissionException(401,UNAUTHORIZED) 的真实路径。
        when(ipdPermission.requireInternal())
            .thenThrow(new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED));

        mvc.perform(get(SCOPE))
            .andExpect(status().isUnauthorized())                    // 修复前会是 500
            .andExpect(jsonPath("$.code").value(CODE_UNAUTHORIZED));  // 修复前会是 90001
    }

    @Test
    @DisplayName("缺陷B②：/list requireAdmin 抛 IpdPermissionException(403,FORBIDDEN) → HTTP 403 + code 30001")
    void listForbiddenMapsTo403() throws Exception {
        when(ipdPermission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));

        mvc.perform(get(LIST))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(CODE_FORBIDDEN));
    }

    @Test
    @DisplayName("缺陷B③：@SaCheckPermission 拒绝（NotPermissionException）在非白名单控制器 → HTTP 403 + code 30001（修复前 500/90001）")
    void notPermissionMapsTo403() throws Exception {
        // 模拟 sa-token AOP 对 ipd:audit-log:list 的拒绝；standalone 下注解不织入，故由协作对象直接抛出，
        // 走真实 MVC 分发验证 advice 的 NotPermissionException handler。
        when(ipdPermission.requireInternal())
            .thenThrow(new NotPermissionException("ipd:audit-log:list"));

        mvc.perform(get(SCOPE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(CODE_FORBIDDEN));
    }

    @Test
    @DisplayName("缺陷B④：NotRoleException 在非白名单控制器 → HTTP 403 + code 30001")
    void notRoleMapsTo403() throws Exception {
        when(ipdPermission.requireInternal())
            .thenThrow(new NotRoleException("SUPER_ADMIN"));

        mvc.perform(get(SCOPE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(CODE_FORBIDDEN));
    }

    @Test
    @DisplayName("DEF-2：请求体不可读（HttpMessageNotReadable）→ HTTP 400 + code 10001（修复前落兜底 500/90001）")
    void unreadableBodyMapsTo400() throws Exception {
        // 用真实 IpdAuthController 的改密 POST（带 @RequestBody）触发反序列化失败；AuditLogController 无请求体端点。
        // IpdPermissionExceptionHandler 无此异常 handler，生产链同样落到本 advice 的 handleNotReadable → 400。
        MockMvc authMvc = MockMvcBuilders
            .standaloneSetup(new IpdAuthController(
                mock(IpdAuthService.class), mock(IpdAuthSession.class), mock(AuditAttemptService.class)))
            .setControllerAdvice(new IpdServiceExceptionAdvice())
            .build();

        authMvc.perform(post(CHANGE_PWD)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ 这不是合法 JSON "))                        // 反序列化失败 → HttpMessageNotReadableException
            .andExpect(status().isBadRequest())                       // 修复前会是 500
            .andExpect(jsonPath("$.code").value(CODE_PARAM_INVALID));  // 修复前会是 90001
    }
}
