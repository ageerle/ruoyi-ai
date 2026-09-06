package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P2-6.1 需求变更单服务（BR-GATE-07，AC-REQ-08）。
 *
 * <p>需求变更双签否决对象。市场PM 与研发PM 双签；任一否决 ⇒ 整体 REJECTED；
 * 双 APPROVE ⇒ APPROVED；原需求（requirements）状态由 P2-6.2 在变更单通过后回写。
 *
 * <p>状态机：
 * <pre>
 *   DRAFT ──submit──&gt; PENDING_SIGN ──approve(双)──&gt; APPROVED
 *                              │
 *                              └──reject(任一方)──&gt; REJECTED
 * </pre>
 *
 * <p>影响评估快照：beforeSnapshot / afterSnapshot 在创建时即冻结（AC-REQ-08 引述需求/PRD
 * 版本快照要求），后续修改不影响在途评审——与 P2-5.1 Gate 要素快照同类处理。
 *
 * <p>未闭环变更可查：listByProject + status=PENDING_SIGN 过滤供 P2-6.2 阶段门禁消费
 * （AC-GATE-11 跳阶拒绝）。
 */
@Service
@RequiredArgsConstructor
public class RequirementChangeService {

    /** 状态常量 */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_SIGN = "PENDING_SIGN";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    /** 双签角色集合（与 GateReviewService 对齐） */
    private static final Set<String> SIGNER_ROLES = Set.of("MARKET_PM", "RD_PM");
    /** 决策集合 */
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REJECT");

    private final RequirementChangeMapper requirementChangeMapper;
    private final RequirementMapper requirementMapper;
    private final AuditLogService auditLogService;

    /**
     * 创建变更单（草稿状态）。AC-REQ-08：需求转需求变更单 ⇒ 可生成。
     * 引用原需求 ID，影响评估快照由调用方提供（beforeSnapshot / afterSnapshot JSON），
     * 包含范围/成本/时限/质量四维度。
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementChange create(RequirementChange change, IpdActor actor) {
        requireInternal(actor);
        requireNonBlankField(change.getChangeType(), "changeType 不能为空");
        requireNonBlankField(change.getReason(), "变更原因不能为空");
        if (change.getRequirementId() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        // 引用原需求必须存在（用于冻结历史版本号）
        Requirement requirement = requirementMapper.selectById(change.getRequirementId());
        if (requirement == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (change.getProjectId() == null) {
            change.setProjectId(requirement.getProjectId());
        }

        change.setStatus(STATUS_DRAFT);
        change.setCreateTime(new Date());
        change.setCreateBy(actor.id());
        requirementChangeMapper.insert(change);
        audit(actor, change, "REQ_CHANGE_CREATE",
            "DRAFT 创建，影响评估四维度快照已冻结");
        return change;
    }

    /**
     * 提交双签：DRAFT ⇒ PENDING_SIGN。
     * 提交校验影响评估四维度（范围/成本/时限/质量）全部非空——任一缺失拒绝。
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementChange submit(Long id, IpdActor actor) {
        requireInternal(actor);
        RequirementChange change = requireById(id);
        requireOwnerOrAdmin(change, actor);
        if (!STATUS_DRAFT.equals(change.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        if (isBlank(change.getBeforeSnapshot()) || isBlank(change.getAfterSnapshot())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        change.setStatus(STATUS_PENDING_SIGN);
        change.setUpdateTime(new Date());
        requirementChangeMapper.updateById(change);
        audit(actor, change, "REQ_CHANGE_SUBMIT",
            "进入双签队列 PENDING_SIGN");
        return change;
    }

    /**
     * 双签：市场PM + 研发PM 双方均 APPROVE ⇒ APPROVED；
     * 任一 REJECT ⇒ REJECTED。签名记录聚合在 signatures 字段（MARKET_PM=APPROVE;RD_PM=APPROVE）。
     *
     * <p>BR-GATE-07 关联：PENDING_SIGN 阶段冻结后，原 requirements 行不得再被直接编辑；
     * 状态联动由 P2-6.2 的 RequirementChangeStateWriter 在 APPROVED 时回写。
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementChange sign(Long id, String decision, String opinion, IpdActor actor) {
        requireInternal(actor);
        if (!SIGNER_ROLES.contains(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN);
        }
        if (decision == null || !DECISIONS.contains(decision)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        RequirementChange change = requireById(id);
        if (!STATUS_PENDING_SIGN.equals(change.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        // 同方重复签署拒绝（signatures 字段聚合签名）
        String existing = change.getSignatures() == null ? "" : change.getSignatures();
        String signature = actor.role() + "=" + decision;
        if (existing.contains(signature)) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        String joined = existing.isBlank() ? signature : (existing + ";" + signature);
        change.setSignatures(joined);
        change.setUpdateTime(new Date());

        // 任何 REJECT ⇒ 整体 REJECTED（无需等另一方）
        if ("REJECT".equals(decision)) {
            change.setStatus(STATUS_REJECTED);
            requirementChangeMapper.updateById(change);
            audit(actor, change, "REQ_CHANGE_REJECT",
                "单方 REJECT，整体 REJECTED；意见：" + opinion);
            return change;
        }

        // APPROVE：累计双方签名，齐签 ⇒ APPROVED
        boolean hasMarket = joined.contains("MARKET_PM=APPROVE");
        boolean hasRd = joined.contains("RD_PM=APPROVE");
        if (hasMarket && hasRd) {
            change.setStatus(STATUS_APPROVED);
            requirementChangeMapper.updateById(change);
            audit(actor, change, "REQ_CHANGE_APPROVE",
                "双签 APPROVE，变更单生效；待 P2-6.2 回写需求池");
            return change;
        }
        // 单方 APPROVE：保持 PENDING_SIGN，等待另一方
        requirementChangeMapper.updateById(change);
        audit(actor, change, "REQ_CHANGE_PARTIAL_SIGN",
            actor.role() + " 已 APPROVE，等待另一方");
        return change;
    }

    /** 详情：返回含影响快照的双签进度视图。 */
    public Map<String, Object> detail(Long id) {
        RequirementChange change = requireById(id);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", change.getId());
        view.put("requirementId", change.getRequirementId());
        view.put("projectId", change.getProjectId());
        view.put("changeType", change.getChangeType());
        view.put("beforeSnapshot", change.getBeforeSnapshot());
        view.put("afterSnapshot", change.getAfterSnapshot());
        view.put("reason", change.getReason());
        view.put("status", change.getStatus());
        view.put("signatures", change.getSignatures());
        view.put("createTime", change.getCreateTime() == null ? null
            : change.getCreateTime().toString());
        view.put("updateTime", change.getUpdateTime() == null ? null
            : change.getUpdateTime().toString());
        return view;
    }

