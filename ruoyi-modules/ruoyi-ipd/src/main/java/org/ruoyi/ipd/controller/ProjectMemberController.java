package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ProjectMemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P2-4.1 项目成员绑定与评级快照（/api/v1/projects/{projectId}/members）。
 *
 * <p>绑定发起沿用组队口径（requireProjectCreator：市场PM/组长/超管，页08 研发PM 不可发起）；
 * 快照锁定与幂等规则见 {@link ProjectMemberService}。
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService memberService;
    private final IpdPermission permission;

    /** P2-4.2：approvalRef 可选——绑第 threshold 个项目时必填（服务端强制，不信任前端提示）。 */
    public record BindRequest(@NotNull Long personId, @NotBlank String role, String approvalRef) { }

    public record MemberView(String id, String projectId, String personId, String role,
                             String memberType, String approvalRef,
                             String lockedLevel, String lockedAmount, String joinDate,
                             String exitDate, String exitReason, String bonusEligible) {
        public static MemberView from(ProjectMember m) {
            return new MemberView(String.valueOf(m.getId()), String.valueOf(m.getProjectId()),
                String.valueOf(m.getPersonId()), m.getRole(), m.getMemberType(), m.getApprovalRef(),
                m.getLockedLevel(),
                m.getLockedAmount() == null ? null : m.getLockedAmount().toPlainString(),
                m.getJoinDate() == null ? null : m.getJoinDate().toString(),
                m.getExitDate() == null ? null : m.getExitDate().toString(),
                m.getExitReason(), m.getBonusEligible());
        }
    }

    /** 绑定成员（评级快照随绑定原子写入，AC-TEAM-10 角色互斥；AC-TEAM-11 超额须备案）。 */
    @PostMapping
    public ApiV1Response<MemberView> bind(@PathVariable Long projectId,
                                          @Valid @RequestBody BindRequest request) {
        IpdActor actor = permission.requireProjectCreator();
        ProjectMember member = memberService.bindMember(
            projectId, request.personId(), request.role(), request.approvalRef(), actor);
        return ApiV1Response.ok(MemberView.from(member));
    }

    /** 在组成员列表（含锁定快照，供津贴台账 P3-3 取数）。 */
    @GetMapping
    public ApiV1Response<List<MemberView>> list(@PathVariable Long projectId) {
        permission.requireInternal();
        return ApiV1Response.ok(memberService.listActiveMembers(projectId).stream().map(MemberView::from).toList());
    }
}
