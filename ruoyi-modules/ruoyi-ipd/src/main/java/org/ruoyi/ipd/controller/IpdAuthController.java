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
import org.ruoyi.ipd.security.IpdPermission;
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
    private final IpdPermission permission;

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

    /**
     * logout 幂等守卫（[CONSISTENCY-18] 2026-09-06）：
     * 同一 token 重复 logout 不报错；token 已撤销/过期直接返回 ok。
     */
    @PostMapping("/logout")
    public ApiV1Response<Void> logout() {
        if (session.tokenValue() != null) {
            session.logout();
        }
        return ApiV1Response.ok();
    }

    /**
     * refresh 并发守卫（[CONSISTENCY-18] 2026-09-06）：
     * 同一用户多次 refresh 不应导致 currentPerson 抛错——并发时第 2 个请求的 token
     * 已被第 1 个撤销。原实现「currentPerson → logout → login」有竞态；改为先拿 personId
     * 再 logout，logout 后用缓存的 person 而非 currentPerson 发新 token。
     */
    @PostMapping("/refresh")
    public ApiV1Response<LoginView> refresh() {
        // 关键：先缓存 Person，logout 后用缓存（避免 currentPerson 抛 NotLoginException）
        Person person = session.currentPerson();
        String oldToken = session.tokenValue();
        // 撤销当前 token（幂等——若已撤销 session.logout 不抛）
        if (oldToken != null) {
            session.logout();
        }
        String newToken = session.login(person);
        return ApiV1Response.ok(new LoginView(newToken, "Bearer", session.timeout(),
            authService.scopeOf(person).name(), "1".equals(person.getMustChangePwd()), PersonView.from(person)));
    }

    /**
     * W5-E-2.1 IDOR 修复（最高危 P0）：调用 {@code permission.requireInternalEvenIfPasswordScope()}
     * 拿当前会话 actor 后传入 service，service 层校验 actor.id == personId 才放行——
     * 任何登录用户都不能通过任意 personId 改别人密码。SUPER_ADMIN 运维豁免由 service 内判定。
     *
     * <p>[SEC-AUTH-DEADLOCK] 2026-09-07 真端到端修复（叠 W5-E-2.1 兄弟在途）：
     * 不可用 {@code requireInternal()}——first-login 账号 scope=PASSWORD_CHANGE_REQUIRED
     * 会被 20003 拦死形成死锁（要 FULL 必须先改密，要改密必须 FULL）。
     * 改用专属放宽方法：仅验 token + 内部角色，scope 限制下放到 service 层 IDOR 守卫。
     */
    @PostMapping("/change-password")
    public ApiV1Response<Void> password(@Valid @RequestBody PasswordRequest request) {
        IpdActor actor = permission.requireInternalEvenIfPasswordScope();
        Person person = session.currentPerson();
        try {
            authService.changePassword(actor, person.getId(), request.currentPassword(), request.newPassword());
        } catch (IpdAuthInputException ex) {
            // 失败审计独立事务落库（REQUIRES_NEW），不与业务事务耦合 → 不被回滚
            if (ex.getReason() == IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT) {
                auditAttempt.record(actor,
                    AuditAttemptService.Outcome.FAILURE, "PASSWORD_CHANGE_REJECTED",
                    "persons", person.getId(), "原密码错误");
            }
            throw ex;
        }
        session.revokeAll(person.getId());
        return ApiV1Response.ok();
    }
}
