package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 招标单服务（P2-3.1 BR-TEAM-03/05）
 * 状态机：OPEN → SELECTED / EXPIRED → CLOSED
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2 软删除）
 */
@Service
public class BidInvitationService {

    private final BidInvitationMapper bidInvitationMapper;
    private final BidResponseMapper bidResponseMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public BidInvitationService(BidInvitationMapper bidInvitationMapper,
                                 BidResponseMapper bidResponseMapper,
                                 AuditLogService auditLogService) {
        this(bidInvitationMapper, bidResponseMapper, auditLogService, null);
    }

    /**
     * Spring 装配入口：双构造器并存时必须显式标注，否则容器无法透型尝试无参构造
     */
    @Autowired
    public BidInvitationService(BidInvitationMapper bidInvitationMapper,
                                 BidResponseMapper bidResponseMapper,
                                 AuditLogService auditLogService,
                                 NotificationService notificationService) {
        this.bidInvitationMapper = bidInvitationMapper;
        this.bidResponseMapper = bidResponseMapper;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * 创建招标单（市场PM）
     * AC-TEAM-03：市场PM 发起招标 ⇒ 招标单状态 OPEN
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation create(BidInvitation invitation) {
        invitation.setStatus("OPEN");
        invitation.setCreateTime(new Date());
        bidInvitationMapper.insert(invitation);
        return invitation;
    }

    /**
     * 发布招标单（通知目标研发PM 或全员）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation publish(Long id) {
        BidInvitation inv = requireOpen(id);
        // 通知逻辑由 OPS-04/OPS-05 消息服务承接，此处仅状态校验
        return inv;
    }

    /**
     * 遴选应标（AC-TEAM-05，P2-3.2 原子提交）：
     * 单事务内选定中标行（回填 rd_pm_id 并置 ACCEPTED）、其余 PENDING 行批量置 REJECTED（落选）、招标单置 SELECTED；
     * 遴选结果写审计（entityType=bid_invitation，action=select，afterData 含中标者与落选者清单——
     * OPS-05 通知系统就绪前由审计行承载“落选通知可查”留痕）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation selectResponse(Long invitationId, Long responseId, Long operatorId) {
        BidInvitation inv = requireOpen(invitationId);
        BidResponse resp = bidResponseMapper.selectById(responseId);
        if (resp == null || !resp.getInvitationId().equals(invitationId)) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "应标记录不存在或不属于该招标单");
        }
        if (!"PENDING".equals(resp.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "应标记录状态不允许遴选: " + resp.getStatus());
        }
        // AC-TEAM-05：中标行回填 rd_pm_id（列语义：应标时可为空，遴选后回填）
        if (resp.getRdPmId() == null) {
            // STATE_CONFLICT 而非 ROLE_LOCKED：缺 rd_pm_id 是业务数据不完整（非角色被锁定）；ROLE_LOCKED 专属"市场PM 不可跨研发PM 动作"语义
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "中标应标行缺少研发PM身份，无法绑定");
        }
        resp.setStatus("ACCEPTED");
        bidResponseMapper.updateById(resp);
        // 落选：同单其余 PENDING 行单 SQL 批量置 REJECTED（避免逐行写放大）
        List<BidResponse> losers = bidResponseMapper.selectList(new LambdaQueryWrapper<BidResponse>()
            .eq(BidResponse::getInvitationId, invitationId)
            .eq(BidResponse::getStatus, "PENDING")
            .ne(BidResponse::getId, responseId));
        String rejectedRdPmIds = losers.stream()
            .map(r -> String.valueOf(r.getRdPmId() == null ? r.getId() : r.getRdPmId()))
            .collect(Collectors.joining(","));
        bidResponseMapper.update(null, new LambdaUpdateWrapper<BidResponse>()
            .set(BidResponse::getStatus, "REJECTED")
            .eq(BidResponse::getInvitationId, invitationId)
            .eq(BidResponse::getStatus, "PENDING")
            .ne(BidResponse::getId, responseId));
        inv.setStatus("SELECTED");
        inv.setSelectedResponseId(responseId);
        bidInvitationMapper.updateById(inv);
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("select").entityType("bid_invitation").entityId(invitationId)
            .afterData("{\"selectedResponseId\":" + responseId
                + ",\"selectedRdPmId\":" + resp.getRdPmId()
                + ",\"rejectedRdPmIds\":[" + rejectedRdPmIds + "]}")
            .reason(inv.getTitle())
            .createTime(new Date()).build());
        // HIGH-1.2 落选通知：镜像 adminAssign 行 332-355 模式
        // 中标者 ⇒ BID_WON；其余落选 PENDING ⇒ BID_LOST（dedupKey 幂等，重复不重发）
        if (notificationService != null) {
            if (resp.getRdPmId() != null) {
                notificationService.publish(resp.getRdPmId(),
                    NotificationService.Types.BID_WON,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", invitationId,
                    "招标已遴选您",
                    "招标单「" + inv.getTitle() + "」(项目 " + invitationId + ") 已遴选您为中标研发PM，请尽快承接。",
                    "/bid-invitations/" + invitationId);
            }
            for (BidResponse loser : losers) {
                if (loser.getRdPmId() == null) continue;
                notificationService.publish(loser.getRdPmId(),
                    NotificationService.Types.BID_LOST,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", invitationId,
                    "招标落选通知",
                    "招标单「" + inv.getTitle() + "」(项目 " + invitationId + ") 已遴选他人，您本次落选。",
                    "/bid-invitations/" + invitationId);
            }
        }
        return inv;
    }

    /**
     * 过期扫描（定时任务，PERF-P0-2：单 SQL 条件 UPDATE，消除 N+1 selectCount 与恒等三元冗余）
     * AC-TEAM-08：招标到期无人应标 ⇒ 自动过期
     * ZK-IPD §四.1.3：到期后通知市场 PM（createBy）重新发起
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        Date now = new Date();
        // ZK-IPD §四.1.3：先 selectList 拿受影响行（id + createBy），update 完发通知
        List<BidInvitation> overdue = bidInvitationMapper.selectList(
            new LambdaQueryWrapper<BidInvitation>()
                .eq(BidInvitation::getStatus, "OPEN")
                .lt(BidInvitation::getExpireAt, now));
        int affected = bidInvitationMapper.update(null, new LambdaUpdateWrapper<BidInvitation>()
            .set(BidInvitation::getStatus, "EXPIRED")
            .eq(BidInvitation::getStatus, "OPEN")
            .lt(BidInvitation::getExpireAt, now));
        if (affected > 0 && notificationService != null) {
            for (BidInvitation inv : overdue) {
                if (inv.getCreateBy() == null) continue;
                notificationService.publish(inv.getCreateBy(),
                    NotificationService.Types.BID_EXPIRED_NO_RESPONSE,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", inv.getId(),
                    "招标已到期",
                    "招标单「" + inv.getTitle() + "」已到期且无应标，请考虑重新发起或调整条件（ZK-IPD §四.1.3）",
                    "/bid-invitations/" + inv.getId());
            }
        }
        return affected;
    }

    /**
     * 撤回招标单（24h 内可撤回，AC-TEAM-13）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation withdraw(Long id) {
        BidInvitation inv = requireOpen(id);
        long millisSinceCreate = System.currentTimeMillis() - inv.getCreateTime().getTime();
        if (millisSinceCreate > 24 * 60 * 60 * 1000L) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "超过24小时不可撤回");
        }
        inv.setStatus("CLOSED");
        bidInvitationMapper.updateById(inv);
        return inv;
    }

    /**
     * 关闭招标单
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation close(Long id) {
        BidInvitation inv = bidInvitationMapper.selectById(id);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "招标单不存在: " + id);
        }
        inv.setStatus("CLOSED");
        bidInvitationMapper.updateById(inv);
        return inv;
    }

    /**
     * 分页查询招标单列表
     */
    public IPage<BidInvitation> page(int pageNo, int pageSize, Long projectId, String status) {
        Page<BidInvitation> page = new Page<>(pageNo, Math.min(pageSize, 200));
        LambdaQueryWrapper<BidInvitation> qw = new LambdaQueryWrapper<BidInvitation>()
            .eq(projectId != null, BidInvitation::getProjectId, projectId)
            .eq(status != null && !status.isBlank(), BidInvitation::getStatus, status)
            .orderByDesc(BidInvitation::getCreateTime);
        return bidInvitationMapper.selectPage(page, qw);
    }

    /**
     * 查询招标单详情
     */
    public BidInvitation getById(Long id) {
        BidInvitation inv = bidInvitationMapper.selectById(id);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "招标单不存在: " + id);
        }
        return inv;
    }

    /**
     * 查询招标单下的应标列表（P2-3.2 隐私 + MEDIUM-2.2 公开招标应标者互见）
     *
     * <ul>
     *   <li>ONE_TO_ONE 模式：非发起人仅见本人应标（不变）</li>
     *   <li>PUBLIC 模式：所有 PENDING/ACCEPTED 记录对全员可见（WITHDRAWN/REJECTED 仅发起人或本人见）</li>
     *   <li>PUBLIC 模式下非发起人视角脱敏：解决方案摘要仅展示前 80 字符（避免互抄）</li>
     * </ul>
     *
     * @param invitationId    招标单 ID
     * @param currentPersonId 会话用户 ID；等于发起人（createBy）时返回全量（含 WITHDRAWN/REJECTED）
     */
    public List<BidResponse> listResponses(Long invitationId, Long currentPersonId) {
        BidInvitation inv = bidInvitationMapper.selectById(invitationId);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "招标单不存在: " + invitationId);
        }
        // MEDIUM-info-disclosure 修复：强制要求已登录 actor，避免 eq(field, null) IS-NULL 语义泄露遗留数据
        if (currentPersonId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "未登录或会话失效");
        }
        LambdaQueryWrapper<BidResponse> qw = new LambdaQueryWrapper<BidResponse>()
            .eq(BidResponse::getInvitationId, invitationId)
            .orderByDesc(BidResponse::getCreateTime);
        boolean isCreator = currentPersonId != null && currentPersonId.equals(inv.getCreateBy());
        boolean isPublic = "PUBLIC".equals(inv.getMode());
        if (isCreator) {
            // 发起人：全量（含 WITHDRAWN/REJECTED），不动 responseNote
        } else if (isPublic) {
            // MEDIUM-2.2：PUBLIC 模式下 WITHDRAWN/REJECTED 仅本人可见，其余应标者互见
            qw.and(w -> w.notIn(BidResponse::getStatus, "WITHDRAWN", "REJECTED")
                .or().eq(BidResponse::getRdPmId, currentPersonId));
        } else {
            // ONE_TO_ONE：仅本人
            qw.eq(BidResponse::getRdPmId, currentPersonId);
        }
        List<BidResponse> rows = bidResponseMapper.selectList(qw);
        // MEDIUM-2.2：PUBLIC 模式下非发起人视角脱敏解决方案摘要为前 80 字符
        if (isPublic && !isCreator) {
            for (BidResponse r : rows) {
                if (r.getRdPmId() != null && !r.getRdPmId().equals(currentPersonId)
                    && r.getResponseNote() != null) {
                    r.setResponseNote(maskSummary(r.getResponseNote()));
                }
            }
        }
        return rows;
    }

    /** MEDIUM-2.2：解决方案摘要脱敏（互见场景下避免互抄完整方案）；超长截断并附省略号 */
    static String maskSummary(String note) {
        if (note == null) {
            return null;
        }
        if (note.length() <= 80) {
            return note;
        }
        return note.substring(0, 80) + "…";
    }


    /**
     * AC-TEAM-13：市场PM（招标单发起人）在有效期内修改招标条件
     * - 仅发起人本人可改（横向越权防御）
     * - 仅 OPEN 状态可改（SELECTED/EXPIRED/CLOSED 状态机封口）
     * - 写审计 action=modify_conditions
     * - 向所有 PENDING 应标者发 BID_CONDITIONS_CHANGED 通知
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation modifyInvitation(Long id, String newTitle, String newContent,
                                         Date newExpireAt, Long operatorId) {
        BidInvitation inv = bidInvitationMapper.selectByIdForUpdate(id);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (operatorId == null || !operatorId.equals(inv.getCreateBy())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN);
        }
        if (!"OPEN".equals(inv.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        Date now = new Date();
        StringBuilder changeLog = new StringBuilder("{");
        changeLog.append("\"before\":{\"title\":\"").append(escape(inv.getTitle()))
            .append("\",\"expireAt\":\"").append(inv.getExpireAt()).append("\"}");
        inv.setTitle(newTitle);
        inv.setContent(newContent);
        inv.setExpireAt(newExpireAt);
        inv.setUpdateTime(now);
        bidInvitationMapper.updateById(inv);
        changeLog.append(",\"after\":{\"title\":\"").append(escape(newTitle))
            .append("\",\"expireAt\":\"").append(newExpireAt).append("\"}");
        changeLog.append("}");
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("modify_conditions").entityType("bid_invitation").entityId(id)
            .afterData(changeLog.toString())
            .reason(inv.getTitle())
            .createTime(now).build());
        // 通知所有 PENDING 应标者
        if (notificationService != null) {
            List<BidResponse> responders = bidResponseMapper.selectList(
                new LambdaQueryWrapper<BidResponse>()
                    .eq(BidResponse::getInvitationId, id)
                    .eq(BidResponse::getStatus, "PENDING"));
            for (BidResponse r : responders) {
                if (r.getRdPmId() == null) continue;
                notificationService.publish(r.getRdPmId(),
                    NotificationService.Types.BID_CONDITIONS_CHANGED,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", id,
                    "招标条件已变更",
                    "招标单 " + id + "「" + newTitle + "」条件已变更，请重新评估。",
                    "/bid-invitations/" + id);
            }
        }
        return inv;
    }

    /**
     * AC-TEAM-09：超管对挂起超 30 日的招标单直接指派（无需应标行）
     * SEC-REV-BID-01：状态门禁 + 年龄判定 + targetPersonId 必填 + 通知对等。
     *
     * <ul>
     *   <li>仅 EXPIRED 状态可被强制指派（避免在 OPEN/SELECTED 上覆盖正常流程）</li>
     *   <li>挂起需 ≥ 30 日（expireAt 锚点；expireAt 缺失回退到 updateTime）</li>
     *   <li>targetPersonId 必填（中标者）</li>
     *   <li>通知对等：targetPersonId ⇒ BID_WON；其他 PENDING 应标者（若有）⇒ BID_LOST</li>
     * </ul>
     *
     * 调用方需校验调用人为超管（controller 守卫 ipd:bid-invitation:admin-assign 注解 + IpdPermission.requireAdmin() 兜底）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation adminAssign(Long id, Long targetPersonId, Long adminId) {
        BidInvitation inv = bidInvitationMapper.selectByIdForUpdate(id);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        // Bug#4 高危：状态门禁 —— 防止 admin 在 OPEN/SELECTED 任意时刻强制指派覆盖正常流程
        if (!"EXPIRED".equals(inv.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "admin-assign 仅适用于 EXPIRED 挂起超 30 日的招标单，当前状态: " + inv.getStatus());
        }
        // Bug#4 高危：年龄判定 —— 挂起 ≥ 30 日才有强制指派的业务理由
        long ageMillis = ageOfInvitationMillis(inv);
        if (ageMillis < ADMIN_ASSIGN_MIN_AGE_MILLIS) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "招标单挂起不足 30 日，禁止 admin-assign");
        }
        // Bug#4 高危：targetPersonId 必填 —— 服务端权威，避免 admin 把空指针写成中标人
        if (targetPersonId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "targetPersonId 必填");
        }
        Date now = new Date();
        inv.setStatus("SELECTED");
        inv.setUpdateTime(now);
        bidInvitationMapper.updateById(inv);
        auditLogService.append(AuditLog.builder()
            .operatorId(adminId).action("admin_assign").entityType("bid_invitation").entityId(id)
            .afterData("{\"targetPersonId\":" + targetPersonId + "}")
            .reason(inv.getTitle())
            .createTime(now).build());
        // Bug#6 中危：兄弟路径门禁对等 —— 中标者 BID_WON；其他 PENDING 应标者 BID_LOST（保持与 selectResponse 一致语义）
        if (notificationService != null) {
            notificationService.publish(targetPersonId,
                NotificationService.Types.BID_WON,
                NotificationService.KIND_ACTION,
                "bid_invitation", id,
                "招标已指派给您",
                "招标单「" + inv.getTitle() + "」已被管理员指派给您，请尽快承接。",
                "/bid-invitations/" + id);
            List<BidResponse> losers = bidResponseMapper.selectList(new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getInvitationId, id)
                .eq(BidResponse::getStatus, "PENDING")
                .ne(BidResponse::getRdPmId, targetPersonId));
            for (BidResponse loser : losers) {
                if (loser.getRdPmId() == null) continue;
                notificationService.publish(loser.getRdPmId(),
                    NotificationService.Types.BID_LOST,
                    NotificationService.KIND_ACTION,
                    "bid_invitation", id,
                    "招标已由管理员指派他人",
                    "招标单「" + inv.getTitle() + "」已由管理员强制指派给其他人，本次落选。",
                    "/bid-invitations/" + id);
            }
        }
        return inv;
    }

    /** Bug#4：admin-assign 最小挂起时长（30 天）。 */
    static final long ADMIN_ASSIGN_MIN_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000L;

    /** Bug#4：挂起时长锚点 —— 优先 expireAt，缺失则回退到 updateTime。 */
    private static long ageOfInvitationMillis(BidInvitation inv) {
        if (inv.getExpireAt() != null) {
            return System.currentTimeMillis() - inv.getExpireAt().getTime();
        }
        if (inv.getUpdateTime() != null) {
            return System.currentTimeMillis() - inv.getUpdateTime().getTime();
        }
        return 0L;
    }

    /**
     * Bug#5 中危：JSON 字符串转义 —— 控制字符全集（\n \r \t \b \f + U+0000..U+001F）。
     * 旧实现仅处理 \\ 与 "，会把换行/制表等字符原样写入审计 afterData，导致 JSON 解析失败。
     */
    static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private BidInvitation requireOpen(Long id) {
        // 锁定读（H-1）：遴选/发布/撤回在招标单行上串行化，防止并发 selectResponse 双中标
        BidInvitation inv = bidInvitationMapper.selectByIdForUpdate(id);
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "招标单不存在: " + id);
        }
        if (!"OPEN".equals(inv.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "招标单状态非 OPEN，当前: " + inv.getStatus());
        }
        return inv;
    }
}