    /** 项目内变更单列表（按状态过滤供 P2-6.2 阶段门禁未闭环检测）。 */
    public IPage<RequirementChange> listByProject(int pageNo, int pageSize,
                                                    Long projectId, String status) {
        LambdaQueryWrapper<RequirementChange> q = new LambdaQueryWrapper<>();
        if (projectId != null) {
            q.eq(RequirementChange::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            q.eq(RequirementChange::getStatus, status);
        }
        q.orderByDesc(RequirementChange::getCreateTime);
        return requirementChangeMapper.selectPage(new Page<>(pageNo, pageSize), q);
    }

    /**
     * 列出项目内未闭环的变更单（status ∈ {DRAFT, PENDING_SIGN}）。
     * 供 P2-6.2 阶段门禁在跳阶前调用，AC-GATE-11 拒绝语义。
     */
    public List<RequirementChange> listOpenByProject(Long projectId) {
        return requirementChangeMapper.selectList(new LambdaQueryWrapper<RequirementChange>()
            .eq(projectId != null, RequirementChange::getProjectId, projectId)
            .in(RequirementChange::getStatus, List.of(STATUS_DRAFT, STATUS_PENDING_SIGN))
            .orderByDesc(RequirementChange::getCreateTime));
    }

    /** 是否存在项目内未闭环变更单（供阶段门禁快速拦截，AC-GATE-11）。 */
    public boolean hasOpenChange(Long projectId) {
        if (projectId == null) {
            return false;
        }
        Long count = requirementChangeMapper.selectCount(new LambdaQueryWrapper<RequirementChange>()
            .eq(RequirementChange::getProjectId, projectId)
            .in(RequirementChange::getStatus, List.of(STATUS_DRAFT, STATUS_PENDING_SIGN)));
        return count != null && count > 0;
    }

    private RequirementChange requireById(Long id) {
        if (id == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        RequirementChange change = requirementChangeMapper.selectById(id);
        if (change == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return change;
    }

    private void requireInternal(IpdActor actor) {
        if (actor == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);
        }
    }

    private void requireOwnerOrAdmin(RequirementChange change, IpdActor actor) {
        boolean isAdmin = "SUPER_ADMIN".equals(actor.role());
        boolean isCreator = actor.id() != null && actor.id().equals(change.getCreateBy());
        if (!isAdmin && !isCreator) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN);
        }
    }

    private void requireNonBlankField(String v, String msg) {
        if (v == null || v.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void audit(IpdActor actor, RequirementChange change, String action, String reason) {
        auditLogService.append(AuditLog.builder()
            .action(action)
            .entityType("requirement_changes")
            .entityId(change.getId())
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .reason(reason)
            .createTime(new Date())
            .build());
    }
}
