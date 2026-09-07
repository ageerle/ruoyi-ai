package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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

    private final RequirementMapper requirementMapper;
    private final RequirementChangeMapper requirementChangeMapper;
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
     * 需求七态迁移入口。事务包裹；非法迁移抛 STATE_CONFLICT；
     * ACCEPTED → CHANGED 自动创建 RequirementChange DRAFT。
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
        if (requirementId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (toState == null || !ALL_STATES.contains(toState)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "目标状态非法：" + toState);
        }
        if (actor == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);
        }
        Requirement requirement = requirementMapper.selectById(requirementId);
        if (requirement == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        String fromState = requirement.getStatus();
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