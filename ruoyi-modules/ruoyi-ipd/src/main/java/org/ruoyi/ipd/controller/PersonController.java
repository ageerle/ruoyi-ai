package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.PersonService;
import org.springframework.web.bind.annotation.*;

/**
 * P2-1.3 人员账户状态联动（/api/v1/persons；页13）。
 *
 * <p>三个端点：
 * <ul>
 *   <li>{@code POST /{id}/resign} 离职冻结（AC-USER-08）</li>
 *   <li>{@code POST /{id}/rehire} 复职（AC-USER-09）</li>
 *   <li>{@code POST /{id}/wecom/unbind} 企微解绑联动（AC-USER-10）</li>
 * </ul>
 *
 * <p>权限：resign 由本人或 HR 角色（SUPER_ADMIN/MARKET_PM/GROUP_LEADER 中可代管人事的角色）触发；
 * rehire + wecomUnbind 限 HR（requireAdmin 或特定角色）。
 */
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final IpdPermission permission;

    /** 离职冻结请求（reason 必填）。 */
    public record ResignRequest(@NotBlank String reason) { }

    /** 复职请求（note 选填）。 */
    public record RehireRequest(String note) { }

    /** 企微解绑请求（reason 必填）。 */
    public record UnbindRequest(@NotBlank String reason) { }

    /** 离职响应（待移交项目数 + 幂等标记）。 */
    public record ResignView(boolean idempotent, long pendingProjects, String message) {
        public static ResignView from(PersonService.ResignResult r) {
            return new ResignView(r.idempotent(), r.pendingProjects(), r.message());
        }
    }

    /** 复职/解绑响应（人员快照）。 */
    public record PersonView(String id, String name, String employmentStatus, String accountStatus,
                            String wecomUserId) {
        public static PersonView from(Person p) {
            return new PersonView(String.valueOf(p.getId()), p.getName(),
                p.getEmploymentStatus(), p.getAccountStatus(),
                p.getWecomUserId() == null ? null : "***");
        }
    }

    /**
     * 离职冻结（AC-USER-08）。
     * <p>权限：HR 角色（SUPER_ADMIN/GROUP_LEADER）或本人（selfId == personId）。
     */
    @PostMapping("/{id}/resign")
    public ApiV1Response<ResignView> resign(@PathVariable("id") Long personId,
                                           @Valid @RequestBody ResignRequest req) {
        // resign: HR or self (BR-USER-05 + BR-IPD-02 自我管理)
        IpdActor operator = permission.requireInternal();
        if (!isHr(operator) && !operator.id().equals(personId)) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "仅 HR 或本人可触发离职");
        }
        var result = personService.resign(personId, req.reason(), operator);
        return ApiV1Response.ok(ResignView.from(result));
    }

    /**
     * 复职（AC-USER-09）。
     * <p>权限：HR 角色（SUPER_ADMIN/GROUP_LEADER）。
     */
    @PostMapping("/{id}/rehire")
    public ApiV1Response<PersonView> rehire(@PathVariable("id") Long personId,
                                           @Valid @RequestBody RehireRequest req) {
        IpdActor operator = permission.requireLeaderOrAdmin();
        Person p = personService.rehire(personId, req == null ? null : req.note(), operator);
        return ApiV1Response.ok(PersonView.from(p));
    }

    /**
     * 企微解绑联动（AC-USER-10）。
     * <p>权限：HR 角色（SUPER_ADMIN/GROUP_LEADER）。
     */
    @PostMapping("/{id}/wecom/unbind")
    public ApiV1Response<PersonView> wecomUnbind(@PathVariable("id") Long personId,
                                                 @Valid @RequestBody UnbindRequest req) {
        IpdActor operator = permission.requireLeaderOrAdmin();
        Person p = personService.unbindWecom(personId, req.reason(), operator);
        return ApiV1Response.ok(PersonView.from(p));
    }

    private static boolean isHr(IpdActor actor) {
        String role = actor.role();
        return "SUPER_ADMIN".equals(role) || "GROUP_LEADER".equals(role);
    }
}
