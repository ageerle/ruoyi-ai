package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.GateReviewObserver;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.GateReviewObserverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * P2-5.2 Gate 双签：G1/G5 盲签与 G2/3/4 领域签署（BR-GATE-03/04/08，AC-GATE-03/04/05）。
 *
 * <p>签署矩阵（双签否决范围 BR-GATE-03：G1/需求变更/G5）：
 * <ul>
 *   <li><b>G1/G5 双签盲签</b>：市场PM 与研发PM 并行独立提交；任一 REJECT ⇒ Gate 直接
 *       REJECTED（AC-GATE-05，双方均收 GATE_REJECTED 通知）；双 APPROVE ⇒ APPROVED，
 *       双方结论同时揭示（AC-GATE-04）。双方都提交前互不可见对方结论，仅见
 *       "对方已提交"（AC-GATE-03，BR-GATE-03 并行签署）。</li>
 *   <li><b>G2/3/4 领域签署</b>：流程门禁但非双签否决（页24），由领域主导方单签终态；
 *       非主导方 PM 签署拒绝（非授权角色）。</li>
 *   <li><b>主导方按领域非先提交方</b>（owner 2026-09-05 决策，覆盖附录 D6 "先提交方"旧解）：
 *       BR-GATE-08 RACI——产品定位与场景（市场 A/R）⇒ G1 立项/G2 规划/G5 上市归市场；
 *       差异化创新（研发 A/R）⇒ G3 开发/G4 验证归研发（G4 否决项 G4-1/2/3 全研发交付侧）。</li>
 * </ul>
 *
 * <p>超时弃权/期限提醒/仲裁归 P2-5.4；条件遗留项归 P2-5.3。
 *
 * <p>P2-5.4 补充（AC-GATE-06~10/21；BR-GATE-04/05/06）：
 * <ul>
 *   <li><b>重发起（AC-GATE-06/07/07b）</b>：REJECTED/双弃权 Gate 可手动重新发起，
 *       round+1 且签署期限重新起算；第 3 轮起双方产品组长自动列席，
 *       第 5 轮起超管介入通知（BR-GATE-05 不限次数）。</li>
 *   <li><b>超时弃权（AC-GATE-08，BR-GATE-04 D17）</b>：双签 Gate 一方 3 个自然日未签
 *       ⇒ 自动补 ABSTAIN 行，按主导方意见执行并审计弃权事件；两人均未签
 *       ⇒ 双 ABSTAIN 转 ABSTAINED_TIMEOUT，不得无依据放行。</li>
 *   <li><b>期限前 1 天提醒（AC-GATE-09）</b>：未签方收 GATE_SIGN_SOON（每日去重）。</li>
 *   <li><b>冲突仲裁（AC-GATE-10，BR-GATE-06）</b>：双 PM 分歧（先 APPROVE 后 REJECT）
 *       ⇒ 自动邀请双方组长仲裁；两组仍不一致 ⇒ 升级超管终裁，
 *       终裁结果写入项目审计日志（gate_arbitrations 表按人去重，避开
 *       gate_reviews 的 reviewer_type 唯一约束）。</li>
 *   <li><b>延期（AC-GATE-21）</b>：超管可单独延长在签 Gate 的签署期限，
 *       最多 3 次，第 4 次拒绝。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GateReviewService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_APPROVED = "APPROVED";
    static final String STATUS_REJECTED = "REJECTED";
    /** 双方均超期未签：不得无依据放行（P2-5.4 验收列），需 reopen 重启 */
    static final String STATUS_ABSTAINED_TIMEOUT = "ABSTAINED_TIMEOUT";
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REJECT");
    private static final Set<String> ARBITRATION_DECISIONS = Set.of("APPROVE", "REJECT");
    /** AC-GATE-21：签署期限最多延长 3 次（默认；运行时由 BusinessConfigService.GATE_EXTENSION_MAX_COUNT 覆盖） */
    static final int DEFAULT_MAX_SIGN_EXTENSIONS = 3;
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_GROUP_LEADER = "GROUP_LEADER";

    /** 签署期限参数（BR-GATE-04 3 个自然日；运行时由 BusinessConfigService.GATE_SIGN_DEADLINE_DAYS 覆盖） */
    static final String SIGN_DEADLINE_KEY = BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS;
    /** G1/G5 双签最低人数（运行时由 BusinessConfigService.GATE_DUAL_SIGN_COUNT 覆盖；当前仅 1-2 个角色值，参与兜底） */
    static final String DUAL_SIGN_COUNT_KEY = BusinessConfigKeys.GATE_DUAL_SIGN_COUNT;
    private static final Set<String> SIGNER_ROLES = Set.of("MARKET_PM", "RD_PM");
    /** MEDIUM-1.3：列席人员角色（销售/供应/售后/品质/合规）。 */
    private static final Set<String> OBSERVER_ROLES = Set.of("SALES", "SUPPLY", "AFTERSALES", "QUALITY", "COMPLIANCE");

    private final GateMapper gateMapper;
    private final GateReviewMapper reviewMapper;
    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final GateArbitrationMapper arbitrationMapper;
    private final GateReviewObserverMapper observerMapper;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    /** ROOT-R1 P0-7 字面量迁移：Gate 配置（双签人数/签署期限/延期上限；B-RULE-05 配套）来源 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BusinessConfigService businessConfigService;

    /** G1/G5 为双签盲签 Gate（BR-GATE-03 双签否决范围：G1/需求变更/G5）。 */
    static boolean isDualSignGate(String gateCode) {
        return "G1".equals(gateCode) || "G5".equals(gateCode);
    }

    /** 领域主导方：G3/G4 研发（差异化创新 A/R），其余市场（产品定位与场景 A/R）。 */
    static String leadSideOf(String gateCode) {
        return "G3".equals(gateCode) || "G4".equals(gateCode) ? "RD_PM" : "MARKET_PM";
    }

    /** 签署：写入本轮 GateReview，按签署矩阵推进 Gate 终态。 */
    @Transactional(rollbackFor = Exception.class)
    public GateReview sign(Long gateId, String decision, String opinion, IpdActor actor) {
        Gate gate = requireGate(gateId);
        requireSubmitted(gate);
        if (decision == null || !DECISIONS.contains(decision)) {
            throw new IpdBusinessException("decision 仅允许 APPROVE|REJECT");
        }
        requireAuthorized(gate.getGateCode(), actor);
        requireNotSigned(gate, actor.role());

        GateReview row = GateReview.builder()
            .gateId(gateId)
            .reviewerType(actor.role())
            .reviewerId(actor.id())
            .decision(decision)
            .opinion(opinion)
            .signedAt(new Date())
            .dueAt(dueAtFrom(gate))
            .round(gate.getCurrentRound())
            .build();
        reviewMapper.insert(row);

        audit(actor, gate, "GATE_SIGN", null,
            "decision", decision, "opinion", opinion, "round", gate.getCurrentRound());

        advance(gate, actor);
        return row;
    }

    /** 双签视图：终态或超管全揭示；在途仅见己方结论与"对方已提交"标志（AC-GATE-03/04）。 */
    public Map<String, Object> view(Long gateId, IpdActor actor) {
        Gate gate = requireGate(gateId);
        List<GateReview> rows = roundRows(gateId, gate.getCurrentRound());
        boolean terminal = !STATUS_PENDING.equals(gate.getStatus());
        boolean superAdmin = "SUPER_ADMIN".equals(actor.role());
        boolean revealed = terminal || superAdmin;

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("gateId", String.valueOf(gateId));
        view.put("gateCode", gate.getGateCode());
        view.put("status", gate.getStatus());
        view.put("round", gate.getCurrentRound());
        view.put("dualSign", isDualSignGate(gate.getGateCode()));
        view.put("leadSide", leadSideOf(gate.getGateCode()));
        // P2-5.4：签署期限/延期次数（AC-GATE-08/21 展示锚点）
        view.put("signDueAt", gate.getSignDueAt() == null ? null : gate.getSignDueAt().toString());
        view.put("extensionCount", gate.getSignExtensionCount());
        // AC-GATE-07：第 3 轮起双方产品组长自动列席（前端展示列席人）
        if (gate.getCurrentRound() != null && gate.getCurrentRound() >= 3) {
            view.put("observers", collectLeaders(gate).stream()
                .map(l -> Map.of("id", String.valueOf(l.getId()), "name", l.getName()))
                .toList());
        }

        GateReview mine = rows.stream().filter(r -> actor.role().equals(r.getReviewerType()))
            .findFirst().orElse(null);
        // 自己的结论对自己总可见；对方的仅在揭示后可见
        view.put("my", mine == null ? null : rowView(mine, true));

        GateReview other = rows.stream().filter(r -> !actor.role().equals(r.getReviewerType()))
            .findFirst().orElse(null);
        // AC-GATE-03：对方已提交但未揭示 ⇒ 只返回 otherSubmitted=true，不泄露结论/意见
        view.put("otherSubmitted", other != null);
        view.put("other", other == null ? null : rowView(other, revealed));
        if (!revealed && mine == null && other != null) {
            view.put("hint", "对方已提交，等待您签署");
        }
        return view;
    }

    /** 终态推进：双签 Gate 双方齐签或任一 REJECT；领域 Gate 主导方单签即终态。 */
    private void advance(Gate gate, IpdActor actor) {
        List<GateReview> rows = roundRows(gate.getId(), gate.getCurrentRound());
        boolean rejected = rows.stream().anyMatch(r -> "REJECT".equals(r.getDecision()));
        boolean dual = isDualSignGate(gate.getGateCode());

        if (rejected) {
            settle(gate, STATUS_REJECTED, actor, rows);
            return;
        }
        if (dual) {
            if (rows.size() >= 2) {   // 双 APPROVE ⇒ 通过（AC-GATE-04）
                settle(gate, STATUS_APPROVED, actor, rows);
            }
            return;                    // 一方已签仍在途：等另一方（盲签保持）
        }
        settle(gate, STATUS_APPROVED, actor, rows);  // 领域 Gate 主导方 APPROVE 单签终态
    }

    /** 落终态：更新 Gate 状态 + REJECTED 时双方 GATE_REJECTED 通知（AC-GATE-05）+ 审计。 */
    private void settle(Gate gate, String status, IpdActor actor, List<GateReview> rows) {
        if (STATUS_PENDING.equals(gate.getStatus())) {
            gate.setStatus(status);
            gateMapper.updateById(gate);
            audit(actor, gate, "REJECTED".equals(status) ? "GATE_REJECT" : "GATE_APPROVE",
                "终态 " + status, "round", gate.getCurrentRound());
        }
        if (STATUS_REJECTED.equals(status)) {
            notifyBothSides(gate, rows);
            // P2-5.4 AC-GATE-10：双 PM 意见分歧（先 APPROVE 后 REJECT）⇒ 自动邀请组长仲裁
            if (hasPmConflict(rows)) {
                openArbitration(gate, actor);
            }
        }
    }

    /** 双方=该 Gate 的市场/研发两位签署人；成员绑定缺失时退化通知已签方（AC-GATE-05）。 */
    private void notifyBothSides(Gate gate, List<GateReview> rows) {
        List<Long> receivers = new java.util.ArrayList<>();
        for (String role : List.of("MARKET_PM", "RD_PM")) {
            rows.stream().filter(r -> role.equals(r.getReviewerType())).findFirst()
                .ifPresent(r -> receivers.add(r.getReviewerId()));
        }
        if (receivers.size() < 2) {
            memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, gate.getProjectId())
                    .in(ProjectMember::getRole, List.of("MARKET_PM", "RD_PM"))
                    .isNull(ProjectMember::getExitDate))
                .forEach(m -> { if (!receivers.contains(m.getPersonId())) receivers.add(m.getPersonId()); });
        }
        for (Long receiver : receivers) {
            notificationService.publish(receiver, NotificationService.Types.GATE_REJECTED,
                NotificationService.KIND_ACTION, "gate", gate.getId(),
                "Gate " + gate.getGateCode() + " 已驳回",
                "Gate " + gate.getGateCode() + " 被否决驳回（第 " + gate.getCurrentRound() + " 轮），请查看评审详情",
                "/reviews/gate/" + gate.getId());
        }
    }

    private Map<String, Object> rowView(GateReview r, boolean revealed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reviewerType", r.getReviewerType());
        m.put("signedAt", r.getSignedAt() == null ? null : r.getSignedAt().toString());
        if (revealed) {
            m.put("decision", r.getDecision());
            m.put("opinion", r.getOpinion());
        }
        return m;
    }

    private Gate requireGate(Long gateId) {
        Gate gate = gateMapper.selectById(gateId);
        if (gate == null) {
            throw new IpdBusinessException("Gate 不存在");
        }
        return gate;
    }

    /** 仅接受 P2-5.1 提交后的待签 Gate（startedAt 置位=要素判定已冻结）。 */
    private void requireSubmitted(Gate gate) {
        if (!STATUS_PENDING.equals(gate.getStatus())) {
            throw new IpdBusinessException("Gate 已终态（" + gate.getStatus() + "），不可签署");
        }
        if (gate.getStartedAt() == null) {
            throw new IpdBusinessException("评审尚未提交，请先完成要素判定并提交（P2-5.1）");
        }
    }

    /** 签署授权：双 PM 才可签；G2/3/4 仅领域主导方（非授权角色拒绝）。 */
    private void requireAuthorized(String gateCode, IpdActor actor) {
        if (!SIGNER_ROLES.contains(actor.role())) {
            throw new IpdBusinessException("仅市场PM/研发PM可签署（组长列席与仲裁归后续流程）");
        }
        if (!isDualSignGate(gateCode) && !actor.role().equals(leadSideOf(gateCode))) {
            String lead = leadSideOf(gateCode);
            throw new IpdBusinessException("Gate " + gateCode + " 由" + sideName(lead) + "主导签署，您无权签署");
        }
    }

    /** 同轮同角色重复签署拒绝（并发窗口由 uk_gr_gate_type_round 唯一约束兜底）。 */
    private void requireNotSigned(Gate gate, String reviewerType) {
        boolean already = roundRows(gate.getId(), gate.getCurrentRound()).stream()
            .anyMatch(r -> reviewerType.equals(r.getReviewerType()));
        if (already) {
            throw new IpdBusinessException("本轮您已签署，不可重复签署");
        }
    }

    private Date dueAtFrom(Gate gate) {
        // P2-5.4：submit/reopen/extend 维护的显式期限优先（AC-GATE-21 延期锚点）；
        // 历史行 sign_due_at 为空时回退旧算法（startedAt + N 天），P2-5.2 行为兼容。
        if (gate.getSignDueAt() != null) {
            return gate.getSignDueAt();
        }
        int days = resolveSignDeadlineDays();
        Date base = gate.getStartedAt() == null ? new Date() : gate.getStartedAt();
        return new Date(base.getTime() + TimeUnit.DAYS.toMillis(days));
    }

    /**
     * ROOT-R1 P0-7：读取签署期限天数。先读 BusinessConfigService.GATE_SIGN_DEADLINE_DAYS，回退 SystemConfig。
     */
    private int resolveSignDeadlineDays() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getInt(SIGN_DEADLINE_KEY);
            } catch (Exception ex) {
                // fall through
            }
        }
        return systemConfigService.getIntValue(SIGN_DEADLINE_KEY, 3);
    }

    /**
     * ROOT-R1 P0-7：读取签署期限最大延期次数（默认 DEFAULT_MAX_SIGN_EXTENSIONS=3）。
     */
    private int resolveMaxSignExtensions() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getInt(BusinessConfigKeys.GATE_EXTENSION_MAX_COUNT);
            } catch (Exception ex) {
                // fall through
            }
        }
        return DEFAULT_MAX_SIGN_EXTENSIONS;
    }

    private List<GateReview> roundRows(Long gateId, Integer round) {
        return reviewMapper.selectList(new LambdaQueryWrapper<GateReview>()
            .eq(GateReview::getGateId, gateId)
            .eq(GateReview::getRound, round));
    }

    private static String sideName(String role) {
        return "RD_PM".equals(role) ? "研发PM" : "市场PM";
    }

    // ==================== P2-5.4：重发起 / 超时弃权 / 提醒 / 仲裁 / 延期 ====================

    /** 重新发起评审（AC-GATE-06/07/07b；BR-GATE-05 不限次数）：round+1、期限重算。
     * <p>第 3 轮起通知双方产品组长列席；第 5 轮起通知超管介入。 */
    @Transactional(rollbackFor = Exception.class)
    public Gate reopen(Long gateId, IpdActor actor) {
        Gate gate = requireGate(gateId);
        if (!STATUS_REJECTED.equals(gate.getStatus())
            && !STATUS_ABSTAINED_TIMEOUT.equals(gate.getStatus())) {
            throw new IpdBusinessException("仅被驳回或双弃权超时的 Gate 可重新发起，当前：" + gate.getStatus());
        }
        if (!SIGNER_ROLES.contains(actor.role()) && !ROLE_SUPER_ADMIN.equals(actor.role())) {
            throw new IpdBusinessException("仅签署双方或超管可重新发起评审");
        }
        int newRound = gate.getCurrentRound() + 1;
        int days = resolveSignDeadlineDays();
        Date newDue = new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(days));
        // 显式 set 清列：MP updateById 忽略 null 字段，concludedAt 必须置回 null
        gateMapper.update(null, new LambdaUpdateWrapper<Gate>()
            .eq(Gate::getId, gateId)
            .set(Gate::getStatus, STATUS_PENDING)
            .set(Gate::getCurrentRound, newRound)
            .set(Gate::getSignDueAt, newDue)
            .set(Gate::getConcludedAt, null));
        audit(actor, gate, "GATE_REOPEN", "否决后重新发起评审（BR-GATE-05 不限次数）",
            "round", newRound, "signDueAt", newDue.toString());

        Gate updated = requireGate(gateId);
        if (newRound >= 3) {
            for (Person leader : collectLeaders(updated)) {
                notificationService.publish(leader.getId(),
                    NotificationService.Types.GATE_ROUND_OBSERVER, NotificationService.KIND_ACTION,
                    "gate", gate.getId(),
                    "Gate " + gate.getGateCode() + " 第 " + newRound + " 轮评审请您列席",
                    "Gate " + gate.getGateCode() + " 进入第 " + newRound + " 轮评审（AC-GATE-07），双方产品组长自动加入列席",
                    "/reviews/gate/" + gate.getId());
            }
        }
        if (newRound >= 5) {
            for (Person admin : superAdmins()) {
                notificationService.publish(admin.getId(),
                    NotificationService.Types.GATE_ADMIN_INTERVENE, NotificationService.KIND_ACTION,
                    "gate", gate.getId(),
                    "Gate " + gate.getGateCode() + " 第 " + newRound + " 轮评审请超管介入",
                    "Gate " + gate.getGateCode() + " 进入第 " + newRound + " 轮评审（AC-GATE-07b），请超管介入协调",
                    "/reviews/gate/" + gate.getId());
            }
        }
        return updated;
    }

    /** 超时弃权扫描（AC-GATE-08，BR-GATE-04 D17）：双签 Gate 超期未签方自动补 ABSTAIN 行。
     * <p>三天规则按主导方区分：主导方已签 APPROVE ⇒ 按主导方意见执行放行；
     * 主导方弃权 ⇒ 无放行依据；两人均未签 ⇒ 双 ABSTAIN 转 ABSTAINED_TIMEOUT（不得无依据放行）。
     * 单签 Gate（G2/3/4）无对方弃权概念，不折算，超期由 scanRemind 催办。 */
    @Transactional(rollbackFor = Exception.class)
    public int scanTimeout(IpdActor operator) {
        List<Gate> pending = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .eq(Gate::getStatus, STATUS_PENDING)
            .isNotNull(Gate::getStartedAt));
        Date now = new Date();
        int handled = 0;
        for (Gate gate : pending) {
            if (!isDualSignGate(gate.getGateCode())) {
                continue;
            }
            Date due = dueAtFrom(gate);
            if (!now.after(due)) {
                continue;
            }
            List<GateReview> rows = roundRows(gate.getId(), gate.getCurrentRound());
            if (rows.stream().anyMatch(r -> "REJECT".equals(r.getDecision()))) {
                continue; // 防御：REJECT 应已 settle REJECTED，不在此折算
            }
            boolean marketSigned = rows.stream().anyMatch(r -> "MARKET_PM".equals(r.getReviewerType()));
            boolean rdSigned = rows.stream().anyMatch(r -> "RD_PM".equals(r.getReviewerType()));
            String lead = leadSideOf(gate.getGateCode());
            if (marketSigned && rdSigned) {
                continue; // 防御：双 APPROVE 应已 settle APPROVED
            }
            if (!marketSigned && !rdSigned) {
                insertAbstain(gate, "MARKET_PM", due);
                insertAbstain(gate, "RD_PM", due);
                settleTimeout(gate, STATUS_ABSTAINED_TIMEOUT, operator,
                    "双方均超期未签，双弃权不放行（需 reopen 重启）");
            } else {
                String absentSide = marketSigned ? "RD_PM" : "MARKET_PM";
                String presentSide = marketSigned ? "MARKET_PM" : "RD_PM";
                insertAbstain(gate, absentSide, due);
                audit(operator, gate, "GATE_ABSTAIN_TIMEOUT",
                    "超期未签自动弃权（AC-GATE-08 审计弃权事件）",
                    "side", absentSide, "round", gate.getCurrentRound());
                if (lead.equals(presentSide)) {
                    settleTimeout(gate, STATUS_APPROVED, operator,
                        "一方弃权按主导方意见执行（" + sideName(presentSide) + " 已签 APPROVE）");
                } else {
                    settleTimeout(gate, STATUS_ABSTAINED_TIMEOUT, operator,
                        "主导方（" + sideName(lead) + "）超期弃权，非主导方意见无放行依据");
                }
            }
            notifyBothPms(gate, NotificationService.Types.GATE_ABSTAINED,
                "Gate " + gate.getGateCode() + " 签署超期弃权流转",
                "第 " + gate.getCurrentRound() + " 轮签署超期，未签方已标记弃权（BR-GATE-04），请查看流转结果");
            handled++;
        }
        return handled;
    }

    /** 期限前 1 天提醒扫描（AC-GATE-09）：双签 Gate 提醒未签方；单签 Gate 提醒主导方。 */
    @Transactional(rollbackFor = Exception.class)
    public int scanRemind(IpdActor operator) {
        List<Gate> pending = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
            .eq(Gate::getStatus, STATUS_PENDING)
            .isNotNull(Gate::getStartedAt));
        Date now = new Date();
        int reminded = 0;
        for (Gate gate : pending) {
            long untilDue = dueAtFrom(gate).getTime() - now.getTime();
            if (untilDue <= 0 || untilDue > TimeUnit.DAYS.toMillis(1)) {
                continue; // 未到提醒窗口或已超期（归 scanTimeout）
            }
            List<GateReview> rows = roundRows(gate.getId(), gate.getCurrentRound());
            boolean dual = isDualSignGate(gate.getGateCode());
            for (String side : List.of("MARKET_PM", "RD_PM")) {
                if (dual && rows.stream().anyMatch(r -> side.equals(r.getReviewerType()))) {
                    continue; // 双签 Gate：已签方不提醒
                }
                if (!dual && !side.equals(leadSideOf(gate.getGateCode()))) {
                    continue; // 单签 Gate：仅主导方有签署义务
                }
                Long personId = signerPersonId(gate, side);
                if (personId == null) {
                    continue;
                }
                notificationService.publishDaily(personId, NotificationService.Types.GATE_SIGN_SOON,
                    NotificationService.KIND_ACTION, "gate", gate.getId(),
                    "Gate " + gate.getGateCode() + " 签署期限将于 24 小时内到期",
                    "第 " + gate.getCurrentRound() + " 轮签署期限即将到期（AC-GATE-09），请及时签署",
                    "/reviews/gate/" + gate.getId(), now);
                reminded++;
            }
        }
        return reminded;
    }

    /** 组长仲裁（AC-GATE-10 中段）：仅冲突双方所在组的产品组长，意见 APPROVE|REJECT。
     * <p>组长意见齐备后：一致 ⇒ 仲裁结果知会双方；不一致 ⇒ 自动升级超管终裁。 */
    @Transactional(rollbackFor = Exception.class)
    public GateArbitration arbitrate(Long gateId, String decision, String opinion, IpdActor actor) {
        Gate gate = requireGate(gateId);
        requireArbitratable(gate, actor, ROLE_GROUP_LEADER, "仅产品组长可提交仲裁意见");
        if (decision == null || !ARBITRATION_DECISIONS.contains(decision)) {
            throw new IpdBusinessException("仲裁意见仅允许 APPROVE|REJECT");
        }
        if (collectLeaders(gate).stream().noneMatch(l -> actor.id().equals(l.getId()))) {
            throw new IpdBusinessException("仅冲突双方所在组的产品组长可提交仲裁意见");
        }
        // 待裁行预落语义（WB-17-1 工作台对偶）：开仲裁时已为每位组长 INSERT decision=NULL 行，
        // 提交 = 原行落决策（UPDATE）；无行时（存量数据/旧路径）退回 INSERT。
        GateArbitration existing = arbitrationRows(gate, ROLE_GROUP_LEADER).stream()
            .filter(o -> actor.id().equals(o.getArbitratorId()))
            .findFirst().orElse(null);
        if (existing != null && existing.getDecision() != null) {
            throw new IpdBusinessException("您已提交本轮仲裁意见，不可重复提交");
        }
        GateArbitration row;
        if (existing != null) {
            row = existing.setDecision(decision).setOpinion(opinion);
            arbitrationMapper.updateById(row);
        } else {
            row = GateArbitration.builder()
                .gateId(gateId)
                .round(gate.getCurrentRound())
                .arbitratorType(ROLE_GROUP_LEADER)
                .arbitratorId(actor.id())
                .decision(decision)
                .opinion(opinion)
                .build();
            arbitrationMapper.insert(row);
        }
        audit(actor, gate, "GATE_ARBITRATION", "组长仲裁意见",
            "decision", decision, "round", gate.getCurrentRound());
        maybeEscalateAfterArbitration(gate, actor);
        return row;
    }

    /** 超管终裁（AC-GATE-10 尾段）：仅两组长意见不一致（已升级）后可提交，
     * 终裁结果写入项目审计日志永久归档。Gate 终态不因终裁翻转（变更须 reopen 新轮）。 */
    @Transactional(rollbackFor = Exception.class)
    public GateArbitration finalRuling(Long gateId, String decision, String opinion, IpdActor actor) {
        Gate gate = requireGate(gateId);
        requireArbitratable(gate, actor, ROLE_SUPER_ADMIN, "仅超级管理员可终裁");
        if (decision == null || !ARBITRATION_DECISIONS.contains(decision)) {
            throw new IpdBusinessException("终裁意见仅允许 APPROVE|REJECT");
        }
        // 预落的待裁行（decision=NULL）不计入「两组对立意见」判断
        List<GateArbitration> leaderOpinions = arbitrationRows(gate, ROLE_GROUP_LEADER).stream()
            .filter(o -> o.getDecision() != null).toList();
        if (leaderOpinions.size() < 2) {
            throw new IpdBusinessException("组长仲裁尚未形成两组对立意见，暂无需超管终裁");
        }
        String first = leaderOpinions.get(0).getDecision();
        if (leaderOpinions.stream().allMatch(o -> first.equals(o.getDecision()))) {
            throw new IpdBusinessException("组长仲裁已一致，无需超管终裁");
        }
        boolean already = arbitrationRows(gate, ROLE_SUPER_ADMIN).stream()
            .anyMatch(o -> actor.id().equals(o.getArbitratorId()));
        if (already) {
            throw new IpdBusinessException("您已提交本轮终裁意见，不可重复提交");
        }
        GateArbitration row = GateArbitration.builder()
            .gateId(gateId)
            .round(gate.getCurrentRound())
            .arbitratorType(ROLE_SUPER_ADMIN)
            .arbitratorId(actor.id())
            .decision(decision)
            .opinion(opinion)
            .build();
        arbitrationMapper.insert(row);
        audit(actor, gate, "GATE_FINAL_RULING",
            "超管终裁（终裁结果写入项目审计日志永久归档，AC-GATE-10）",
            "decision", decision, "round", gate.getCurrentRound());
        notifyBothPms(gate, NotificationService.Types.GATE_FINAL_RULING_RESULT,
            "Gate " + gate.getGateCode() + " 超管终裁：" + ("APPROVE".equals(decision) ? "支持通过" : "支持驳回"),
            "第 " + gate.getCurrentRound() + " 轮双PM分歧经组长仲裁未决，超管终裁意见已归档审计日志");
        return row;
    }

    // ==================== MEDIUM-1.3：Gate 列席人员邀请 ====================

    /** 列席人角色白名单。 */
    private static final Set<String> OBSERVER_ROLE_WHITELIST = OBSERVER_ROLES;

    /**
     * 邀请列席人员（MEDIUM-1.3）：仅超管/组长可邀请；
     * 同 gate+observer 唯一约束兜底幂等（重复邀请不报错，返回既有行）。
     *
     * @param gateId      Gate 实例
     * @param observerIds 列席人 personId 列表
     * @param role        列席角色（5 类之一）
     * @param actor       邀请人（必须 SUPER_ADMIN/GROUP_LEADER）
     * @return 邀请行数（去重后）
     */
    @Transactional(rollbackFor = Exception.class)
    public int inviteObservers(Long gateId, List<Long> observerIds, String role, IpdActor actor) {
        Gate gate = requireGate(gateId);
        if (!"SUPER_ADMIN".equals(actor.role()) && !"GROUP_LEADER".equals(actor.role())) {
            throw new IpdBusinessException("仅超管/产品组长可邀请列席人员");
        }
        if (role == null || !OBSERVER_ROLE_WHITELIST.contains(role)) {
            throw new IpdBusinessException("列席角色仅允许 SALES|SUPPLY|AFTERSALES|QUALITY|COMPLIANCE");
        }
        if (observerIds == null || observerIds.isEmpty()) {
            throw new IpdBusinessException("请选择至少 1 位列席人员");
        }
        Date now = new Date();
        int invited = 0;
        for (Long observerId : observerIds) {
            if (observerId == null) continue;
            // person 存在性校验
            Person observer = personMapper.selectById(observerId);
            if (observer == null) {
                throw new IpdBusinessException("列席人不存在：personId=" + observerId);
            }
            // 幂等：先查再插；uk(gate_id, observer_id) 兜底
            Long exist = observerMapper.selectCount(new LambdaQueryWrapper<GateReviewObserver>()
                .eq(GateReviewObserver::getGateId, gateId)
                .eq(GateReviewObserver::getObserverId, observerId));
            if (exist != null && exist > 0) {
                continue;
            }
            GateReviewObserver row = GateReviewObserver.builder()
                .gateId(gateId)
                .observerId(observerId)
                .role(role)
                .invitedBy(actor.id())
                .invitedAt(now)
                .attended(0)
                .build();
            observerMapper.insert(row);
            invited++;
            // 知会被邀请人
            notificationService.publish(observerId,
                NotificationService.Types.GATE_OBSERVER_INVITED, NotificationService.KIND_ACTION,
                "gate", gateId,
                "Gate " + gate.getGateCode() + " 邀请您列席",
                "您被邀请作为 " + role + " 角色列席 Gate " + gate.getGateCode()
                    + " 评审（MEDIUM-1.3），请提交列席意见",
                "/reviews/gate/" + gateId);
        }
        audit(actor, gate, "GATE_OBSERVER_INVITED",
            "邀请列席人员（" + role + "，" + invited + " 人）",
            "role", role, "count", invited);
        return invited;
    }

    /**
     * 列席人提交意见（MEDIUM-1.3）：仅 observer 本人可写自己的一行；
     * 不入主审投票（不动 gate_reviews）。
     */
    @Transactional(rollbackFor = Exception.class)
    public GateReviewObserver recordOpinion(Long gateId, Long observerId, String opinion, IpdActor actor) {
        Gate gate = requireGate(gateId);
        if (!actor.id().equals(observerId)) {
            throw new IpdBusinessException("仅本人可提交自己的列席意见（MEDIUM-1.3）");
        }
        if (opinion == null || opinion.isBlank()) {
            throw new IpdBusinessException("列席意见不能为空");
        }
        if (opinion.length() > 2000) {
            throw new IpdBusinessException("列席意见最长 2000 字符");
        }
        GateReviewObserver row = observerMapper.selectOne(new LambdaQueryWrapper<GateReviewObserver>()
            .eq(GateReviewObserver::getGateId, gateId)
            .eq(GateReviewObserver::getObserverId, observerId));
        if (row == null) {
            throw new IpdBusinessException("您未在 Gate " + gateId + " 的列席名单中");
        }
        row.setOpinion(opinion);
        row.setAttended(1);
        observerMapper.updateById(row);
        audit(actor, gate, "GATE_OBSERVER_OPINION",
            "列席人提交意见",
            "observerId", observerId, "opinionLength", opinion.length());
        return row;
    }

    /**
     * 查 gate 全部列席人员 + 意见（MEDIUM-1.3）：仅 PRODUCT_LEADER/GROUP_LEADER/SUPER_ADMIN 可见。
     */
    public List<GateReviewObserver> listObservers(Long gateId, IpdActor actor) {
        requireGate(gateId);
        String role = actor.role();
        if (!"GROUP_LEADER".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new IpdBusinessException("仅组长/超管可查询列席人员名单");
        }
        return observerMapper.selectList(new LambdaQueryWrapper<GateReviewObserver>()
            .eq(GateReviewObserver::getGateId, gateId)
            .orderByDesc(GateReviewObserver::getInvitedAt));
    }

    /** 延长签署期限（AC-GATE-21）：仅超管、仅在签 Gate；最多 3 次，第 4 次拒绝。 */
    @Transactional(rollbackFor = Exception.class)
    public Gate extendDeadline(Long gateId, int days, IpdActor actor) {
        Gate gate = requireGate(gateId);
        if (!ROLE_SUPER_ADMIN.equals(actor.role())) {
            throw new IpdBusinessException("仅超级管理员可延长签署期限（AC-GATE-21）");
        }
        if (!STATUS_PENDING.equals(gate.getStatus()) || gate.getStartedAt() == null) {
            throw new IpdBusinessException("仅签署中的 Gate 可延长签署期限");
        }
        if (days <= 0 || days > 30) {
            throw new IpdBusinessException("延长天数须为 1-30");
        }
        int count = gate.getSignExtensionCount() == null ? 0 : gate.getSignExtensionCount();
        if (count >= resolveMaxSignExtensions()) {
            throw new IpdBusinessException("签署期限最多延长 " + resolveMaxSignExtensions() + " 次，已达上限（AC-GATE-21）");
        }
        Date newDue = new Date(dueAtFrom(gate).getTime() + TimeUnit.DAYS.toMillis(days));
        gateMapper.update(null, new LambdaUpdateWrapper<Gate>()
            .eq(Gate::getId, gateId)
            .set(Gate::getSignDueAt, newDue)
            .set(Gate::getSignExtensionCount, count + 1));
        audit(actor, gate, "GATE_SIGN_EXTEND", "超管延长签署期限",
            "days", days, "count", count + 1, "newDueAt", newDue.toString());
        return requireGate(gateId);
    }

    /** 仲裁前置：调用者角色匹配 + Gate 已驳回且当轮双 PM 意见分歧（先 APPROVE 后 REJECT）。 */
    private void requireArbitratable(Gate gate, IpdActor actor, String role, String deniedMessage) {
        if (!role.equals(actor.role())) {
            throw new IpdBusinessException(deniedMessage);
        }
        if (!STATUS_REJECTED.equals(gate.getStatus())) {
            throw new IpdBusinessException("仅被驳回的 Gate 存在仲裁/终裁流程，当前：" + gate.getStatus());
        }
        if (!hasPmConflict(roundRows(gate.getId(), gate.getCurrentRound()))) {
            throw new IpdBusinessException("本轮无双 PM 意见分歧，无需仲裁");
        }
    }

    /** 双 PM 意见分歧 = 当轮同时存在 APPROVE 与 REJECT（先 A 后 R 序列）。 */
    private boolean hasPmConflict(List<GateReview> rows) {
        boolean approve = rows.stream().anyMatch(r -> SIGNER_ROLES.contains(r.getReviewerType())
            && "APPROVE".equals(r.getDecision()));
        boolean reject = rows.stream().anyMatch(r -> SIGNER_ROLES.contains(r.getReviewerType())
            && "REJECT".equals(r.getDecision()));
        return approve && reject;
    }

    /** 分歧自动开仲裁：审计开启 + 预落组长待裁行 + 邀请通知（AC-GATE-10 链起点）。
     * <p>工作台对偶（WB-17-1）：开仲裁即预落每位组长一条 decision=NULL 待裁行
     * （分配即落行，与 gate_reviews 预建占位行同构）；幂等由先查 + uk(gate_id, round, arbitrator_id) 兜底。 */
    private void openArbitration(Gate gate, IpdActor actor) {
        audit(actor, gate, "GATE_ARBITRATION_OPEN",
            "双PM意见分歧，自动发起组长仲裁（BR-GATE-06）", "round", gate.getCurrentRound());
        List<GateArbitration> existingRows = arbitrationRows(gate, ROLE_GROUP_LEADER);
        for (Person leader : collectLeaders(gate)) {
            boolean preallocated = existingRows.stream()
                .anyMatch(o -> leader.getId().equals(o.getArbitratorId()));
            if (!preallocated) {
                arbitrationMapper.insert(GateArbitration.builder()
                    .gateId(gate.getId())
                    .round(gate.getCurrentRound())
                    .arbitratorType(ROLE_GROUP_LEADER)
                    .arbitratorId(leader.getId())
                    .build()); // decision/opinion 留空 = 待裁（2026-09-08 ALTER 后可 NULL）
            }
            notificationService.publish(leader.getId(),
                NotificationService.Types.GATE_ARBITRATION_REQUEST, NotificationService.KIND_ACTION,
                "gate", gate.getId(),
                "Gate " + gate.getGateCode() + " 双PM意见分歧，请仲裁",
                "第 " + gate.getCurrentRound() + " 轮双方意见冲突，请提交仲裁意见（AC-GATE-10）",
                "/reviews/gate/" + gate.getId());
        }
    }

    /** 组长意见齐备后：一致 ⇒ SETTLED 审计 + 结果知会双方；
     * 不一致 ⇒ ESCALATED 审计 + 通知全部超管终裁（AC-GATE-10 "自动升级"）。 */
    private void maybeEscalateAfterArbitration(Gate gate, IpdActor actor) {
        List<Person> leaders = collectLeaders(gate);
        if (leaders.isEmpty()) {
            return;
        }
        // 只统计已裁行：预落的 decision=NULL 待裁行不算「已提交」
        List<GateArbitration> opinions = arbitrationRows(gate, ROLE_GROUP_LEADER).stream()
            .filter(o -> o.getDecision() != null).toList();
        boolean allOpined = leaders.stream().noneMatch(l -> opinions.stream()
            .noneMatch(o -> l.getId().equals(o.getArbitratorId())));
        if (!allOpined) {
            return; // 还有组长未提交
        }
        String first = opinions.get(0).getDecision();
        if (opinions.stream().allMatch(o -> first.equals(o.getDecision()))) {
            audit(actor, gate, "GATE_ARBITRATION_SETTLED",
                "组长仲裁一致：" + first, "round", gate.getCurrentRound());
            notifyBothPms(gate, NotificationService.Types.GATE_ARBITRATION_RESULT,
                "Gate " + gate.getGateCode() + " 仲裁结果：" + ("APPROVE".equals(first) ? "支持通过" : "支持驳回"),
                "第 " + gate.getCurrentRound() + " 轮双PM分歧经组长仲裁达成一致意见");
        } else {
            audit(actor, gate, "GATE_ARBITRATION_ESCALATED",
                "两组长意见不一致，自动升级超管终裁（BR-GATE-06）", "round", gate.getCurrentRound());
            for (Person admin : superAdmins()) {
                notificationService.publish(admin.getId(),
                    NotificationService.Types.GATE_FINAL_RULING_REQUEST, NotificationService.KIND_ACTION,
                    "gate", gate.getId(),
                    "Gate " + gate.getGateCode() + " 两组长仲裁不一致，请终裁",
                    "第 " + gate.getCurrentRound() + " 轮双PM分歧升级至超管终裁（AC-GATE-10），请提交终裁意见",
                    "/reviews/gate/" + gate.getId());
            }
        }
    }

    /** 弃权行（decision=ABSTAIN，signedAt=null 表示未实际签署；AC-GATE-08）。 */
    private void insertAbstain(Gate gate, String side, Date due) {
        Long personId = signerPersonId(gate, side);
        if (personId == null) {
            throw new IpdBusinessException("签署方在册成员缺失，无法标记弃权：" + side
                + "（projectId=" + gate.getProjectId() + "）");
        }
        reviewMapper.insert(GateReview.builder()
            .gateId(gate.getId())
            .reviewerType(side)
            .reviewerId(personId)
            .decision("ABSTAIN")
            .opinion("超期未签署，自动弃权（BR-GATE-04 / AC-GATE-08）")
            .dueAt(due)
            .round(gate.getCurrentRound())
            .build());
    }

    /** 超时流转落终态 + 审计（弃权事件/按主导方执行各自留痕）。 */
    private void settleTimeout(Gate gate, String status, IpdActor operator, String reason) {
        gateMapper.update(null, new LambdaUpdateWrapper<Gate>()
            .eq(Gate::getId, gate.getId())
            .set(Gate::getStatus, status));
        audit(operator, gate, STATUS_APPROVED.equals(status) ? "GATE_APPROVE" : "GATE_ABSTAIN_TIMEOUT",
            reason, "round", gate.getCurrentRound());
    }

    /** 双 PM 各自的产品组长（在册双 PM → 所在组 → 组长），按人去重；
     * 成员/人员绑定缺失时退化为空列表（仲裁链静默不触发，不阻塞主流程）。 */
    private List<Person> collectLeaders(Gate gate) {
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, gate.getProjectId())
            .in(ProjectMember::getRole, List.of("MARKET_PM", "RD_PM"))
            .isNull(ProjectMember::getExitDate));
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Long> pmIds = members.stream().map(ProjectMember::getPersonId).distinct().toList();
        List<Person> pms = personMapper.selectBatchIds(pmIds);
        if (pms == null || pms.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = pms.stream().map(Person::getGroupId)
            .filter(Objects::nonNull).distinct().toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return personMapper.selectList(new LambdaQueryWrapper<Person>()
            .eq(Person::getPersonType, ROLE_GROUP_LEADER)
            .in(Person::getGroupId, groupIds));
    }

    /** 某签署角色在本项目的在册人（project_members 首个命中；缺失返回 null）。 */
    private Long signerPersonId(Gate gate, String role) {
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, gate.getProjectId())
            .eq(ProjectMember::getRole, role)
            .isNull(ProjectMember::getExitDate));
        return members == null || members.isEmpty() ? null : members.get(0).getPersonId();
    }

    /** 全部超管（persons.personType=SUPER_ADMIN）。 */
    private List<Person> superAdmins() {
        return personMapper.selectList(new LambdaQueryWrapper<Person>()
            .eq(Person::getPersonType, ROLE_SUPER_ADMIN));
    }

    /** 知会双方 PM（弃权流转/仲裁/终裁结果；在册成员缺失时静默跳过）。 */
    private void notifyBothPms(Gate gate, String eventType, String title, String content) {
        for (String role : List.of("MARKET_PM", "RD_PM")) {
            Long personId = signerPersonId(gate, role);
            if (personId != null) {
                notificationService.publish(personId, eventType, NotificationService.KIND_ACTION,
                    "gate", gate.getId(), title, content, "/reviews/gate/" + gate.getId());
            }
        }
    }

    /** 本轮某类型的仲裁意见行。 */
    private List<GateArbitration> arbitrationRows(Gate gate, String arbitratorType) {
        return arbitrationMapper.selectList(new LambdaQueryWrapper<GateArbitration>()
            .eq(GateArbitration::getGateId, gate.getId())
            .eq(GateArbitration::getRound, gate.getCurrentRound())
            .eq(GateArbitration::getArbitratorType, arbitratorType));
    }

    private void audit(IpdActor actor, Gate gate, String action, String reason, Object... pairs) {
        auditLogService.append(AuditLog.builder()
            .action(action)
            .entityType("gates")
            .entityId(gate.getId())
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .afterData(pairs.length == 0 ? null : AuditEventData.json(pairs))
            .reason(reason)
            .createTime(new Date())
            .build());
    }
}
