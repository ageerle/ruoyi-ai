package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.NegativeFeedback;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.dto.NegativeFeedbackCreateReq;
import org.ruoyi.ipd.dto.NegativeFeedbackDecisionReq;
import org.ruoyi.ipd.dto.NegativeFeedbackView;
import org.ruoyi.ipd.mapper.NegativeFeedbackMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * P3-8.2 负反馈执行（BR-INC-10；AC-INC-36b/37/38/39/40）
 *
 * <p>四种触发情形 × 主责方 / 连带方映射：
 * <ul>
 *   <li>REWORK_EXCEEDED 需求返工率超标：MARKET_PM（主责，STOP_ALLOWANCE）+ RD_PM（连带，HALVE_ALLOWANCE）</li>
 *   <li>QUALITY_ACCIDENT 质量事故：RD_PM（主责，STOP_ALLOWANCE）+ MARKET_PM（连带，HALVE_ALLOWANCE）</li>
 *   <li>SPEC_PILE_COPY 参数堆砌/对标抄袭：RD_PM（主责，STOP_ALLOWANCE）+ MARKET_PM（连带，HALVE_ALLOWANCE）</li>
 *   <li>MISSED_MARKET_WINDOW 错过市场窗口：BOTH 双PM共同担责（STOP_ALLOWANCE × 2，无连带减半）</li>
 * </ul>
 *
 * <p>状态机：DRAFT → PENDING_DECISION → EXECUTED → LIFTED；任意阶段可 REJECTED。
 * <p>AC-INC-40 唯一索引 uk_nf_project_trigger_active + service 二次校验 = 重复事件不重复扣减。
 * <p>decide 后通过 {@link NotificationService} 发 ACTION 通知双 PM（AC-INC-40 联动）。
 */
@Slf4j
@Service
public class NegativeFeedbackService {

    /** BR-INC-10 四种触发情形白名单 */
    public static final Set<String> TRIGGER_TYPES = Set.of(
        "REWORK_EXCEEDED", "QUALITY_ACCIDENT", "SPEC_PILE_COPY", "MISSED_MARKET_WINDOW");
    /** 状态机常量 */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_DECISION = "PENDING_DECISION";
    public static final String STATUS_EXECUTED = "EXECUTED";
    public static final String STATUS_LIFTED = "LIFTED";
    public static final String STATUS_REJECTED = "REJECTED";
    /** 执行动作常量 */
    public static final String EXEC_STOP_ALLOWANCE = "STOP_ALLOWANCE";
    public static final String EXEC_HALVE_ALLOWANCE = "HALVE_ALLOWANCE";
    /** YYYY-MM 月份正则 */
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    /** 默认贡献度系数降低值（BR-INC-10：贡献度系数降低） */
    public static final BigDecimal DEFAULT_TIER_DELTA = new BigDecimal("-0.50");

    private final NegativeFeedbackMapper mapper;
    private final ProjectMemberMapper memberMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    /** SEC-REV-round3 Bug#4：项目归属校验——非超管仅可访问本人所在 group 的项目 */
    private final ProjectMapper projectMapper;
    private final IpdPermission ipdPermission;

    /** 测试口：仅 mapper 单注入（其它 mapper mock 时） */
    public NegativeFeedbackService(NegativeFeedbackMapper mapper) {
        this(mapper, null, null, null, null, null);
    }

    /** 兼容测试口：mapper + memberMapper */
    public NegativeFeedbackService(NegativeFeedbackMapper mapper,
                                   ProjectMemberMapper memberMapper,
                                   AuditLogService auditLogService,
                                   NotificationService notificationService) {
        this(mapper, memberMapper, auditLogService, notificationService, null, null);
    }

    /** Spring 装配入口（新增 ProjectMapper + IpdPermission 注入；Bug#4 修复） */
    @Autowired
    public NegativeFeedbackService(NegativeFeedbackMapper mapper,
                                   ProjectMemberMapper memberMapper,
                                   AuditLogService auditLogService,
                                   NotificationService notificationService,
                                   ProjectMapper projectMapper,
                                   IpdPermission ipdPermission) {
        this.mapper = mapper;
        this.memberMapper = memberMapper;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.projectMapper = projectMapper;
        this.ipdPermission = ipdPermission;
    }

