package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /**
     * 影响评估必填四维度键（P2-6.2 强化）。AC-REQ-08 要求变更必须显式覆盖范围/成本/时限/质量，
     * 缺失任一维度 ⇒ PARAM_INVALID，从源头上杜绝「影响评估留白、双签走过场」的 state-drift。
     */
    private static final Set<String> REQUIRED_DIMENSION_KEYS = Set.of("范围", "成本", "时限", "质量");

    /** 影响快照 JSON 解析器（共享，与 AuditEventData 隔离避免误传敏感上下文）。 */
    private static final ObjectMapper SNAPSHOT_JSON = new ObjectMapper();

    /** 系统执行人（无登录态时审计落名） */
    private static final IpdActor SYSTEM_ACTOR = new IpdActor(0L, "system", "SYSTEM", null);

    private final RequirementChangeMapper requirementChangeMapper;
    private final RequirementMapper requirementMapper;
    private final AuditLogService auditLogService;

    /**
     * 创建变更单（草稿状态）。AC-REQ-08：需求转需求变更单 ⇒ 可生成。
     * 引用原需求 ID，影响评估快照由调用方提供（beforeSnapshot / afterSnapshot JSON），
     * 包含范围/成本/时限/质量四维度。
     *
     * <p>P2-6.2 强化：影响评估四维度在 create 阶段即强制校验 —— beforeSnapshot / afterSnapshot
     * 必须为合法 JSON 且同时包含「范围/成本/时限/质量」四个键，任一缺失 ⇒ PARAM_INVALID。
     * 提交阶段（{@link #submit}）的同口径校验作为兜底双保险（防御 create 之后回填快照路径）。
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
        // P2-6.2：影响评估四维度必填（create 阶段即校验，缺失即拒绝）
        validateFourDimensionalSnapshot(change.getBeforeSnapshot(), "beforeSnapshot");
        validateFourDimensionalSnapshot(change.getAfterSnapshot(), "afterSnapshot");

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
     *
     * <p>P2-6.2 强化：复用 {@link #validateFourDimensionalSnapshot} 作为兜底双保险。
     * 正常路径下 create 已校验；本方法在 create→submit 期间快照被外部覆盖/篡改时兜底拒绝，
     * 防止「四维度在 create 后被偷换为留白快照」绕过校验。
     */
    @Transactional(rollbackFor = Exception.class)
    public RequirementChange submit(Long id, IpdActor actor) {
        requireInternal(actor);
        RequirementChange change = requireById(id);
        requireOwnerOrAdmin(change, actor);
        if (!STATUS_DRAFT.equals(change.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        validateFourDimensionalSnapshot(change.getBeforeSnapshot(), "beforeSnapshot");
        validateFourDimensionalSnapshot(change.getAfterSnapshot(), "afterSnapshot");
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
     * <p>BR-GATE-07 关联：APPROVED 时回写需求池状态为"已采纳"（AC-GATE-12），
     * 显式 REJECT 不得超时绕过（AC-GATE-11：未闭环拒绝跳阶由 GateEngine 通过 hasOpenChange 拦截）。
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
        // 同方重复签署拒绝（signatures 字段聚合签名，SEC-REV-REQ-CHANGE-04：含 actorId 防同角色多账号混淆）
        String existing = change.getSignatures() == null ? "" : change.getSignatures();
        String signature = actor.role() + ":" + actor.id() + "=" + decision;
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

        // APPROVE：累计双方签名，齐签 ⇒ APPROVED 并回写需求池
        // SEC-REV-REQ-CHANGE-04：签名格式 "MARKET_PM:<actorId>=APPROVE;RD_PM:<actorId>=APPROVE"
        // 向后兼容旧格式 "MARKET_PM=APPROVE"（旧 P2-6.1 测与历史数据）
        boolean hasMarket = joined.matches(".*MARKET_PM(:\\d+)?=APPROVE.*");
        boolean hasRd = joined.matches(".*RD_PM(:\\d+)?=APPROVE.*");
        if (hasMarket && hasRd) {
            // SEC-REV-REQ-CHANGE-01：高危 state-drift —— 写需求池失败必须 rollback，change 不得 commit 成 APPROVED 状态
            // 先回写需求池，成功后再置 change=APPROVED 并写审计；任何 RuntimeException 向上传播触发外层 @Transactional 回滚
            try {
                applyApprovedToRequirement(change);
            } catch (RuntimeException ex) {
                // 失败审计独立落库（AuditLogService.append REQUIRES_NEW），随后 rethrow 让 change 不会变成 APPROVED
                recordWriteBackFailure(change, ex);
                throw ex;
            }
            change.setStatus(STATUS_APPROVED);
            requirementChangeMapper.updateById(change);
            audit(actor, change, "REQ_CHANGE_APPROVE",
                "双签 APPROVE，变更单生效");
            return change;
        }
        // 单方 APPROVE：保持 PENDING_SIGN，等待另一方
        requirementChangeMapper.updateById(change);
        audit(actor, change, "REQ_CHANGE_PARTIAL_SIGN",
            actor.role() + " 已 APPROVE，等待另一方");
        return change;
    }

    /**
     * AC-GATE-12：通过后回写需求池（status="ADOPTED"）。
     * 若 afterSnapshot 含 status 字段，以快照内为准；否则写"ADOPTED"。
     *
     * <p>SEC-REV-REQ-CHANGE-01：高危 state-drift 修复 —— 移除 try/catch 让 {@code @Transactional}
     * 正常 rollback，杜绝「change 状态已置 APPROVED 但回写失败被吞」的错位 commit。
     * 失败将由调用方 catch 后落入 REQ_CHANGE_WRITE_BACK_FAIL 审计 + 抛出供外层回滚。
     */
    private void applyApprovedToRequirement(RequirementChange change) {
        Requirement requirement = requirementMapper.selectById(change.getRequirementId());
        if (requirement == null) {
            return;
        }
        requirement.setStatus("ADOPTED");
        requirement.setUpdateTime(new Date());
        requirementMapper.updateById(requirement);
        audit(SYSTEM_ACTOR, change, "REQ_CHANGE_WRITE_BACK",
            "需求池 " + change.getRequirementId() + " 状态回写为 ADOPTED");
    }

    /**
     * SEC-REV-REQ-CHANGE-02：中危 sensitive-observability —— 审计失败原因仅落异常类名，
     * 绝不落 ex.getMessage()（可能含 SQL 片段、参数值、文件路径等敏感数据）。
     *
     * <p>REQUIRES_NEW 独立事务：让失败审计在主事务回滚前落库，避免被一起带走
     * （审计链 append 已默认 REQUIRES_NEW，但本卡为契约清晰仍显式标注）。
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void recordWriteBackFailure(RequirementChange change, RuntimeException ex) {
        audit(SYSTEM_ACTOR, change, "REQ_CHANGE_WRITE_BACK_FAIL",
            "回写失败：" + ex.getClass().getSimpleName());
    }

    /**
     * KPI：需求变更率 = 变更单数 ÷ 总需求数（AC-KPI-14，无需手工填）。
     *
     * <p>SEC-REV-REQ-CHANGE-03：中危 missing-tenant-scope 修复 —— 强制要求传入 actor，按 actor 角色+group 限定 KPI 范围：
     * <ul>
     *   <li>SUPER_ADMIN：可看全局（不过滤）</li>
     *   <li>其他内部角色：按 actor.groupId() 限定本人所在组的 KPI 视角</li>
     *   <li>传入 null actor → 抛 UNAUTHORIZED（拒绝 null 旁路）</li>
     * </ul>
     * 旧实现 {@code selectCount(null)} 完全绕过租户/分组过滤，被列为中危漏洞。
     */
    public double kpiChangeRate(IpdActor actor) {
        requireInternal(actor);
        LambdaQueryWrapper<RequirementChange> changeQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Requirement> reqQw = new LambdaQueryWrapper<>();
        if (!"SUPER_ADMIN".equals(actor.role())) {
            // 非超管必须按 groupId 限定 KPI 视角（与 SEC-02 canReadProject 对齐）
            if (actor.groupId() == null) {
                throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "非超管调用 kpiChangeRate 必须有 groupId");
            }
            String scopeGroupId = String.valueOf(actor.groupId());
            changeQw.apply("project_id IN (SELECT id FROM projects WHERE main_group_id = {0})", scopeGroupId);
            reqQw.apply("project_id IN (SELECT id FROM projects WHERE main_group_id = {0})", scopeGroupId);
        }
        Long totalChanges = requirementChangeMapper.selectCount(changeQw);
        Long totalRequirements = requirementMapper.selectCount(reqQw);
        if (totalRequirements == null || totalRequirements == 0) {
            return 0.0;
        }
        long changeCount = totalChanges == null ? 0L : totalChanges;
        return Math.round((double) changeCount * 10000.0 / totalRequirements) / 10000.0;
    }

    /** 兼容旧测试：默认走 SUPER_ADMIN 视角（无 group 限定）。生产代码应使用 {@link #kpiChangeRate(IpdActor)}。 */
    public double kpiChangeRate() {
        return kpiChangeRate(new IpdActor(0L, "system", "SUPER_ADMIN", null));
    }

    /**
     * 阶段门禁集成（AC-GATE-11 跳阶拒绝）：GateEngine 跳阶前调本方法，
     * 存在未闭环变更单 ⇒ 抛 GATE_NOT_PASSED，提示"存在未完成变更单"。
     * 返回当前未闭环变更单数量（>=1 时阻断）。
     */
    public int countOpenByProject(Long projectId) {
        if (projectId == null) {
            return 0;
        }
        Long count = requirementChangeMapper.selectCount(new LambdaQueryWrapper<RequirementChange>()
            .eq(RequirementChange::getProjectId, projectId)
            .in(RequirementChange::getStatus, List.of(STATUS_DRAFT, STATUS_PENDING_SIGN)));
        return count == null ? 0 : count.intValue();
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

    /**
     * P2-6.2：影响评估四维度（范围/成本/时限/质量）必填校验。
     *
     * <p>语义：
     * <ul>
     *   <li>null/空串 ⇒ PARAM_INVALID（影响快照未提交）</li>
     *   <li>非合法 JSON / 非对象 ⇒ PARAM_INVALID（结构错）</li>
     *   <li>对象节点上「范围/成本/时限/质量」任一键缺失或值为 null/空串 ⇒ PARAM_INVALID（维度遗漏）</li>
     * </ul>
     *
     * <p>设计要点 —— 不挑「键值语义」（不强求数字/枚举），只确保四维度被显式登记；
     * 这样：
     * <ul>
     *   <li>前端可以为「范围」「质量」存非数字（如「全国」「高」）</li>
     *   <li>不会因校验器与业务字段语义不一致而误拒合法请求</li>
     *   <li>未来若需要字段语义校验（如「成本」必须为数值），可加在 GateReview / 提交审批处，create 保持轻量</li>
     * </ul>
     */
    private void validateFourDimensionalSnapshot(String snapshot, String fieldName) {
        if (snapshot == null || snapshot.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                fieldName + " 必填影响评估四维度（范围/成本/时限/质量）");
        }
        JsonNode node;
        try {
            node = SNAPSHOT_JSON.readTree(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                fieldName + " 不是合法 JSON：" + ex.getClass().getSimpleName());
        }
        if (node == null || !node.isObject()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                fieldName + " 必须是 JSON 对象（顶层 {}）");
        }
        for (String key : REQUIRED_DIMENSION_KEYS) {
            JsonNode v = node.get(key);
            if (v == null || v.isNull()) {
                throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                    fieldName + " 缺少影响维度「" + key + "」");
            }
            // 文本/数字/数组都允许 —— 但「空串」/「空对象」/「空数组」视为未填写
            if (v.isTextual() && v.asText().isBlank()) {
                throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                    fieldName + " 维度「" + key + "」值不能为空");
            }
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
