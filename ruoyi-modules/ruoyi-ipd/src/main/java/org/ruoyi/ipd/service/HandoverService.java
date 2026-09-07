package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.springframework.transaction.PlatformTransactionManager;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 单项目角色移交（P2-7.1；AC-HAND-01c/01d/03/06；BR-HAND-01/04、BR-USER-06）。
 *
 * <p>口径：
 * <ul>
 *   <li>AC-HAND-01c：产品组长/超管代已离职或冻结人员一键移交——从在任绑定反查原负责人，
 *       发起即接受即完成（解决"人已走无法登录移交"）</li>
 *   <li>AC-HAND-01d：名下项目全部移交完成 ⇒ 自动 DISABLED + 企微解绑；
 *       禁止登录由 IpdAuthService 的 DISABLED→NONE 兜底（P0-7.x 已验）</li>
 *   <li>AC-HAND-03：代办发起即把在任账号置 FROZEN_PENDING_HANDOVER（移交完成前冻结窗口，
 *       仅保留移交相关权限）；HTTP 层读/写分离映射超出本卡 PATHS，验收登记 PARTIAL</li>
 *   <li>AC-HAND-06：按 handoverRole 维度移交，只动目标角色绑定，另一侧 PM 不受影响</li>
 *   <li>接手绑定复用 {@link ProjectMemberService#bindMember} 五参重载：
 *       重复/超项备案/角色互斥/津贴快照校验全量继承（与 AC-TEAM-11 链贯通）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class HandoverService {

    /** 与 ProjectMemberService.ROLES 同源：移交只在这两个角色维度。 */
    private static final Set<String> HANDOVER_ROLES = Set.of("MARKET_PM", "RD_PM");

    /** 状态机适配既有 DDL 枚举（DRAFT|CONFIRMED|COMPLETED|ROLLED_BACK），本卡用 DRAFT→COMPLETED→ROLLED_BACK。 */
    private static final String ST_DRAFT = "DRAFT";
    private static final String ST_COMPLETED = "COMPLETED";
    /** HIGH-3.1：撤销终态—COMPLETED → ROLLED_BACK；显式终态，不再转回。 */
    private static final String ST_ROLLED_BACK = "ROLLED_BACK";

    /** HIGH-3.1：撤销窗口（COMPLETED 完成后多少小时可被撤销）。 */
    private static final long ROLLBACK_WINDOW_HOURS = 24L;

    /** ZK-IPD 页49 原型：超管移交二次确认短语（原型按钮 disabled 直至输入与该短语一致）。 */
    private static final String CONFIRM_PHRASE = "确认移交管理员";

    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final ProjectMapper projectMapper;
    private final HandoverMapper handoverMapper;
    private final AuditLogService auditLogService;
    private final ProjectMemberService projectMemberService;
    private final PlatformTransactionManager transactionManager;

    /** 本人发起移交（DRAFT，等待接手人 accept）。 */
    public HandoverRecord initiate(Long projectId, String role, Long toPersonId, String note, IpdActor operator) {
        return createDraft(projectId, role, operator.id(), toPersonId, note, operator);
    }

    /**
     * AC-HAND-01c：产品组长/超管代已离职或冻结人员一键移交——发起即接受即完成。
     *
     * <p>原负责人从该项目该角色的在任绑定反查（人已走无法登录，不信任前端传参）；
     * 仅接受 employmentStatus=RESIGNED 或 accountStatus=FROZEN_PENDING_HANDOVER 的代办对象。
     * approvalRef：接手后达到项目数上限阈值时必填（AC-TEAM-11，与普通移交同规则）。
     */
    public HandoverRecord initiateOnBehalf(Long projectId, String role, Long toPersonId,
                                           String note, String approvalRef, IpdActor leader) {
        // SEC-REV-HANDOVER-01：入口加项目归属校验——SUPER_ADMIN 豁免；其他角色必须
        // leader.groupId == project.mainGroupId（横向越权防护，BR-IPD-02/R8X-CONT-1 同款语义）。
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        assertSameGroup(leader.role(), leader.groupId(), project.getMainGroupId(), "代移交项目");
        ProjectMember current = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getRole, role)
            .isNull(ProjectMember::getExitDate)
            .orderByAsc(ProjectMember::getId)
            .last("LIMIT 1"));
        if (current == null) {
            throw new ServiceException("该项目该角色无在任成员，无从移交");
        }
        Long fromId = current.getPersonId();
        Person from = personMapper.selectById(fromId);
        if (from == null) {
            throw new ServiceException("原负责人不存在: " + fromId);
        }
        boolean gone = "RESIGNED".equals(from.getEmploymentStatus())
            || "FROZEN_PENDING_HANDOVER".equals(from.getAccountStatus());
        if (!gone) {
            throw new ServiceException("一键代办仅适用于离职或冻结待移交人员；在任人员应自行发起移交");
        }
        freezeForHandover(fromId);
        HandoverRecord rec = createDraft(projectId, role, fromId, toPersonId, note, leader);
        return doAccept(rec, approvalRef, leader);
    }

    /**
     * 接手人确认接受：DRAFT → COMPLETED，原子转移角色绑定并联动账号禁用检查。
     */
    public HandoverRecord accept(Long handoverId, String approvalRef, IpdActor recipient) {
        HandoverRecord rec = handoverMapper.selectById(handoverId);
        if (rec == null) {
            throw new ServiceException("移交记录不存在: " + handoverId);
        }
        if (!ST_DRAFT.equals(rec.getStatus())) {
            throw new ServiceException("移交已处理（状态 " + rec.getStatus() + "），不可重复处理");
        }
        if (!recipient.id().equals(rec.getToPersonId())) {
            throw new ServiceException("仅接手人本人可确认移交");
        }
        return doAccept(rec, approvalRef, recipient);
    }

    /** 收件箱：待我接收或我发起的未完结移交。 */
    public List<HandoverRecord> inbox(IpdActor me) {
        return handoverMapper.selectList(new LambdaQueryWrapper<HandoverRecord>()
            .and(w -> w.eq(HandoverRecord::getToPersonId, me.id())
                .or().eq(HandoverRecord::getFromPersonId, me.id()))
            .ne(HandoverRecord::getStatus, ST_COMPLETED)
            .orderByAsc(HandoverRecord::getId));
    }

    // ---------- P2-7.2 批量移交与失败补偿 ----------

    /** 批量移交逐项目结果：COMPLETED / REJECTED（reason 必填）/ SKIPPED_ALREADY_HANDED_OVER。 */
    public record BatchHandoverResult(Long projectId, String status, String reason) { }

    /**
     * AC-HAND-04（P2-7.2）：组长/超管代离职人按角色批量移交名下项目。
     *
     * <p>口径：
     * <ul>
     *   <li>逐项目结果明确：返回每项目 COMPLETED/REJECTED/SKIPPED 及原因</li>
     *   <li>失败项目保持原归属：单项目在独立事务中执行，拒绝/异常仅回滚该项目，不影响其余</li>
     *   <li>重试不重复成功项：from 在该项目该角色已无活跃绑定 ⇒ SKIPPED_ALREADY_HANDED_OVER，不重写</li>
     *   <li>不得提前禁用：disableIfAllCleared 仅在真全清（活跃绑定计数=0）时禁用，
     *       批量中间态因余留绑定自然跳过（配合下方修正版计数语义）</li>
     *   <li>AC-HAND-04 历史跟随：只动 project_members / handover_records，项目本体、
     *   审计、Gate、台账记录零删除零改写，新 PM 在原项目上接续</li>
     * </ul>
     *
     * <p>事务：方法级 NOT_SUPPORTED 挂起类级事务；逐项目用 TransactionTemplate 独立事务，
     * 内层 {@link #initiateOnBehalf}（REQUIRED）加入，异常标 rollback-only 由模板回滚该项目全部写入。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<BatchHandoverResult> batchHandover(Long fromPersonId, String role, Long toPersonId,
                                                   String note, String approvalRef,
                                                   List<Long> projectIds, IpdActor leader) {
        if (!HANDOVER_ROLES.contains(role)) {
            throw new ServiceException("角色非法: " + role);
        }
        if (fromPersonId == null || fromPersonId.equals(toPersonId)) {
            throw new ServiceException("原负责人缺失或与接手人相同");
        }
        List<Long> targets = (projectIds == null || projectIds.isEmpty())
            ? memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getPersonId, fromPersonId)
                    .eq(ProjectMember::getRole, role)
                    .isNull(ProjectMember::getExitDate))
                .stream().map(ProjectMember::getProjectId).distinct().toList()
            : projectIds;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<BatchHandoverResult> results = new ArrayList<>(targets.size());
        for (Long projectId : targets) {
            results.add(handOverOne(tx, projectId, fromPersonId, role, toPersonId, note, approvalRef, leader));
        }
        return results;
    }

    /** 单项目批量单元：幂等跳过/归属校验在事务外只读，写路径整项目独立事务。 */
    private BatchHandoverResult handOverOne(TransactionTemplate tx, Long projectId, Long fromPersonId,
                                            String role, Long toPersonId, String note,
                                            String approvalRef, IpdActor leader) {
        try {
            // 重试幂等：from 在该项目该角色已无活跃绑定 ⇒ 上轮已成功，跳过不重写
            Long active = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getPersonId, fromPersonId)
                .eq(ProjectMember::getRole, role)
                .isNull(ProjectMember::getExitDate));
            if (active == null || active == 0) {
                return new BatchHandoverResult(projectId, "SKIPPED_ALREADY_HANDED_OVER", "上轮移交已成功，重试跳过");
            }
            // 指定 from 语义：在任绑定者必须就是指定原负责人（否则逐项拒绝，保持原归属）
            ProjectMember current = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getRole, role)
                .isNull(ProjectMember::getExitDate)
                .orderByAsc(ProjectMember::getId)
                .last("LIMIT 1"));
            if (current == null) {
                return new BatchHandoverResult(projectId, "REJECTED", "该项目该角色无在任成员");
            }
            if (!fromPersonId.equals(current.getPersonId())) {
                return new BatchHandoverResult(projectId, "REJECTED",
                    "该项目该角色在任成员(" + current.getPersonId() + ")与指定原负责人不符，保持原归属");
            }
            tx.executeWithoutResult(st -> initiateOnBehalf(projectId, role, toPersonId, note, approvalRef, leader));
            return new BatchHandoverResult(projectId, "COMPLETED", null);
        } catch (Exception e) {
            return new BatchHandoverResult(projectId, "REJECTED", e.getMessage());
        }
    }

    // ---------- 内部 ----------

    private HandoverRecord createDraft(Long projectId, String role, Long fromId, Long toPersonId,
                                       String note, IpdActor operator) {
        if (!HANDOVER_ROLES.contains(role)) {
            throw new ServiceException("角色非法: " + role);
        }
        if (fromId.equals(toPersonId)) {
            throw new ServiceException("接手人不能与原负责人相同");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        Person to = personMapper.selectById(toPersonId);
        if (to == null) {
            throw new ServiceException("接手人不存在: " + toPersonId);
        }
        if ("RESIGNED".equals(to.getEmploymentStatus()) || "DISABLED".equals(to.getAccountStatus())) {
            throw new ServiceException("接手人已离职/禁用，不可承接: " + to.getName());
        }
        if (!role.equals(to.getPersonType())) {
            throw new ServiceException("角色不匹配：接手人类型 " + to.getPersonType() + " 不可承接 " + role);
        }
        Long dup = handoverMapper.selectCount(new LambdaQueryWrapper<HandoverRecord>()
            .eq(HandoverRecord::getProjectId, projectId)
            .eq(HandoverRecord::getHandoverRole, role)
            .eq(HandoverRecord::getStatus, ST_DRAFT));
        if (dup != null && dup > 0) {
            throw new ServiceException("该项目该角色已有进行中的移交，不可重复发起");
        }
        HandoverRecord rec = HandoverRecord.builder()
            .handoverType("PROJECT")
            .fromPersonId(fromId)
            .toPersonId(toPersonId)
            .projectId(projectId)
            .handoverRole(role)
            .note(note)
            .status(ST_DRAFT)
            .build();
        handoverMapper.insert(rec);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("HANDOVER_CREATE").entityType("handover").entityId(rec.getId())
            .reason("projectId=" + projectId + " role=" + role + " from=" + fromId)
            .afterData(AuditEventData.json("toPersonId", toPersonId, "handoverRole", role,
                "onBehalf", !operator.id().equals(fromId)))
            .createTime(new Date())
            .build());
        return rec;
    }

    /** 原子转移：旧绑定退出 → 接手绑定（bindMember 全量校验）→ 记录完结 → 审计 → 全清禁用检查。 */
    private HandoverRecord doAccept(HandoverRecord rec, String approvalRef, IpdActor operator) {
        int exited = projectMemberService.exitForHandover(
            rec.getProjectId(), rec.getFromPersonId(), rec.getHandoverRole());
        if (exited == 0) {
            throw new ServiceException("原负责人在该项目已无此角色在任绑定，移交中止");
        }
        ProjectMember bound = projectMemberService.bindMember(
            rec.getProjectId(), rec.getToPersonId(), rec.getHandoverRole(), approvalRef, operator);
        Date now = new Date();
        rec.setStatus(ST_COMPLETED);
        if (rec.getConfirmedAt() == null) {
            rec.setConfirmedAt(now);
        }
        rec.setCompletedAt(now);
        handoverMapper.updateById(rec);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("HANDOVER_ACCEPT").entityType("handover").entityId(rec.getId())
            .reason("projectId=" + rec.getProjectId() + " role=" + rec.getHandoverRole())
            .afterData(AuditEventData.json("fromPersonId", rec.getFromPersonId(),
                "toPersonId", rec.getToPersonId(), "newMemberId", bound.getId()))
            .createTime(now)
            .build());
        disableIfAllCleared(rec.getFromPersonId(), operator);
        return rec;
    }

    /** AC-HAND-03：代办移交把在任账号置冻结（幂等：仅 ACTIVE→FROZEN_PENDING_HANDOVER）。 */
    private void freezeForHandover(Long personId) {
        personMapper.update(null, new LambdaUpdateWrapper<Person>()
            .eq(Person::getId, personId)
            .eq(Person::getAccountStatus, "ACTIVE")
            .set(Person::getAccountStatus, "FROZEN_PENDING_HANDOVER"));
    }

    /**
     * HIGH-3.1：撤销已接受移交（COMPLETED → ROLLED_BACK 终态）。
     *
     * <p>口径：
     * <ul>
     *   <li>状态机：仅 status=COMPLETED 可撤销；撤销后 status=ROLLED_BACK 显式终态，幂等拒重复</li>
     *   <li>窗口：completedAt 在 ROLLBACK_WINDOW_HOURS（24h）内，超窗抛 HANDOVER_LOCKED</li>
     *   <li>权限：发起人本人 OR 项目主组组长 OR SUPER_ADMIN（继承 initiateOnBehalf 同款同组校验）</li>
     *   <li>副作用反转：接手人 exit + 发起人 exit_date/exit_reason 复位；
     *       不重 bindMember 五参重载（避免再走评级快照/津贴锁定），直接 update 复位</li>
     *   <li>审计：HANDOVER_ROLLBACK；写 rollbackReason + rollbackAt；同事务强一致</li>
     * </ul>
     *
     * <p>事务：方法级 REQUIRED 加入类级事务；与 disableIfAllCleared 不冲突（撤销不触发禁用检查）。
     */
    public HandoverRecord rollback(Long handoverId, String reason, IpdActor actor) {
        HandoverRecord rec = handoverMapper.selectById(handoverId);
        if (rec == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "移交记录不存在: " + handoverId);
        }
        // 幂等：已撤销直接拒（终态不再接受任何操作）
        if (ST_ROLLED_BACK.equals(rec.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.HANDOVER_LOCKED,
                "移交已撤销（状态 ROLLED_BACK），不可重复撤销");
        }
        // 状态机：仅 COMPLETED 可撤销
        if (!ST_COMPLETED.equals(rec.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.HANDOVER_LOCKED,
                "移交记录状态不允许撤销（当前 " + rec.getStatus() + "，仅 COMPLETED 可撤销）");
        }
        // 窗口：completedAt + 24h 仍允许撤销
        Date completedAt = rec.getCompletedAt();
        if (completedAt == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.HANDOVER_LOCKED,
                "移交完成时间缺失，禁止撤销（HANDOVER_LOCKED）");
        }
        long hoursSince = (System.currentTimeMillis() - completedAt.getTime()) / 3_600_000L;
        if (hoursSince > ROLLBACK_WINDOW_HOURS) {
            throw new IpdBusinessException(ApiV1ErrorCode.HANDOVER_LOCKED,
                "已完成超过 " + ROLLBACK_WINDOW_HOURS + "h，禁止撤销（HANDOVER_LOCKED）");
        }
        // 权限：发起人 OR 项目主组组长 OR SUPER_ADMIN
        boolean isInitiator = actor.id().equals(rec.getFromPersonId())
            || actor.id().equals(rec.getToPersonId());
        boolean isLeaderOrAdmin = "SUPER_ADMIN".equals(actor.role());
        if (!isLeaderOrAdmin) {
            Project project = projectMapper.selectById(rec.getProjectId());
            if (project != null && "GROUP_LEADER".equals(actor.role())
                && actor.groupId() != null && actor.groupId().equals(project.getMainGroupId())) {
                isLeaderOrAdmin = true;
            }
        }
        if (!isInitiator && !isLeaderOrAdmin) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN,
                "仅移交发起人、项目组长或超管可撤销移交");
        }
        // 副作用反转：接手人 exit + 发起人 exit_date/exit_reason 复位
        restoreForRollback(rec);
        Date now = new Date();
        rec.setStatus(ST_ROLLED_BACK);
        rec.setRollbackReason(reason);
        rec.setRollbackAt(now);
        handoverMapper.updateById(rec);
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id()).operatorName(actor.name()).operatorRole(actor.role())
            .action("HANDOVER_ROLLBACK").entityType("handover").entityId(rec.getId())
            .reason("projectId=" + rec.getProjectId() + " role=" + rec.getHandoverRole()
                + " rollback=" + (reason == null ? "" : reason))
            .afterData(AuditEventData.json("fromPersonId", rec.getFromPersonId(),
                "toPersonId", rec.getToPersonId(),
                "rollbackReason", reason,
                "rollbackWindowHours", ROLLBACK_WINDOW_HOURS,
                "hoursSinceCompleted", hoursSince))
            .createTime(now)
            .build());
        return rec;
    }

    /**
     * HIGH-3.1：撤销时反转副作用——接手人退出 + 发起人绑定恢复。
     *
     * <p>不调用 bindMember 五参重载（避开评级快照 / 津贴锁定 / 项目数上限阈值校验的副作用），
     * 直接 update 复位原绑定行的 exit_date/exit_reason。
     */
    private void restoreForRollback(HandoverRecord rec) {
        // 1) 接手人退出（accept 时由 bindMember 插入，本次撤销退出）
        int exited = memberMapper.update(null, new LambdaUpdateWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, rec.getProjectId())
            .eq(ProjectMember::getPersonId, rec.getToPersonId())
            .eq(ProjectMember::getRole, rec.getHandoverRole())
            .isNull(ProjectMember::getExitDate)
            .set(ProjectMember::getExitDate, new Date())
            .set(ProjectMember::getExitReason, "HANDOVER_ROLLBACK"));
        if (exited == 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "接手人在该项目该角色已无在任绑定，撤销失败（副作用不可逆）");
        }
        // 2) 发起人绑定恢复：原 exit_date/exit_reason=HANDOVER 的绑定行复位
        int restored = memberMapper.update(null, new LambdaUpdateWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, rec.getProjectId())
            .eq(ProjectMember::getPersonId, rec.getFromPersonId())
            .eq(ProjectMember::getRole, rec.getHandoverRole())
            .eq(ProjectMember::getExitReason, "HANDOVER")
            .isNotNull(ProjectMember::getExitDate)
            .set(ProjectMember::getExitDate, null)
            .set(ProjectMember::getExitReason, null));
        if (restored == 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "发起人在该项目该角色无可恢复的 HANDOVER 绑定行，撤销失败");
        }
    }

    /**
     * ZK-IPD §九：超管权限移交专属路径
     * <ul>
     *   <li>移交完成后原超管账号作废失效（accountStatus=DISABLED + wecom 解绑）</li>
     *   <li>新超管 personType 提升为 SUPER_ADMIN</li>
     *   <li>二次确认：confirmation 必须与页49 原型确认短语一致（防误触，后端强制）</li>
     *   <li>旧会话即失效：DISABLED 后由 IpdAuthService.scopeOf→NONE + IpdPermission 401
     *   每请求实时兜底（P0-7.x 已验，登录侧 DISABLED 同拒）</li>
     *   <li>全程审计 SUPER_ADMIN_TRANSFER</li>
     * </ul>
     * 与普通 PM 移交的区别：超管不在任何项目上做 PM 绑定，disableIfAllCleared 不适用；
     * 必须显式把原超管置 DISABLED、不可登录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferSuperAdmin(Long toPersonId, String note, String confirmation, IpdActor operator) {
        if (operator == null || !"SUPER_ADMIN".equals(operator.role())) {
            throw new ServiceException("ZK-IPD §九：仅超管本人可发起超管权限移交");
        }
        if (!CONFIRM_PHRASE.equals(confirmation)) {
            throw new ServiceException("确认短语不匹配，二次确认未通过（须输入：" + CONFIRM_PHRASE + "）");
        }
        if (toPersonId == null) {
            throw new ServiceException("ZK-IPD §九：接手人 ID 不能为空");
        }
        if (toPersonId.equals(operator.id())) {
            throw new ServiceException("接手人不能与原负责人相同");
        }
        // 找当前在任超管（页49 契约：系统始终只有一名活动超管；多名则为违例存量，拒绝移交并提示收敛）
        List<Person> admins = personMapper.selectList(
            new LambdaQueryWrapper<Person>()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .eq(Person::getAccountStatus, "ACTIVE"));
        if (admins.isEmpty()) {
            throw new ServiceException("ZK-IPD §九：当前无在任超管（系统异常）");
        }
        if (admins.size() > 1) {
            throw new ServiceException("ZK-IPD §九：检测到 " + admins.size() + " 名在任超管（违反单超管不变式），须先收敛至一名再移交");
        }
        Person currentAdmin = admins.get(0);
        if (currentAdmin.getId().equals(toPersonId)) {
            throw new ServiceException("接手人不能与原负责人相同");
        }
        Person newAdmin = personMapper.selectById(toPersonId);
        if (newAdmin == null) {
            throw new ServiceException("接手人不存在: " + toPersonId);
        }
        if ("RESIGNED".equals(newAdmin.getEmploymentStatus()) || "DISABLED".equals(newAdmin.getAccountStatus())) {
            throw new ServiceException("接手人已离职/禁用，不可承接超管权限: " + newAdmin.getName());
        }
        Date now = new Date();
        // 1) 原超管 → DISABLED + 企微解绑（作废失效）
        personMapper.update(null, new LambdaUpdateWrapper<Person>()
            .eq(Person::getId, currentAdmin.getId())
            .set(Person::getAccountStatus, "DISABLED")
            .set(Person::getWecomUserId, null)
            .set(Person::getWecomBoundAt, null));
        // 2) 新人 → SUPER_ADMIN
        newAdmin.setPersonType("SUPER_ADMIN");
        personMapper.updateById(newAdmin);
        // 3) 审计：SUPER_ADMIN_TRANSFER
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("SUPER_ADMIN_TRANSFER").entityType("person").entityId(currentAdmin.getId())
            .reason("超管权限移交：" + currentAdmin.getName() + " → " + newAdmin.getName()
                + (note == null || note.isBlank() ? "" : "（" + note + "）"))
            .afterData(AuditEventData.json("fromPersonId", currentAdmin.getId(),
                "fromPersonName", currentAdmin.getName(),
                "toPersonId", newAdmin.getId(),
                "toPersonName", newAdmin.getName(),
                "originalAccountStatus", "DISABLED",
                "wecomUnbound", true))
            .createTime(now)
            .build());
    }

    /**
     * AC-HAND-01d：名下项目全部移交完成 ⇒ DISABLED + 企微解绑。
     * 未全清（名下还有活跃项目）则保持现状（冻结的保持冻结）。
     *
     * <p>SEC-REV-HANDOVER-02（P2-7.3 复核修正版）：FOR UPDATE 锁定该 person 全部活跃绑定行后
     * 计数——保留原子修订的 TOCTOU 防护意图（行锁串行化并发移交/新增绑定），但语义回归
     * 「仅真全清（活跃绑定计数=0）才禁用」。此前的原子 UPDATE 版会把余留绑定一并置退出
     * （updated&gt;0 才继续禁用），名下多项目时提前退出待移交项目绑定并禁用账号，与
     * AC-HAND-01d「全部移交完成才 DISABLED」及 P2-7.2「不得提前禁用仍有待移交人员」相反。
     * person 侧 DISABLED 再加 accountStatus=ACTIVE 条件守卫：并发双过计数窗口内仅一人
     * update 生效（affected=1），后到者 affected=0 直接返回不重复审计。
     *
     * <p>package-private（无 private）便于测试直接调用；不暴露给 controller/service。
     */
    void disableIfAllCleared(Long personId, IpdActor operator) {
        List<ProjectMember> remaining = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getPersonId, personId)
            .isNull(ProjectMember::getExitDate)
            .last("FOR UPDATE"));
        if (!remaining.isEmpty()) {
            return;
        }
        Person p = personMapper.selectById(personId);
        if (p == null || "DISABLED".equals(p.getAccountStatus())) {
            return;
        }
        // updateById 不落 null 字段：企微解绑必须显式 set null；ACTIVE 守卫防并发重复禁用/审计
        int disabled = personMapper.update(null, new LambdaUpdateWrapper<Person>()
            .eq(Person::getId, personId)
            .eq(Person::getAccountStatus, "ACTIVE")
            .set(Person::getAccountStatus, "DISABLED")
            .set(Person::getWecomUserId, null)
            .set(Person::getWecomBoundAt, null));
        if (disabled == 0) {
            return;
        }
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("ACCOUNT_DISABLED_AFTER_HANDOVER").entityType("person").entityId(personId)
            .reason("名下项目全部移交完成（BR-USER-06 先移交后禁用）")
            .afterData(AuditEventData.json("accountStatus", "DISABLED", "wecomUnbound", true))
            .createTime(new Date())
            .build());
    }

    /**
     * SEC-REV-HANDOVER-01：横向越权防护——SUPER_ADMIN 一律通过；
     * 其他角色必须 actor.groupId == project.mainGroupId。
     * 语义同 ProjectService.assertSameGroup（避免跨 service 依赖）。
     */
    private void assertSameGroup(String actorRole, Long actorGroupId, Long objectGroupId, String roleLabel) {
        if ("SUPER_ADMIN".equals(actorRole)) {
            return;
        }
        if (actorGroupId == null || !actorGroupId.equals(objectGroupId)) {
            throw new ServiceException(roleLabel + "必须归属项目主组（横向越权防护）");
        }
    }
}
