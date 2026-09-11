package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 招标 3/7/30 日扫描服务（P2-3.3；AC-TEAM-06/07/08）
 *
 * <p>由定时任务按需调用：
 * <ul>
 *   <li>{@link #scanExpiringSoon()} — AC-TEAM-06：到期前 3 天的 OPEN 单提醒市场 PM</li>
 *   <li>{@link #scanSelectOverdue()} — AC-TEAM-07：到期有应标但超 7 天未遴选，升级通知主组组长</li>
 *   <li>{@link #scanExpireNoResponse()} — AC-TEAM-08：到期无人应标，自动 EXPIRED + 项目挂起为 TEAMING</li>
 * </ul>
 *
 * <p>扫描去重：依赖 {@link NotificationService#publishDaily} 的日期 dedup_key（自然日），
 * 同日多次扫描只生成一次事件；次日可再提醒。
 */
@Service
@RequiredArgsConstructor
public class BidScanService {

    /** AC-TEAM-06：到期前 N 天提醒 */
    static final int EXPIRING_SOON_DAYS = 3;
    /** AC-TEAM-07：到期后超 N 天未遴选即升级组长 */
    static final int SELECT_OVERDUE_DAYS = 7;

    private final BidInvitationMapper bidInvitationMapper;
    private final BidResponseMapper bidResponseMapper;
    private final ProjectMapper projectMapper;
    private final PersonMapper personMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /** 可注入时钟（仿 stateMachineGuard 模式；测试固定时刻消除真实时钟摇摆，生产零影响）。 */
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();
    public void setClock(java.time.Clock clock) {
        this.clock = (clock == null) ? java.time.Clock.systemDefaultZone() : clock;
    }
    private Date now() { return Date.from(clock.instant()); }
    /**
     * AC-TEAM-06：到期前 3 天的 OPEN 单，提醒市场 PM（dedup_key 含自然日）。
     * 扫描范围：expireAt ∈ [now, now+3d] 且 status=OPEN。
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanExpiringSoon() {
        Date window = new Date(now().getTime() + EXPIRING_SOON_DAYS * 86400_000L);
        List<BidInvitation> due = bidInvitationMapper.selectList(
            new LambdaQueryWrapper<BidInvitation>()
                .eq(BidInvitation::getStatus, "OPEN")
                .ge(BidInvitation::getExpireAt, now())
                .le(BidInvitation::getExpireAt, window));
        for (BidInvitation inv : due) {
            Long receiverId = inv.getCreateBy();
            if (receiverId == null) {
                continue;
            }
            notificationService.publishDaily(receiverId,
                NotificationService.Types.BID_EXPIRING_SOON,
                NotificationService.KIND_ACTION,
                "bid_invitation", inv.getId(),
                "招标单即将到期",
                "招标单 " + inv.getId() + "「" + inv.getTitle() + "」将于 3 天内到期，请尽快遴选或续期。",
                "/bid-invitations/" + inv.getId(),
                now());
        }
        return due.size();
    }

    /**
     * AC-TEAM-07：到期有应标但超 7 天未遴选，升级通知产品组长（MEDIUM-2.3 真发通知）。
     * 扫描范围：expireAt < (now-7d) 且 status=OPEN（说明有应标但还未遴选，否则 scanExpireNoResponse 已处理）。
     * 升级目标：项目主组组长（persons.person_type=GROUP_LEADER AND group_id=project.main_group_id）。
     * 幂等：依赖 publishDaily 的自然日 dedup_key；同日重扫同一 overdue 单只发一次。
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanSelectOverdue() {
        Date cutoff = new Date(now().getTime() - SELECT_OVERDUE_DAYS * 86400_000L);
        List<BidInvitation> overdue = bidInvitationMapper.selectList(
            new LambdaQueryWrapper<BidInvitation>()
                .eq(BidInvitation::getStatus, "OPEN")
                .lt(BidInvitation::getExpireAt, cutoff));
        int escalated = 0;
        for (BidInvitation inv : overdue) {
            // 仅处理"有应标"的单；无应标的走 scanExpireNoResponse 路径
            Long hasResp = bidResponseMapper.selectCount(new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getInvitationId, inv.getId())
                .in(BidResponse::getStatus, "PENDING", "ACCEPTED"));
            if (hasResp == null || hasResp == 0) {
                continue;
            }
            // 升级给市场 PM（FIRSTLY）+ 项目主组组长（升级链）
            if (inv.getCreateBy() != null) {
                notificationService.publishDaily(inv.getCreateBy(),
                    NotificationService.Types.BID_SELECT_OVERDUE,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", inv.getId(),
                    "遴选已超期，请尽快处理",
                    "招标单 " + inv.getId() + "「" + inv.getTitle() + "」到期已超 7 日仍未遴选，请尽快处理。",
                    "/bid-invitations/" + inv.getId(),
                    now());
            }
            // MEDIUM-2.3：升级主组组长（person_type=GROUP_LEADER AND group_id=project.main_group_id）
            Project project = projectMapper.selectById(inv.getProjectId());
            if (project != null && project.getMainGroupId() != null) {
                List<Person> leaders = personMapper.selectList(
                    new LambdaQueryWrapper<Person>()
                        .eq(Person::getPersonType, "GROUP_LEADER")
                        .eq(Person::getGroupId, project.getMainGroupId()));
                if (leaders != null && !leaders.isEmpty()) {
                    String title = "招标遴选超期已升级组长";
                    String content = "招标单 " + inv.getId() + "「" + inv.getTitle() + "」到期已超 7 日仍未遴选，已升级至主组组长处理。";
                    for (Person leader : leaders) {
                        if (leader.getId() == null) continue;
                        notificationService.publishDaily(leader.getId(),
                            NotificationService.Types.BID_SELECT_OVERDUE_ESCALATED,
                            NotificationService.KIND_ACTION,
                            "bid_invitation", inv.getId(),
                            title,
                            content,
                            "/bid-invitations/" + inv.getId(),
                            now());
                    }
                    auditLogService.append(AuditLog.builder()
                        .operatorId(null)
                        .action("bid_select_overdue_escalate")
                        .entityType("bid_invitation")
                        .entityId(inv.getId())
                        .reason("主组=" + project.getMainGroupId() + "，已通知组长 " + leaders.size() + " 人")
                        .createTime(now())
                        .build());
                } else {
                    // MEDIUM-2.3：未找到组长时仅 audit 留痕，不发通知
                    auditLogService.append(AuditLog.builder()
                        .operatorId(null)
                        .action("bid_select_overdue_escalate")
                        .entityType("bid_invitation")
                        .entityId(inv.getId())
                        .reason("主组=" + project.getMainGroupId() + "，未找到 GROUP_LEADER，未发通知")
                        .createTime(now())
                        .build());
                }
            }
            escalated++;
        }
        return escalated;
    }

    /**
     * AC-TEAM-08：到期无人应标自动 EXPIRED + 项目置 TEAMING（待组队挂起）。
     * 扫描范围：expireAt < now 且 status=OPEN 且无 PENDING/ACCEPTED 应标。
     *
     * @return 关闭单数
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanExpireNoResponse() {
        List<BidInvitation> expired = bidInvitationMapper.selectList(
            new LambdaQueryWrapper<BidInvitation>()
                .eq(BidInvitation::getStatus, "OPEN")
                .lt(BidInvitation::getExpireAt, now()));
        int closed = 0;
        for (BidInvitation inv : expired) {
            Long hasResp = bidResponseMapper.selectCount(new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getInvitationId, inv.getId())
                .in(BidResponse::getStatus, "PENDING", "ACCEPTED"));
            if (hasResp != null && hasResp > 0) {
                continue; // 有应标走 AC-TEAM-07 升级路径
            }
            // 1. 招标单置 EXPIRED（单 SQL 条件 UPDATE，避免二次拉取）
            int affected = bidInvitationMapper.update(null,
                new LambdaUpdateWrapper<BidInvitation>()
                    .eq(BidInvitation::getId, inv.getId())
                    .eq(BidInvitation::getStatus, "OPEN")
                    .set(BidInvitation::getStatus, "EXPIRED"));
            if (affected == 0) {
                continue;
            }
            // 2. 项目置 TEAMING（待组队挂起）
            projectMapper.update(null,
                new LambdaUpdateWrapper<Project>()
                    .eq(Project::getId, inv.getProjectId())
                    .set(Project::getStatus, "TEAMING"));
            // 3. 通知市场 PM
            if (inv.getCreateBy() != null) {
                notificationService.publishDaily(inv.getCreateBy(),
                    NotificationService.Types.BID_EXPIRED_NO_RESPONSE,
                    NotificationService.KIND_FYI,
                    "bid_invitation", inv.getId(),
                    "招标已到期无人应标",
                    "招标单 " + inv.getId() + "「" + inv.getTitle() + "」已到期且无人应标，项目转待组队状态。",
                    "/bid-invitations/" + inv.getId(),
                    now());
            }
            closed++;
        }
        return closed;
    }
}
