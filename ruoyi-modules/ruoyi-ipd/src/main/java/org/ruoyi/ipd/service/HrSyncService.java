package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * HR 同步事件接入（P2-2.2；AC-AUTH-06/AC-HAND-01 联动；BR-USER-05/06）。
 *
 * <p>真源切换对接在二期；一期为 Mock（PersonSyncService 已落 P2-2.3）。
 * 本服务仅承担"HR 系统上报离职/复职事件"入口，核心业务（冻结/解冻/解绑/复职）
 * 仍走 PersonService.resign / rehire / unbindWecom — 本服务为调度适配器，不复制业务规则。
 *
 * <p>端点：
 * <ul>
 *   <li>{@link #markResignedByHr} — HR 系统上报离职 → PersonService.resign（联动撤销会话/解绑企微/通知）</li>
 *   <li>{@link #listPendingHandoverEscalations} — 查询 FROZEN_PENDING_HANDOVER 超过阈值的待升级人员</li>
 *   <li>{@link #escalateStaleResignations} — 15 日倒计时升级超管（手动触发 / cron 调用）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class HrSyncService {

    /** 15 日升级阈值（AC-HAND-01：15 日倒计时超时升级超管）。 */
    public static final int DEFAULT_HANDOVER_DEADLINE_DAYS = 15;

    private final PersonService personService;
    private final NotificationService notificationService;
    private final PersonMapper personMapper;
    private final ProjectMemberMapper memberMapper;

    /**
     * HR 系统上报离职事件 → 触发 PersonService.resign 完整链路。
     *
     * <p>权限：本端点仅 admin 可调（HrSyncController 已 requireAdmin 守门）。
     * 联动副作用（撤销会话/解绑企微/通知）由 PersonService.resign 内部完成；本方法仅做参数透传与日志。
     *
     * @return PersonService.ResignResult（含 wecomUnbound / sessionsRevoked / notificationsSent）
     */
    public PersonService.ResignResult markResignedByHr(Long personId, String reason, IpdActor operator) {
        log.info("P2-2.2 hr-sync mark-resigned: personId={} operator={} reason={}",
            personId, operator.id(), reason);
        return personService.resign(personId, reason, operator);
    }

    /**
     * 查询当前所有 FROZEN_PENDING_HANDOVER 人员，附加其名下待移交项目数。
     * 用于前端"待移交"列表与管理员视图；测试亦用于断言升级前的快照。
     */
    public List<PendingHandover> listPendingHandoverEscalations(int thresholdDays) {
        // 注：FROZEN_PENDING_HANDOVER + employmentStatus=RESIGNED 双条件锁定；employmentStatus 仍
        // 为 RESIGNED 是因 resign 仅切 account_status，未回切 employment_status（BR-USER-06 守恒）。
        List<Person> frozen = personMapper.selectList(new LambdaQueryWrapper<Person>()
            .eq(Person::getEmploymentStatus, PersonService.EM_RESIGNED)
            .eq(Person::getAccountStatus, PersonService.AC_FROZEN)
            .eq(Person::getDelFlag, "0")
            .orderByAsc(Person::getUpdateTime));
        if (frozen.isEmpty()) {
            return Collections.emptyList();
        }
        long thresholdMs = thresholdDays * 24L * 60L * 60L * 1000L;
        long nowMs = System.currentTimeMillis();
        List<PendingHandover> out = new ArrayList<>(frozen.size());
        for (Person p : frozen) {
            long activeCount = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getPersonId, p.getId())
                .isNull(ProjectMember::getExitDate)
                .eq(ProjectMember::getDelFlag, "0"));
            long frozenSinceMs = p.getUpdateTime() == null ? nowMs : p.getUpdateTime().getTime();
            long ageDays = (nowMs - frozenSinceMs) / (24L * 60L * 60L * 1000L);
            out.add(new PendingHandover(p.getId(), p.getName(), p.getEmployeeNo(),
                p.getGroupId(), p.getUpdateTime(), activeCount, ageDays, ageDays >= thresholdDays));
        }
        // 抑制未用警告
        if (thresholdMs < 0) log.debug("thresholdDays={}", thresholdDays);
        return out;
    }

    /**
     * 15 日倒计时升级超管：扫描 FROZEN_PENDING_HANDOVER 超过 {@code thresholdDays} 天的人员，
     * 给全部 SUPER_ADMIN 发 ACTION 通知。已发过升级通知的通过 dedup_key 幂等去重
     * （NotificationService 内置 uk_notify_dedup 唯一约束 + DuplicateKeyException 捕获）。
     *
     * <p>调度接入：本方法由 {@link PersonResignEscalator} 每日 09:00 调用；亦可 admin 手动触发。
     *
     * @return 升级条数（成功发 ACTION 通知的 person 数；幂等命中不计）
     */
    public int escalateStaleResignations(int thresholdDays, IpdActor operator) {
        List<PendingHandover> stale = listPendingHandoverEscalations(thresholdDays).stream()
            .filter(p -> p.escalate).toList();
        if (stale.isEmpty()) {
            log.info("P2-2.2 升级扫描无遗留项: thresholdDays={}", thresholdDays);
            return 0;
        }
        List<Long> admins = personMapper.selectList(
            new LambdaQueryWrapper<Person>()
                .eq(Person::getPersonType, "SUPER_ADMIN")
                .eq(Person::getEmploymentStatus, "ACTIVE")
                .eq(Person::getAccountStatus, "ACTIVE")
                .eq(Person::getDelFlag, "0"))
            .stream().map(Person::getId).toList();
        int escalated = 0;
        for (PendingHandover p : stale) {
            for (Long adminId : admins) {
                try {
                    notificationService.publish(adminId, PersonService.EVT_RESIGN_ESCALATION,
                        NotificationService.KIND_ACTION, "persons", p.personId,
                        "离职冻结超期升级（" + p.name + "）",
                        "人员 " + p.name + "（" + p.employeeNo + "）离职冻结已达 "
                            + p.ageDays + " 天（阈值 " + thresholdDays + " 天），名下 "
                            + p.activeProjects + " 个项目仍待移交。请尽快处置。",
                        "/ipd/admin/handovers/pending");
                } catch (Exception e) {
                    log.warn("P2-2.2 升级通知失败: adminId={} personId={}", adminId, p.personId, e);
                }
            }
            escalated++;
        }
        log.info("P2-2.2 升级扫描: staleCount={} escalated={} admins={} operator={}",
            stale.size(), escalated, admins.size(), operator.id());
        return escalated;
    }

    /** 待移交人员快照（FROZEN_PENDING_HANDOVER）。 */
    public record PendingHandover(Long personId, String name, String employeeNo, Long groupId,
                                  Date frozenSince, long activeProjects, long ageDays,
                                  boolean escalate) { }
}
