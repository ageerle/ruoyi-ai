package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.service.NotificationService.Types;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 人员账户状态联动（P2-1.3；AC-USER-08/09/10；BR-USER-05/06）。
 *
 * <p>口径：
 * <ul>
 *   <li>AC-USER-08：离职冻结（resign）— employment_status→RESIGNED + account_status→FROZEN_PENDING_HANDOVER；
 *       名下未移交项目成员绑定全部冻结标记为"待移交"，禁止登录由 IpdAuthService 的状态机兜底</li>
 *   <li>AC-USER-09：复职（rehire）— employment_status→ACTIVE + account_status→ACTIVE；仅接受
 *       当前状态为 RESIGNED 的对象；groupId/level 不变（level 由 HR API 同步，非本卡权限）</li>
 *   <li>AC-USER-10：企微解绑联动（unbindWecom）— 清空 wecom_user_id + wecom_bound_at；
 *       联动 account_status→DISABLED（无企微账号无法走企微扫码登录）；仅 HR 角色可调</li>
 * </ul>
 *
 * <p>与 P0-7.x 解冻登录状态机解耦：本卡只写账户/雇佣状态字段；IpdAuthService 读 account_status
 * 决定 NONE/QRSCAN/ACCOUNT 三态。本卡不动 IpdAuthService。
 *
 * <p>与 HandoverService 配合：resign 后触发 DRAFT 移交代办交由 HandoverService.initiateOnBehalf 闭环（HTTP 层串接）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PersonService {

    /** 账户状态常量（与 persons.account_status DDL 枚举对齐）。 */
    public static final String AC_ACTIVE = "ACTIVE";
    public static final String AC_FROZEN = "FROZEN_PENDING_HANDOVER";
    public static final String AC_DISABLED = "DISABLED";

    /** 雇佣状态常量（与 persons.employment_status DDL 枚举对齐）。 */
    public static final String EM_ACTIVE = "ACTIVE";
    public static final String EM_RESIGNED = "RESIGNED";

    private final PersonMapper personMapper;
    private final ProjectMemberMapper memberMapper;
    private final AuditLogService auditLogService;
    /** P2-2.2 离职冻结撤销会话（ipd loginType revokeAll；不改基线 sys_user）。 */
    private final IpdAuthSession ipdAuthSession;
    /** P2-2.2 离职通知派发（outbox 模式；NotificationChannel 仅留痕不外送）。 */
    private final NotificationService notificationService;

    /** P2-2.2 通知事件类型——离职冻结全链路知会（双方组长 + 本人 + 超管）。 */
    public static final String EVT_RESIGN_PENDING_HANDOVER = "RESIGN_PENDING_HANDOVER";
    /** P2-2.2 通知事件类型——15 日升级超管。 */
    public static final String EVT_RESIGN_ESCALATION = "RESIGN_ESCALATION";

    /**
     * 离职冻结（AC-USER-08；BR-USER-05；P2-2.2 联动：撤销会话 + 企微自动解绑 + 双方组长/本人/超管通知）。
     *
     * <p>状态机：employment_status ACTIVE → RESIGNED；account_status ACTIVE → FROZEN_PENDING_HANDOVER；
     * 已 RESIGNED 返 NOOP（幂等）。FROZEN_PENDING_HANDOVER / DISABLED 返 CONFLICT（避免重复冻结）。
     *
     * <p>P2-2.2 联动副作用（同事务内；侧副作用 revokeAll / notify 走 best-effort try/catch，
     * 不阻塞主链路 status 更新）：
     * <ol>
     *   <li>企微自动解绑：清空 wecom_user_id + wecom_bound_at，<b>不动 account_status</b>
     *       （必须保留 FROZEN_PENDING_HANDOVER — B8 先移交后禁用；DISABLED 由 HandoverService 全清后置入）。
     *       幂等：wecom 已空跳解绑审计。</li>
     *   <li>撤销会话：ipd loginType revokeAll；旧 token 立即失效。
     *       失败不抛（Sa-Token 故障不应阻塞主链路；前端下次请求由 scopeOf→NONE 兜底 401）。</li>
     *   <li>通知派发（outbox 模式）：
     *       <ul>
     *         <li>本人 → FYI（离职冻结知会）</li>
     *         <li>本人所属组组长（actor.groupId）→ ACTION（有项目待移交）/ FYI（无项目）</li>
     *         <li>对方组长（MARKET_PM 离职时通知 RD 组组长，反之亦然；无对方组长时跳过）→ FYI</li>
     *         <li>全部 SUPER_ADMIN → ACTION（升级兜底）</li>
     *       </ul>
     *       失败仅 warn 日志（已落 RESIGN，不回滚）。</li>
     *   <li>不创建 HandoverRecord — 实际移交流程由 HandoverService.initiateOnBehalf 接手
     *       （组长在收件箱看到通知后触发）。</li>
     * </ol>
     *
     * <p>不动 project_members 绑定（exitDate 由 HandoverService.accept 写）。
     *
     * @param personId 人员主键
     * @param reason 离职原因（审计可见）
     * @param operator 操作者（需 HR 角色或本人）
     * @return 影响 1 条 + 待移交项目数 + 联动副作用标记
     */
    public ResignResult resign(Long personId, String reason, IpdActor operator) {
        Person person = requirePerson(personId);
        // SEC-RESIGN-GROUP: 与 rehire/unbindWecom 同款跨组守卫；controller 语义为"HR 或本人"，
        // 本人操作先放行（对齐 PersonController 的 isHr-or-self 判定），其余走同组/超管校验。
        if (!operator.id().equals(personId)) {
            assertSameGroupOrAdmin(person, operator, "离职冻结");
        }
        if (EM_RESIGNED.equals(person.getEmploymentStatus())) {
            // 幂等 NOOP：返回当前快照，不抛错；联动副作用不重复触发
            long pending = countActiveMemberships(personId);
            return new ResignResult(true, pending, "已离职，幂等返回", false, false, 0);
        }
        if (AC_FROZEN.equals(person.getAccountStatus()) || AC_DISABLED.equals(person.getAccountStatus())) {
            // FROZEN_PENDING_HANDOVER / DISABLED 是"移交中"或"已禁用"，离职应在 ACTIVE 发起
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "当前账户状态不允许离职冻结: " + person.getAccountStatus());
        }
        Person before = cloneState(person);
        person.setEmploymentStatus(EM_RESIGNED);
        person.setAccountStatus(AC_FROZEN);
        int rows = personMapper.updateById(person);
        if (rows != 1) {
            throw new ServiceException("人员离职冻结未更新唯一记录: id=" + personId);
        }
        long pending = countActiveMemberships(personId);
        auditLogService.append(AuditLog.builder()
            .entityType("persons")
            .entityId(personId)
            .action("RESIGN")
            .operatorId(operator.id())
            .beforeData(snapshot(before))
            .afterData(snapshot(person) + " | pendingProjects=" + pending + " | reason=" + safe(reason))
            .build());

        // P2-2.2 联动 — 企微自动解绑（不动 accountStatus；保留 FROZEN_PENDING_HANDOVER）
        boolean wecomUnbound = autoUnbindWecomOnResign(person, operator);

        // P2-2.2 联动 — 撤销会话（best-effort；DB 已提交）
        boolean sessionsRevoked = revokeIpdSessions(personId, operator);

        // P2-2.2 联动 — 通知派发（best-effort；outbox 模式 publish 内部 try/catch DuplicateKeyException）
        int notificationsSent = publishResignNotifications(person, pending, reason, operator);

        log.info("P2-2.2 resign 联动: personId={} operator={} pendingProjects={} wecomUnbound={} sessionsRevoked={} notificationsSent={}",
            personId, operator.id(), pending, wecomUnbound, sessionsRevoked, notificationsSent);
        return new ResignResult(false, pending, "冻结成功", wecomUnbound, sessionsRevoked, notificationsSent);
    }

    /**
     * P2-2.2：离职冻结时企微自动解绑（保留 accountStatus=FROZEN_PENDING_HANDOVER；不写 DISABLED）。
     * 幂等：wecom 已空跳解绑审计。
     *
     * @return true=本次实际执行了清空
     */
    private boolean autoUnbindWecomOnResign(Person person, IpdActor operator) {
        if (person.getWecomUserId() == null || person.getWecomUserId().isBlank()) {
            return false;
        }
        String beforeWecom = "***";
        person.setWecomUserId(null);
        person.setWecomBoundAt(null);
        int rows = personMapper.updateById(person);
        if (rows != 1) {
            log.warn("P2-2.2 企微解绑更新影响行数≠1: personId={} rows={}", person.getId(), rows);
            return false;
        }
        auditLogService.append(AuditLog.builder()
            .entityType("persons")
            .entityId(person.getId())
            .action("UNBIND_WECHAT")
            .operatorId(operator.id())
            .reason("离职冻结自动解绑企微（AC-AUTH-06）")
            .beforeData("{wecom=" + beforeWecom + "}")
            .afterData("{wecom=null, accountStatus=" + person.getAccountStatus() + "}")
            .build());
        return true;
    }

    /**
     * P2-2.2：撤销 IPD 独立会话（loginType=ipd）。
     * 失败不抛（Sa-Token 故障不应阻塞主链路；前端下次请求由 IpdAuthService.scopeOf→NONE 兜底 401）。
     */
    private boolean revokeIpdSessions(Long personId, IpdActor operator) {
        try {
            ipdAuthSession.revokeAll(personId);
            auditLogService.append(AuditLog.builder()
                .entityType("persons")
                .entityId(personId)
                .action("REVOKE_SESSIONS")
                .operatorId(operator.id())
                .reason("离职冻结撤销会话（AC-AUTH-06）")
                .build());
            return true;
        } catch (Exception e) {
            log.warn("P2-2.2 撤销会话失败（不影响主链路）: personId={}", personId, e);
            return false;
        }
    }

    /**
     * P2-2.2：通知派发 — 本人 + 双方组长 + 全部超管。
     * 收件人去重：本人与 actor 同人时跳过本人；同组组长与对方组长同人时仅发一次。
     */
    private int publishResignNotifications(Person person, long pendingProjects, String reason, IpdActor operator) {
        int sent = 0;
        String kindLeader = pendingProjects > 0
            ? NotificationService.KIND_ACTION  // 待移交 — 需行动
            : NotificationService.KIND_FYI;     // 无项目 — 知会即可
        String contentLeader = pendingProjects > 0
            ? "您组内 " + person.getName() + " 离职冻结，名下有 " + pendingProjects + " 个项目待移交，请尽快处理。"
            : "您组内 " + person.getName() + " 离职冻结，名下无活跃项目。";
        String contentSelf = "您的账号已冻结（" + (reason == null ? "未注明原因" : reason)
            + "），仅保留移交相关权限。如需复职请联系 HR。";
        // 1) 本人 FYI（actor=本人时跳过，避免自收件箱冗余）
        if (!operator.id().equals(person.getId())) {
            try {
                notificationService.publish(person.getId(), EVT_RESIGN_PENDING_HANDOVER,
                    NotificationService.KIND_FYI, "persons", person.getId(),
                    "账号已冻结（离职）", contentSelf, "/ipd/profile");
                sent++;
            } catch (Exception e) {
                log.warn("P2-2.2 通知派发本人失败: personId={}", person.getId(), e);
            }
        }
        // 2) 本组组长 ACTION/FYI
        if (person.getGroupId() != null) {
            List<Long> groupLeaders = personMapper.selectList(
                new LambdaQueryWrapper<Person>()
                    .eq(Person::getGroupId, person.getGroupId())
                    .eq(Person::getPersonType, "GROUP_LEADER")
                    .eq(Person::getEmploymentStatus, "ACTIVE")
                    .eq(Person::getAccountStatus, "ACTIVE")
                    .eq(Person::getDelFlag, "0"))
                .stream().map(Person::getId).toList();
            for (Long leaderId : groupLeaders) {
                try {
                    notificationService.publish(leaderId, EVT_RESIGN_PENDING_HANDOVER,
                        kindLeader, "persons", person.getId(),
                        "组员离职待移交（" + person.getName() + "）",
                        contentLeader, "/ipd/handovers/inbox");
                    sent++;
                } catch (Exception e) {
                    log.warn("P2-2.2 通知本组组长失败: leaderId={}", leaderId, e);
                }
            }
        }
        // 3) 对方组长 FYI（MARKET_PM ↔ RD_PM；如有反向组长）
        String counterpartRole = "MARKET_PM".equals(person.getPersonType()) ? "RD_PM" : "MARKET_PM";
        // 对方组长 = 另一 personType 下的 GROUP_LEADER（跨 personType 必有；不限组）
        List<Long> counterpartLeaders = personMapper.selectList(
            new LambdaQueryWrapper<Person>()
                .eq(Person::getPersonType, "GROUP_LEADER")
                .eq(Person::getEmploymentStatus, "ACTIVE")
                .eq(Person::getAccountStatus, "ACTIVE")
                .eq(Person::getDelFlag, "0"))
            .stream()
            .filter(p -> !p.getGroupId().equals(person.getGroupId()))
            .map(Person::getId).toList();
        for (Long leaderId : counterpartLeaders) {
            try {
                notificationService.publish(leaderId, EVT_RESIGN_PENDING_HANDOVER,
                    NotificationService.KIND_FYI, "persons", person.getId(),
                    "对方 PM 离职知会（" + person.getName() + "）",
                    counterpartRole + " 角色 " + person.getName() + " 离职冻结，对方侧项目移交请关注。",
                    "/ipd/handovers/inbox");
                sent++;
            } catch (Exception e) {
                log.warn("P2-2.2 通知对方组长失败: leaderId={}", leaderId, e);
            }
        }
        // 4) 全部 SUPER_ADMIN ACTION（升级兜底）
        List<Long> admins = personMapper.selectList(
            new LambdaQueryWrapper<Person>()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .eq(Person::getEmploymentStatus, "ACTIVE")
                .eq(Person::getAccountStatus, "ACTIVE")
                .eq(Person::getDelFlag, "0"))
            .stream().map(Person::getId).toList();
        for (Long adminId : admins) {
            try {
                notificationService.publish(adminId, EVT_RESIGN_PENDING_HANDOVER,
                    NotificationService.KIND_ACTION, "persons", person.getId(),
                    "离职冻结升级通知（" + person.getName() + "）",
                    "人员 " + person.getName() + " 已离职冻结，15 日倒计时开始。名下 "
                        + pendingProjects + " 个项目待移交。",
                    "/ipd/admin/handovers/pending");
                sent++;
            } catch (Exception e) {
                log.warn("P2-2.2 通知超管失败: adminId={}", adminId, e);
            }
        }
        return sent;
    }

    /**
     * 复职（AC-USER-09；BR-USER-06）。
     *
     * <p>状态机：employment_status RESIGNED → ACTIVE；account_status FROZEN_PENDING_HANDOVER → ACTIVE；
     * DISABLED 需先调用 IpdAuthService 解禁（不允许直接复职）；ACTIVE 返 CONFLICT。
     *
     * <p>SEC-01b/HIGH 跨组守卫：GROUP_LEADER 仅可复职本组员工，跨组返 FORBIDDEN；SUPER_ADMIN 例外。
     * 不动 level/groupId（HR API 权威源；非本卡权限）。
     *
     * @param personId 人员主键
     * @param note 复职说明
     * @param operator 操作者（需 HR 角色）
     */
    public Person rehire(Long personId, String note, IpdActor operator) {
        Person person = requirePerson(personId);
        assertSameGroupOrAdmin(person, operator, "复职");
        if (!EM_RESIGNED.equals(person.getEmploymentStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "当前雇佣状态不允许复职: " + person.getEmploymentStatus());
        }
        if (AC_DISABLED.equals(person.getAccountStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "账户已禁用，需先解禁后再复职");
        }
        Person before = cloneState(person);
        person.setEmploymentStatus(EM_ACTIVE);
        person.setAccountStatus(AC_ACTIVE);
        int rows = personMapper.updateById(person);
        if (rows != 1) {
            throw new ServiceException("人员复职未更新唯一记录: id=" + personId);
        }
        auditLogService.append(AuditLog.builder()
            .entityType("persons")
            .entityId(personId)
            .action("REHIRE")
            .operatorId(operator.id())
            .beforeData(snapshot(before))
            .afterData(snapshot(person) + " | note=" + safe(note))
            .build());
        log.info("P2-1.3 rehire: personId={} operator={}", personId, operator.id());
        return person;
    }

    /**
     * 企微解绑联动（AC-USER-10；BR-USER-06）。
     *
     * <p>副作用：清空 wecom_user_id + wecom_bound_at；account_status → DISABLED
     * （无企微账号 ⇒ 无法走企微扫码登录，禁用兜底）。RESIGNED 状态允许解绑（不抛错）。
     * 幂等：wecom_user_id 已空返 NOOP。
     *
     * <p>SEC-01b/HIGH 跨组守卫：GROUP_LEADER 仅可解绑本组员工企微，跨组返 FORBIDDEN；SUPER_ADMIN 例外。
     * SEC-02b/MEDIUM 在职守卫：在职（employment_status=ACTIVE）员工被解绑企微将级联为 account=DISABLED
     * 阻断登录，要求 HR 先触发离职冻结；SUPER_ADMIN 例外可强制解绑（合规/账号封禁场景）。
     */
    public Person unbindWecom(Long personId, String reason, IpdActor operator) {
        Person person = requirePerson(personId);
        assertSameGroupOrAdmin(person, operator, "解绑企微");
        if (EM_ACTIVE.equals(person.getEmploymentStatus())
            && !"SUPER_ADMIN".equals(operator.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "在职员工解绑企微需先触发离职冻结");
        }
        if (person.getWecomUserId() == null || person.getWecomUserId().isBlank()) {
            // 幂等 NOOP
            return person;
        }
        Person before = cloneState(person);
        person.setWecomUserId(null);
        person.setWecomBoundAt(null);
        person.setAccountStatus(AC_DISABLED);
        int rows = personMapper.updateById(person);
        if (rows != 1) {
            throw new ServiceException("企微解绑未更新唯一记录: id=" + personId);
        }
        auditLogService.append(AuditLog.builder()
            .entityType("persons")
            .entityId(personId)
            .action("WECOM_UNBIND")
            .operatorId(operator.id())
            .beforeData(snapshot(before))
            .afterData(snapshot(person) + " | reason=" + safe(reason))
            .build());
        log.info("P2-1.3 wecomUnbind: personId={} operator={}", personId, operator.id());
        return person;
    }

    /** 查询当前生效项目成员绑定数（用于 resign 提示）。 */
    long countActiveMemberships(Long personId) {
        return memberMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getPersonId, personId)
            .isNull(ProjectMember::getExitDate)
            .eq(ProjectMember::getDelFlag, "0"));
    }

    /** 列出该人员的所有活跃项目成员绑定（用于前端展示+提示待移交）。 */
    public List<ProjectMember> listActiveMemberships(Long personId) {
        return memberMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getPersonId, personId)
            .isNull(ProjectMember::getExitDate)
            .eq(ProjectMember::getDelFlag, "0"));
    }

    /**
     * SEC-01b/HIGH 跨组守卫：GROUP_LEADER 仅可操作本组人员；SUPER_ADMIN 例外放行；其他角色（理论上
     * Controller 已 requireLeaderOrAdmin 限过，但 service 内兜底）一律同组校验。失败抛 FORBIDDEN。
     */
    private void assertSameGroupOrAdmin(Person person, IpdActor operator, String action) {
        if ("SUPER_ADMIN".equals(operator.role())) return;
        if ("GROUP_LEADER".equals(operator.role())) {
            if (operator.groupId() == null || !operator.groupId().equals(person.getGroupId())) {
                throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN,
                    "GROUP_LEADER 仅可" + action + "本组员工（操作组=" + operator.groupId()
                        + "，员工组=" + person.getGroupId() + "）");
            }
        }
    }

    private Person requirePerson(Long personId) {
        Person person = personMapper.selectById(personId);
        if (person == null || "1".equals(person.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "人员不存在: id=" + personId);
        }
        return person;
    }

    /** 深拷贝（只拷贝审计需要的字段；Person 字段少且不可变 from service 视角，原地修改前快照一次）。 */
    private Person cloneState(Person src) {
        return Person.builder()
            .id(src.getId())
            .employmentStatus(src.getEmploymentStatus())
            .accountStatus(src.getAccountStatus())
            .wecomUserId(src.getWecomUserId())
            .build();
    }

    /** 审计快照（最小化字段；不写 passwordHash）。 */
    private String snapshot(Person p) {
        return "{id=" + p.getId()
            + ",emp=" + p.getEmploymentStatus()
            + ",acc=" + p.getAccountStatus()
            + ",wecom=" + (p.getWecomUserId() == null ? "null" : "***") + "}";
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]", " ").substring(0, Math.min(s.length(), 200));
    }

    /** 离职结果：idempotent=true 表示幂等命中；pendingProjects 为待移交项目成员绑定数。
     * <p>P2-2.2 联动副作用标记：wecomUnbound=企微是否本次清空；sessionsRevoked=会话是否撤销成功；
     * notificationsSent=通知派发条数（本人 + 双方组长 + 全部超管）。 */
    public record ResignResult(boolean idempotent, long pendingProjects, String message,
                              boolean wecomUnbound, boolean sessionsRevoked, int notificationsSent) { }
}
