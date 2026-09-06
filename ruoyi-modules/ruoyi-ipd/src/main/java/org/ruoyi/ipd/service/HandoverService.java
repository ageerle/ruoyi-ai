package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
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
import org.springframework.transaction.annotation.Transactional;

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

    /** 状态机适配既有 DDL 枚举（DRAFT|CONFIRMED|COMPLETED），本卡只用 DRAFT→COMPLETED。 */
    private static final String ST_DRAFT = "DRAFT";
    private static final String ST_COMPLETED = "COMPLETED";

    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final ProjectMapper projectMapper;
    private final HandoverMapper handoverMapper;
    private final AuditLogService auditLogService;
    private final ProjectMemberService projectMemberService;

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
     * ZK-IPD §九：超管权限移交专属路径
     * <ul>
     *   <li>移交完成后原超管账号作废失效（accountStatus=DISABLED + wecom 解绑）</li>
     *   <li>新超管 personType 提升为 SUPER_ADMIN</li>
     *   <li>二次确认（操作人必须本身是超管）</li>
     *   <li>全程审计 SUPER_ADMIN_TRANSFER</li>
     * </ul>
     * 与普通 PM 移交的区别：超管不在任何项目上做 PM 绑定，disableIfAllCleared 不适用；
     * 必须显式把原超管置 DISABLED、不可登录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferSuperAdmin(Long toPersonId, String note, IpdActor operator) {
        if (operator == null || !"SUPER_ADMIN".equals(operator.role())) {
            throw new ServiceException("ZK-IPD §九：仅超管本人可发起超管权限移交");
        }
        if (toPersonId == null) {
            throw new ServiceException("ZK-IPD §九：接手人 ID 不能为空");
        }
        if (toPersonId.equals(operator.id())) {
            throw new ServiceException("接手人不能与原负责人相同");
        }
        // 找当前在任超管
        Person currentAdmin = personMapper.selectList(
            new LambdaQueryWrapper<Person>()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .eq(Person::getAccountStatus, "ACTIVE")
                .last("LIMIT 1"))
            .stream().findFirst().orElse(null);
        if (currentAdmin == null) {
            throw new ServiceException("ZK-IPD §九：当前无在任超管（系统异常）");
        }
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
     */
    private void disableIfAllCleared(Long personId, IpdActor operator) {
        Long remaining = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getPersonId, personId)
            .isNull(ProjectMember::getExitDate));
        if (remaining != null && remaining > 0) {
            return;
        }
        Person p = personMapper.selectById(personId);
        if (p == null || "DISABLED".equals(p.getAccountStatus())) {
            return;
        }
        // updateById 不落 null 字段：企微解绑必须显式 set null
        personMapper.update(null, new LambdaUpdateWrapper<Person>()
            .eq(Person::getId, personId)
            .set(Person::getAccountStatus, "DISABLED")
            .set(Person::getWecomUserId, null)
            .set(Person::getWecomBoundAt, null));
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("ACCOUNT_DISABLED_AFTER_HANDOVER").entityType("person").entityId(personId)
            .reason("名下项目全部移交完成（BR-USER-06 先移交后禁用）")
            .afterData(AuditEventData.json("accountStatus", "DISABLED", "wecomUnbound", true))
            .createTime(new Date())
            .build());
    }
}
