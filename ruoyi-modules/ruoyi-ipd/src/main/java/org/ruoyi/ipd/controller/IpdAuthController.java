package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.ratelimiter.annotation.RateLimiter;
import org.ruoyi.common.ratelimiter.enums.LimitType;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.service.AuditAttemptService;
import org.ruoyi.ipd.service.IpdAuthInputException;
import org.ruoyi.ipd.service.IpdAuthService;
import org.springframework.web.bind.annotation.*;

/** IPD认证边界；请求只接受凭据，不接受人员ID、角色或scope。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class IpdAuthController {
    private final IpdAuthService authService;
    private final IpdAuthSession session;
    private final AuditAttemptService auditAttempt;

    public record LoginRequest(@NotBlank @Size(max = 64) String username,
                               @NotBlank @Size(max = 72) String password) { }
    public record PasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
                                  @NotBlank @Size(min = 8, max = 72) String newPassword) { }
    public record PersonView(String id, String name, String username, String personType,
                             String groupId, String accountStatus) {
        public static PersonView from(Person p) {
            return new PersonView(String.valueOf(p.getId()), p.getName(), p.getUsername(), p.getPersonType(),
                p.getGroupId() == null ? null : String.valueOf(p.getGroupId()), p.getAccountStatus());
        }
    }
    public record LoginView(String token, String tokenType, long expiresIn, String scope,
                            boolean mustChangePwd, PersonView person) { }
    public record MeView(PersonView person, String scope, boolean mustChangePwd) { }

    /** 防爆破限流：同IP同账号60秒最多5次登录尝试 */
    @RateLimiter(key = "#{#request.username()}", time = 60, count = 5, limitType = LimitType.IP,
        message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public ApiV1Response<LoginView> login(@Valid @RequestBody LoginRequest request) {
        IpdAuthService.LoginResult result = authService.login(request.username(), request.password());
        String token = session.login(result.person());
        return ApiV1Response.ok(new LoginView(token, "Bearer", session.timeout(), result.scope().name(),
            result.mustChangePwd(), PersonView.from(result.person())));
    }

    @GetMapping("/me")
    public ApiV1Response<MeView> me() {
        Person person = session.currentPerson();
        return ApiV1Response.ok(new MeView(PersonView.from(person), authService.scopeOf(person).name(),
            "1".equals(person.getMustChangePwd())));
    }

    @PostMapping("/logout")
    public ApiV1Response<Void> logout() {
        session.logout();
        return ApiV1Response.ok();
    }

    /** P0-7.3：会话轮换——发新 token 撤销旧 token；旧 token 立即失效。 */
    @PostMapping("/refresh")
    public ApiV1Response<LoginView> refresh() {
        Person person = session.currentPerson();
        // 撤销当前 token 后立刻发新 token：旧 token 此刻已不可用
        String oldToken = session.tokenValue();
        session.logout();
        String newToken = session.login(person);
        return ApiV1Response.ok(new LoginView(newToken, "Bearer", session.timeout(),
            authService.scopeOf(person).name(), "1".equals(person.getMustChangePwd()), PersonView.from(person)));
    }

    @PostMapping("/change-password")
    public ApiV1Response<Void> password(@Valid @RequestBody PasswordRequest request) {
        Person person = session.currentPerson();
        try {
            authService.changePassword(person.getId(), request.currentPassword(), request.newPassword());
        } catch (IpdAuthInputException ex) {
            // 失败审计独立事务落库（REQUIRES_NEW），不与业务事务耦合 → 不被回滚
            if (ex.getReason() == IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT) {
                auditAttempt.record(new IpdActor(person.getId(), person.getName(), person.getPersonType(), person.getGroupId()),
                    AuditAttemptService.Outcome.FAILURE, "PASSWORD_CHANGE_REJECTED",
                    "persons", person.getId(), "原密码错误");
            }
            throw ex;
        }
        session.revokeAll(person.getId());
        return ApiV1Response.ok();
    }
}
