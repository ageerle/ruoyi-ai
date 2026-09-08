package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdIdorGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * P4-1.4 需求七态状态机 (BR-REQ-05; AC-REQ-05/06/07/08)
 *
 * <p>状态图（与开发说明书 §P4 一致）：
 * <pre>
 *   DRAFT ──submit──&gt; SUBMITTED ──route──&gt; ROUTED ──accept──&gt; ACCEPTED ──change──&gt; CHANGED ──close──&gt; CLOSED
 *                            │                  │                                       │
 *                            └──reject──&gt; REJECTED                                     └──reject──&gt; REJECTED
 * </pre>
 *
 * <p>关键约束：
 * <ul>
 *   <li>ACCEPTED → CHANGED 自动创建 RequirementChange 草稿（DRAFT），关联原 requirementId</li>
 *   <li>CHANGED → CLOSED 必须先有 RequirementChange PENDING_SIGN/APPROVED（stage guard 由调用方保证）</li>
 *   <li>REJECTED / CLOSED 为终态（不可再迁移；除终态收敛外）</li>
 *   <li>越界（如 SUBMITTED → ACCEPTED 直跳）抛 STATE_CONFLICT，拒绝</li>
 * </ul>
 *
 * <p><b>安全加固（commit 9228ecae-followup2 安全审查响应）</b>：transition() 入口插 IpdIdorGuard 鉴权链：
 * <ol>
 *   <li>{@code requireAuthenticated(actor)} —— 服务层兜底，防 Controller 注解漂移</li>
 *   <li>{@code requireProjectMemberOrSuperAdmin(actor, projectId, ...)} —— 跨租户守卫 + 项目成员守卫</li>
 *   <li>{@code assertSameGroupIpd(actor, mainGroupId)} —— 横向组一致性</li>
 *   <li>{@code requireRoleForTransition(from, to, actor)} —— 每条迁移的角色门（AC-REQ-05/06）</li>
 * </ol>
 *
 * <p>本类是轻量级「七态」实现，不动现有 requirements.status 字面量词表（已含 SUBMITTED/ACCEPTED/EVALUATING/SCHEDULED/PROCESSING/CLOSED/ARCHIVED/WITHDRAWN），
 * 而是用 {@link StateMachineGuard} 隔离一个 entityType="requirement_v2" 的规则表，避免覆盖旧业务。
 *
 * <p>本卡只关心「状态机语义」；外部 controller / service 调 transition() 完成迁移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementStateMachine {

    /** 七态常量（与卡面验收 P4-1.4 对齐）。 */
    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_SUBMITTED = "SUBMITTED";
    public static final String ST_ROUTED = "ROUTED";
    public static final String ST_ACCEPTED = "ACCEPTED";
    public static final String ST_REJECTED = "REJECTED";
    public static final String ST_CHANGED = "CHANGED";
    public static final String ST_CLOSED = "CLOSED";

    /** 全部合法状态词表（防止调用方传错字面量）。 */
    public static final Set<String> ALL_STATES = Set.of(
        ST_DRAFT, ST_SUBMITTED, ST_ROUTED, ST_ACCEPTED, ST_REJECTED, ST_CHANGED, ST_CLOSED);

    /** 终态集合：不可再迁移。 */
    public static final Set<String> TERMINAL_STATES = Set.of(ST_REJECTED, ST_CLOSED);

    /** 迁移图：fromState → 允许的 toState 集合。越界迁移一律 STATE_CONFLICT。 */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
        ST_DRAFT,    Set.of(ST_SUBMITTED, ST_REJECTED),
        ST_SUBMITTED, Set.of(ST_ROUTED, ST_REJECTED),
        ST_ROUTED,   Set.of(ST_ACCEPTED, ST_REJECTED),
        ST_ACCEPTED, Set.of(ST_CHANGED, ST_REJECTED),
        ST_CHANGED,  Set.of(ST_CLOSED, ST_REJECTED)
        // REJECTED / CLOSED 终态不放任何出口
    );

    /**
     * 角色门（每条迁移的合法角色集）。AC-REQ-05/06/07/08 + BR-REQ-05 业务侧：
     * <ul>
     *   <li>DRAFT → SUBMITTED：申请人（GUEST 提交）/ GROUP_LEADER（代提）</li>
     *   <li>DRAFT → REJECTED：申请人撤回 / GROUP_LEADER 驳回</li>
     *   <li>SUBMITTED → ROUTED：GROUP_LEADER / SUPER_ADMIN（自动派单）</li>
     *   <li>SUBMITTED → REJECTED：GROUP_LEADER / SUPER_ADMIN（驳回）</li>
     *   <li>ROUTED → ACCEPTED：双 PM（MARKET_PM / RD_PM）/ GROUP_LEADER / SUPER_ADMIN</li>
     *   <li>ROUTED → REJECTED：双 PM / GROUP_LEADER / SUPER_ADMIN</li>
     *   <li>ACCEPTED → CHANGED：双 PM（采纳后变更）/ GROUP_LEADER / SUPER_ADMIN</li>
     *   <li>ACCEPTED → REJECTED：双 PM / GROUP_LEADER / SUPER_ADMIN</li>
     *   <li>CHANGED → CLOSED：GROUP_LEADER / SUPER_ADMIN（项目级关闭）</li>
     *   <li>CHANGED → REJECTED：SUPER_ADMIN（仅超管可撤变更）</li>
    * </ul>
     * SUPER_ADMIN 在 IpdIdorGuard 守卫层已通过 requireProjectMemberOrSuperAdmin 兜底，故此处不再重复枚举。
     */
    private static final Map<String, Set<String>> TRANSITION_ROLES = buildTransitionRoles();

    private static Map<String, Set<String>> buildTransitionRoles() {
        Map<String, Set<String>> m = new HashMap<>();
        m.put(ST_DRAFT + "->" + ST_SUBMITTED,    Set.of("GUEST", "MEMBER", "GROUP_LEADER"));
        m.put(ST_DRAFT + "->" + ST_REJECTED,     Set.of("GUEST", "MEMBER", "GROUP_LEADER"));
        m.put(ST_SUBMITTED + "->" + ST_ROUTED,   Set.of("GROUP_LEADER"));
        m.put(ST_SUBMITTED + "->" + ST_REJECTED, Set.of("GROUP_LEADER"));
        m.put(ST_ROUTED + "->" + ST_ACCEPTED,    Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER"));
        m.put(ST_ROUTED + "->" + ST_REJECTED,    Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER"));
        m.put(ST_ACCEPTED + "->" + ST_CHANGED,   Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER"));
        m.put(ST_ACCEPTED + "->" + ST_REJECTED,  Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER"));
        m.put(ST_CHANGED + "->" + ST_CLOSED,     Set.of("GROUP_LEADER"));
        m.put(ST_CHANGED + "->" + ST_REJECTED,   Set.of("GROUP_LEADER"));  // 仅组长/超管可撤销
        return m;
    }

    private final RequirementMapper requirementMapper;
    private final RequirementChangeMapper requirementChangeMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final AuditLogService auditLogService;

    /**
     * 查询 (from → to) 是否为合法迁移。
     *
     * @param from 源状态；null 视为 DRAFT 起点
     * @param to   目标状态；必须非 null
     * @return true=合法；false=非法或终态
     */
    public boolean isAllowed(String from, String to) {
        if (to == null || !ALL_STATES.contains(to)) {
            return false;
        }
        String src = from == null ? ST_DRAFT : from;
        if (!ALL_STATES.contains(src)) {
            return false;
        }
        if (TERMINAL_STATES.contains(src)) {
            return false;
        }
        return ALLOWED.getOrDefault(src, Set.of()).contains(to);
    }

    /**
     * 查询 (from → to) 的合法角色集（含 SUPER_ADMIN 兜底）。
     *
     * @param from 源状态
     * @param to   目标状态
     * @return 合法角色集；空集表示该迁移禁止
     */
    public Set<String> allowedRolesFor(String from, String to) {
        Set<String> base = TRANSITION_ROLES.getOrDefault(from + "->" + to, Set.of());
        Set<String> withSuperAdmin = new HashSet<>(base);
        withSuperAdmin.add("SUPER_ADMIN");
        return withSuperAdmin;
    }

    /**
     * 查询 (from → to) 的角色门是否通过（actor.role() ∈ 合法集 ∪ {SUPER_ADMIN}）。
     *
     * @param from 源状态
     * @param to   目标状态
     * @param actor 操作人
     * @throws IpdBusinessException FORBIDDEN 当 actor.role() 不在合法集
     */
    private void requireRoleForTransition(String from, String to, IpdActor actor) {
        Set<String> allowed = allowedRolesFor(from, to);
        if (allowed.isEmpty() || (!allowed.contains(actor.role()) && !"SUPER_ADMIN".equals(actor.role()))) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN,
                String.format("需求状态机角色门拒绝：from=%s, to=%s, actorRole=%s, allowed=%s",
                    from, to, actor.role(), allowed));
        }
    }

    /**
     * 需求七态迁移入口。事务包裹；非法迁移抛 STATE_CONFLICT；
     * ACCEPTED → CHANGED 自动创建 RequirementChange DRAFT。
     *
     * <p><b>鉴权链（commit 9228ecae-followup2 加固）</b>：
     * <ol>
     *   <li>requireAuthenticated —— actor 必填</li>
     *   <li>requireProjectMemberOrSuperAdmin —— 项目成员守卫 + 跨租户守卫</li>
     *   <li>assertSameGroupIpd —— 横向组一致性</li>
     *   <li>requireRoleForTransition —— 角色门</li>
     *   <li>isAllowed —— 状态机守卫</li>
     * </ol>
     *
     * @param requirementId 需求 ID
     * @param toState       目标状态
     * @param reason        迁移原因（审计 detail 用）
     * @param actor         操作人
     * @return 迁移后的 Requirement
     * @throws IpdBusinessException 40001/50002 系列错误
     */
    @Transactional(rollbackFor = Exception.class)
    public Requirement transition(Long requirementId, String toState, String reason, IpdActor actor) {
        // 守卫 0：actor 必填（与下方 requireAuthenticated 等价但更早暴露，便于诊断）
        if (actor == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);
        }
        // 守卫 1：actor 鉴权（service 层兜底，防 Controller 注解漂移）
        IpdIdorGuard.requireAuthenticated(actor);

        if (requirementId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (toState == null || !ALL_STATES.contains(toState)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "目标状态非法：" + toState);
        }
        Requirement requirement = requirementMapper.selectById(requirementId);
        if (requirement == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }

        // 守卫 2：项目成员 + 跨租户守卫（SUPER_ADMIN 绕过）
        IpdIdorGuard.requireProjectMemberOrSuperAdmin(
            actor, requirement.getProjectId(), projectMemberMapper, projectMapper);

        // 守卫 3：横向组一致性（拉 project 取 mainGroupId）
        Project project = projectMapper.selectById(requirement.getProjectId());
        if (project == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "需求所属项目不存在");
        }
        IpdIdorGuard.assertSameGroupIpd(actor, project.getMainGroupId());

        String fromState = requirement.getStatus();

        // 守卫 4：角色门（每条迁移的合法角色集 + SUPER_ADMIN 兜底）
        requireRoleForTransition(fromState, toState, actor);

        // 守卫 5：状态机守卫（迁移图）
        if (!isAllowed(fromState, toState)) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                String.format("需求状态机非法迁移：from=%s, to=%s（守卫层未登记该迁移）",
                    fromState, toState));
        }
        // ACCEPTED → CHANGED：自动创建 RequirementChange DRAFT（与原 requirementId 关联）
        if (ST_ACCEPTED.equals(fromState) && ST_CHANGED.equals(toState)) {
            RequirementChange change = new RequirementChange();
            change.setRequirementId(requirementId);
            change.setProjectId(requirement.getProjectId());
            change.setChangeType("ADOPTION_MODIFIED");
            change.setReason(reason == null ? "需求采纳后变更" : reason);
            change.setBeforeSnapshot("{\"status\":\"ACCEPTED\"}");
            change.setAfterSnapshot("{\"status\":\"CHANGED\"}");
            change.setStatus(RequirementChangeService.STATUS_DRAFT);
            change.setCreateTime(new Date());
            change.setCreateBy(actor.id());
            requirementChangeMapper.insert(change);
            audit(actor, requirementId, "REQUIREMENT_TRANSITION",
                "from=" + fromState + ",to=" + toState + ",changeId=" + change.getId() + ";reason=" + reason);
        } else {
            audit(actor, requirementId, "REQUIREMENT_TRANSITION",
                "from=" + fromState + ",to=" + toState + ";reason=" + reason);
        }
        requirement.setStatus(toState);
        requirement.setUpdateTime(new Date());
        requirementMapper.updateById(requirement);
        return requirement;
    }

    private void audit(IpdActor actor, Long requirementId, String action, String reason) {
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action(action)
            .entityType("requirement_v2")
            .entityId(requirementId)
            .reason(reason)
            .createTime(new Date())
            .build());
    }
}