    /* ========================================================================
     *  AC-INC-40：triggerType → (mainRole, relatedRole, mainExec, relatedExec)
     * ======================================================================== */

    /**
     * 推导主责 / 连带映射（BR-INC-10 权威表）。
     *
     * @param triggerType 四种情形之一
     * @return 4 元映射（mainRole, relatedRole, mainExec, relatedExec）；relatedRole/relatedExec 可空
     */
    public static Map<String, String> deriveRoleMapping(String triggerType) {
        if (triggerType == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_TRIGGER_TYPE_INVALID);
        }
        Map<String, String> map = new LinkedHashMap<>();
        switch (triggerType) {
            case "REWORK_EXCEEDED":
                map.put("mainRole", "MARKET_PM");
                map.put("relatedRole", "RD_PM");
                map.put("mainExec", EXEC_STOP_ALLOWANCE);
                map.put("relatedExec", EXEC_HALVE_ALLOWANCE);
                break;
            case "QUALITY_ACCIDENT":
            case "SPEC_PILE_COPY":
                map.put("mainRole", "RD_PM");
                map.put("relatedRole", "MARKET_PM");
                map.put("mainExec", EXEC_STOP_ALLOWANCE);
                map.put("relatedExec", EXEC_HALVE_ALLOWANCE);
                break;
            case "MISSED_MARKET_WINDOW":
                map.put("mainRole", "BOTH");
                map.put("relatedRole", null);
                map.put("mainExec", EXEC_STOP_ALLOWANCE);
                map.put("relatedExec", null);
                break;
            default:
                throw new IpdBusinessException(ApiV1ErrorCode.NF_TRIGGER_TYPE_INVALID);
        }
        return map;
    }

    /** 月份格式校验 */
    public static void validateMonth(String month, String fieldName) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_MONTH_FORMAT_INVALID);
        }
    }

    /* ========================================================================
     *  状态机流转
     * ======================================================================== */

    /**
     * 录入负反馈（DRAFT 创建）。
     *
     * <p>AC-INC-40 重复检查：同项目同 triggerType 已 EXECUTED → NF_REENTRY_NOT_ALLOWED。
     * <p>SEC-REV-round3 Bug#6（中危 TOCTOU-dedup-bypass）：service selectCount 与 DB 唯一索引
     * 维度不同会导致并发插入绕过 service 检查但被 DB 拒抛 raw DuplicateKeyException；
     * 修复：service 镜像索引维度（count 任意 del_flag=0）+ catch DuplicateKeyException 翻 NF_REENTRY_NOT_ALLOWED。
     * <p>SEC-REV-round3 Bug#4（高危 horizontal-privilege）：录入前按项目 group 校验 actor 归属。
     */
    @Transactional(rollbackFor = Exception.class)
    public NegativeFeedback create(NegativeFeedbackCreateReq req, IpdActor actor) {
        if (req == null || req.projectId() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.triggerType() == null || !TRIGGER_TYPES.contains(req.triggerType())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_TRIGGER_TYPE_INVALID);
        }
        validateMonth(req.triggerMonth(), "triggerMonth");
        if (req.recoveryMonth() != null && !req.recoveryMonth().isBlank()) {
            validateMonth(req.recoveryMonth(), "recoveryMonth");
        }
        // Bug#4：项目归属校验
        assertProjectReadableById(req.projectId(), actor);

        // Bug#6：service 镜像索引维度（任意 del_flag=0 而非仅 EXECUTED）+ 兜底 catch DuplicateKeyException
        Long activeCount = mapper.selectCount(Wrappers.<NegativeFeedback>lambdaQuery()
            .eq(NegativeFeedback::getProjectId, req.projectId())
            .eq(NegativeFeedback::getTriggerType, req.triggerType())
            .eq(NegativeFeedback::getDelFlag, "0"));
        if (activeCount != null && activeCount > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_REENTRY_NOT_ALLOWED);
        }

        Map<String, String> mapping = deriveRoleMapping(req.triggerType());
        // 取项目双 PM（在职、未退出）
        List<Long> mainAndRelatedIds = lookupPmPair(req.projectId(), mapping);
        if (mainAndRelatedIds.size() < 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_NOT_PM);
        }

        NegativeFeedback row = NegativeFeedback.builder()
            .projectId(req.projectId())
            .triggerType(req.triggerType())
            .mainRole(mapping.get("mainRole"))
            .mainPersonId(mainAndRelatedIds.get(0))
            .relatedRole(mapping.get("relatedRole"))
            .relatedPersonId(mainAndRelatedIds.size() > 1 ? mainAndRelatedIds.get(1) : null)
            .mainExecution(mapping.get("mainExec"))
            .relatedExecution(mapping.get("relatedExec"))
            .bonusDisqualify(1)
            .tierDelta(DEFAULT_TIER_DELTA)
            .triggerMonth(req.triggerMonth())
            .recoveryMonth(req.recoveryMonth())
            .triggerEvidence(req.triggerEvidence())
            .status(STATUS_DRAFT)
            .triggeredBy(actor.id())
            .build();
        try {
            // Bug#6：捕获 DB 唯一索引违例——并发残余插入翻成 NF_REENTRY_NOT_ALLOWED
            mapper.insert(row);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_REENTRY_NOT_ALLOWED,
                "同项目同触发情形的负反馈记录已存在（DB 唯一索引兜底）");
        }
        appendAudit("CREATE", row, actor, null, "P3-8.2 录入");
        return row;
    }

    /**
     * 提交认定（DRAFT → PENDING_DECISION）。
     */
    @Transactional(rollbackFor = Exception.class)
    public NegativeFeedback submit(Long id, IpdActor actor) {
        NegativeFeedback row = requireRow(id);
        assertProjectReadable(row, actor);  // SEC-REV-round3 Bug#4
        if (!STATUS_DRAFT.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_STATE_INVALID);
        }
        row.setStatus(STATUS_PENDING_DECISION);
        mapper.updateById(row);
        appendAudit("SUBMIT", row, actor, STATUS_DRAFT, "P3-8.2 提交认定");
        return row;
    }

    /**
     * 组长 / 超管认定（PENDING_DECISION → EXECUTED；REJECT → REJECTED）。
     *
     * <p>decide APPROVE 后通过 NotificationService 发 NEGATIVE_FEEDBACK_EXECUTED 通知双 PM。
     */
    @Transactional(rollbackFor = Exception.class)
    public NegativeFeedback decide(Long id, NegativeFeedbackDecisionReq req, IpdActor actor) {
        if (req == null || req.decision() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        NegativeFeedback row = requireRow(id);
        assertProjectReadable(row, actor);  // SEC-REV-round3 Bug#4
        if (!STATUS_PENDING_DECISION.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_STATE_INVALID);
        }

        String before = row.getStatus();
        if ("APPROVE".equalsIgnoreCase(req.decision())) {
            row.setStatus(STATUS_EXECUTED);
            row.setDecidedBy(actor.id());
            row.setDecidedAt(new Date());
            row.setDecisionComment(req.comment());
            mapper.updateById(row);
            appendAudit("DECIDE_EXECUTE", row, actor, before, "P3-8.2 认定执行（AC-INC-36b/37/38/39）");
            notifyExecuted(row);
        } else if ("REJECT".equalsIgnoreCase(req.decision())) {
            row.setStatus(STATUS_REJECTED);
            row.setDecidedBy(actor.id());
            row.setDecidedAt(new Date());
            row.setDecisionComment(req.comment());
            mapper.updateById(row);
            appendAudit("DECIDE_REJECT", row, actor, before, "P3-8.2 驳回");
        } else {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        return row;
    }

    /**
     * 解除（EXECUTED → LIFTED；恢复 bonusEligible）。
     *
     * <p>AC-INC-40：解除动作不可逆（lift 后再触发新事件需要重新走 create 流程并通过唯一索引）。
     */
    @Transactional(rollbackFor = Exception.class)
    public NegativeFeedback lift(Long id, NegativeFeedbackDecisionReq req, IpdActor actor) {
        if (req == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        NegativeFeedback row = requireRow(id);
        assertProjectReadable(row, actor);  // SEC-REV-round3 Bug#4
        if (!STATUS_EXECUTED.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NF_STATE_INVALID);
        }
        String before = row.getStatus();
        row.setStatus(STATUS_LIFTED);
        row.setLiftedBy(actor.id());
        row.setLiftedAt(new Date());
        if (req.comment() != null && !req.comment().isBlank()) {
            row.setDecisionComment(req.comment());
        }
        mapper.updateById(row);
        appendAudit("LIFT", row, actor, before, "P3-8.2 解除（恢复津贴+bonusEligible）");
        notifyLifted(row);
        return row;
    }

    /* ========================================================================
     *  查询
     * ======================================================================== */

    public NegativeFeedback getById(Long id, IpdActor actor) {
        NegativeFeedback row = requireRow(id);
        // SEC-REV-round3 Bug#4：详情查询也走归属校验（cross-group 拒绝）
        assertProjectReadable(row, actor);
        return row;
    }

    /** 兼容旧测试口（无 actor）；详情默认放行——controller 路径已走 requireInternal。 */
    public NegativeFeedback getById(Long id) {
        return requireRow(id);
    }

    /**
     * 项目下当前生效中的负反馈记录（EXECUTED 且未解除）。
     * <p>SEC-REV-round3 Bug#4：actor 必传，非超管需校验项目归属。
     */
    public List<NegativeFeedback> effectiveByProject(Long projectId, IpdActor actor) {
        assertProjectReadableById(projectId, actor);  // Bug#4
        return mapper.selectList(Wrappers.<NegativeFeedback>lambdaQuery()
            .eq(NegativeFeedback::getProjectId, projectId)
            .eq(NegativeFeedback::getStatus, STATUS_EXECUTED)
            .orderByDesc(NegativeFeedback::getDecidedAt));
    }

    /** 兼容旧测试口 */
    public List<NegativeFeedback> effectiveByProject(Long projectId) {
        return effectiveByProject(projectId, null);
    }

    public List<NegativeFeedback> listByProject(Long projectId, IpdActor actor, String status) {
        assertProjectReadableById(projectId, actor);  // Bug#4
        LambdaQueryWrapper<NegativeFeedback> q = Wrappers.<NegativeFeedback>lambdaQuery()
            .eq(NegativeFeedback::getProjectId, projectId)
            .orderByDesc(NegativeFeedback::getCreateTime);
        if (status != null && !status.isBlank()) {
            q.eq(NegativeFeedback::getStatus, status);
        }
        return mapper.selectList(q);
    }

    /** 兼容旧测试口 */
    public List<NegativeFeedback> listByProject(Long projectId, String status) {
        return listByProject(projectId, null, status);
    }

    /** Bug#4：根据 projectId 校验 actor 是否有权访问该项目。 */
    private void assertProjectReadableById(Long projectId, IpdActor actor) {
        if (projectId == null) return;
        if (projectMapper == null || ipdPermission == null || actor == null) return;
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (!ipdPermission.canReadProject(actor.role(), actor.groupId(), project.getMainGroupId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权访问该项目的负反馈记录（跨 group 拒绝）");
        }
    }

    /* ========================================================================
     *  视图转换
     * ======================================================================== */

    public static NegativeFeedbackView toView(NegativeFeedback row) {
        if (row == null) return null;
        return NegativeFeedbackView.builder()
            .id(row.getId())
            .projectId(row.getProjectId())
            .triggerType(row.getTriggerType())
            .mainRole(row.getMainRole())
            .mainPersonId(row.getMainPersonId())
            .relatedRole(row.getRelatedRole())
            .relatedPersonId(row.getRelatedPersonId())
            .mainExecution(row.getMainExecution())
            .relatedExecution(row.getRelatedExecution())
            .bonusDisqualify(row.getBonusDisqualify() != null && row.getBonusDisqualify() == 1)
            .tierDelta(row.getTierDelta())
            .triggerMonth(row.getTriggerMonth())
            .recoveryMonth(row.getRecoveryMonth())
            .triggerEvidence(row.getTriggerEvidence())
            .status(row.getStatus())
            .triggeredBy(row.getTriggeredBy())
            .decidedBy(row.getDecidedBy())
            .decidedAt(row.getDecidedAt())
            .liftedBy(row.getLiftedBy())
            .liftedAt(row.getLiftedAt())
            .decisionComment(row.getDecisionComment())
            .build();
    }

    /* ========================================================================
     *  私有
     * ======================================================================== */

    private NegativeFeedback requireRow(Long id) {
        if (id == null) throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        NegativeFeedback row = mapper.selectById(id);
        if (row == null) throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        return row;
    }

    /**
     * SEC-REV-round3 Bug#4（高危 horizontal-privilege-escalation）：
     * 校验 actor 对项目归属——非超管仅可访问本人所在 group 的项目。
     * <p>SUPER_ADMIN 例外放行；其他内部角色按 actor.groupId() == project.mainGroupId() 判定。
     * 缺 mapper（测试口）时降级为放行——避免破坏既有 mock 单元测试。
     */
    private void assertProjectReadable(NegativeFeedback row, IpdActor actor) {
        if (row == null || row.getProjectId() == null || actor == null) return;
        if (projectMapper == null || ipdPermission == null) return;
        Project project = projectMapper.selectById(row.getProjectId());
        if (project == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        boolean canRead = ipdPermission.canReadProject(actor.role(), actor.groupId(), project.getMainGroupId());
        if (!canRead) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权访问该项目的负反馈记录（跨 group 拒绝）");
        }
    }

    /**
     * 查项目双 PM：返回 [主责PersonId, 连带PersonId]；MISSED_MARKET_WINDOW 返 [市场, 研发]。
     * 缺任一角色抛 NF_NOT_PM。
     */
    private List<Long> lookupPmPair(Long projectId, Map<String, String> mapping) {
        if (memberMapper == null) {
            // 测试环境或缺 mapper：返空，service 层按 NF_NOT_PM 走
            return List.of();
        }
        List<ProjectMember> rows = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
            .eq(ProjectMember::getProjectId, projectId)
            .in(ProjectMember::getRole, Arrays.asList("MARKET_PM", "RD_PM"))
            .isNull(ProjectMember::getExitDate));
        Long marketId = null;
        Long rdId = null;
        for (ProjectMember m : rows) {
            if ("MARKET_PM".equals(m.getRole())) marketId = m.getPersonId();
            if ("RD_PM".equals(m.getRole())) rdId = m.getPersonId();
        }
        String mainRole = mapping.get("mainRole");
        String relatedRole = mapping.get("relatedRole");
        java.util.List<Long> ids = new java.util.ArrayList<>();
        if ("MARKET_PM".equals(mainRole) || "BOTH".equals(mainRole)) {
            if (marketId == null) throw new IpdBusinessException(ApiV1ErrorCode.NF_NOT_PM);
            ids.add(marketId);
        } else if ("RD_PM".equals(mainRole)) {
            if (rdId == null) throw new IpdBusinessException(ApiV1ErrorCode.NF_NOT_PM);
            ids.add(rdId);
        }
        if (relatedRole != null) {
            if ("MARKET_PM".equals(relatedRole) && marketId != null) ids.add(marketId);
            if ("RD_PM".equals(relatedRole) && rdId != null) ids.add(rdId);
        }
        return ids;
    }

    /** 通知主责 / 连带 PM：EXECUTE 通知 */
    private void notifyExecuted(NegativeFeedback row) {
        if (notificationService == null) return;
        for (Long personId : Arrays.asList(row.getMainPersonId(), row.getRelatedPersonId())) {
            if (personId == null) continue;
            notificationService.publish(personId,
                NotificationService.Types.NEGATIVE_FEEDBACK_EXECUTED,
                NotificationService.KIND_ACTION,
                "negative_feedback", row.getId(),
                "负反馈已执行（" + triggerZh(row.getTriggerType()) + "）",
                "项目负反馈已认定执行；主责方" + execZh(row.getMainExecution())
                    + (row.getRelatedExecution() != null
                        ? "；连带方" + execZh(row.getRelatedExecution())
                        : "；双PM共同担责") + "。生效月份 " + row.getTriggerMonth(),
                "/incentive/negative-feedback");
        }
    }

    /** 通知主责 / 连带 PM：LIFT 通知（FYI） */
    private void notifyLifted(NegativeFeedback row) {
        if (notificationService == null) return;
        for (Long personId : Arrays.asList(row.getMainPersonId(), row.getRelatedPersonId())) {
            if (personId == null) continue;
            notificationService.publish(personId,
                NotificationService.Types.NEGATIVE_FEEDBACK_LIFTED,
                NotificationService.KIND_FYI,
                "negative_feedback", row.getId(),
                "负反馈已解除（" + triggerZh(row.getTriggerType()) + "）",
                "项目负反馈已解除；津贴/奖金资格已恢复。",
                "/incentive/negative-feedback");
        }
    }

    private static String triggerZh(String triggerType) {
        if (triggerType == null) return "未知";
        return switch (triggerType) {
            case "REWORK_EXCEEDED" -> "需求返工率超标";
            case "QUALITY_ACCIDENT" -> "质量事故";
            case "SPEC_PILE_COPY" -> "参数堆砌/对标抄袭";
            case "MISSED_MARKET_WINDOW" -> "错过市场窗口";
            default -> triggerType;
        };
    }

    private static String execZh(String exec) {
        if (exec == null) return "—";
        return switch (exec) {
            case "STOP_ALLOWANCE" -> "津贴停发";
            case "HALVE_ALLOWANCE" -> "津贴减半";
            case "BONUS_DOWNGRADE" -> "贡献度系数降低";
            case "BONUS_DISQUALIFY" -> "取消奖金分配资格";
            default -> exec;
        };
    }

    /** 审计日志：append P3-8.2 写动作（create/submit/decide/lift） */
    private void appendAudit(String action, NegativeFeedback row, IpdActor actor, String before, String reason) {
        if (auditLogService == null) return;
        try {
            // SEC-REV-round3 Bug#5（中危 audit-integrity）：原实现把 row.getTriggeredBy() 当操作人——
            // 这是 creator（创建人），而非 operator（操作人）。submit/decide/lift 的真实操作人是 actor，
            // 二者可能完全不同（例如组长跨人代签）。修复：用 actor.id() 作为 operatorName。
            String operatorName = actor != null && actor.id() != null ? String.valueOf(actor.id()) : "system";
            auditLogService.append(AuditLog.builder()
                .operatorName(operatorName)
                .operatorRole(actor != null ? actor.role() : "PM")
                .action(action)
                .entityType("NEGATIVE_FEEDBACK")
                .entityId(row.getId())
                .beforeData(before != null ? ("\"" + before + "\"") : null)
                .afterData("\"" + row.getStatus() + "\"")
                .reason(reason)
                .build());
        } catch (RuntimeException e) {
            // 审计失败不阻断主流程（与既有 StageActionService / GateReviewService 一致策略）
            log.warn("P3-8.2 audit append failed (action={}, id={}): {}", action, row.getId(), e.getMessage());
        }
    }

    /** 旧签名兼容：保留 appendAudit(action, row, before, reason) 用于未传 actor 的调用点（已无调用方，本方法 deprecated）。 */
    @Deprecated
    private void appendAudit(String action, NegativeFeedback row, String before, String reason) {
        appendAudit(action, row, null, before, reason);
    }
}