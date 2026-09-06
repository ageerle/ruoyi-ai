package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
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
@RequiredArgsConstructor
public class BidInvitationService {

    private final BidInvitationMapper bidInvitationMapper;
    private final BidResponseMapper bidResponseMapper;
    private final AuditLogService auditLogService;

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
            throw new IllegalArgumentException("应标记录不存在或不属于该招标单");
        }
        if (!"PENDING".equals(resp.getStatus())) {
            throw new IllegalStateException("应标记录状态不允许遴选: " + resp.getStatus());
        }
        // AC-TEAM-05：中标行回填 rd_pm_id（列语义：应标时可为空，遴选后回填）
        if (resp.getRdPmId() == null) {
            throw new IllegalStateException("中标应标行缺少研发PM身份，无法绑定");
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
        return inv;
    }

    /**
     * 过期扫描（定时任务，PERF-P0-2：单 SQL 条件 UPDATE，消除 N+1 selectCount 与恒等三元冗余）
     * AC-TEAM-08：招标到期无人应标 ⇒ 自动过期
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        return bidInvitationMapper.update(null, new LambdaUpdateWrapper<BidInvitation>()
            .set(BidInvitation::getStatus, "EXPIRED")
            .eq(BidInvitation::getStatus, "OPEN")
            .lt(BidInvitation::getExpireAt, new Date()));
    }

    /**
     * 撤回招标单（24h 内可撤回，AC-TEAM-13）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation withdraw(Long id) {
        BidInvitation inv = requireOpen(id);
        long millisSinceCreate = System.currentTimeMillis() - inv.getCreateTime().getTime();
        if (millisSinceCreate > 24 * 60 * 60 * 1000L) {
            throw new IllegalStateException("超过24小时不可撤回");
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
            throw new IllegalArgumentException("招标单不存在: " + id);
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
            throw new IllegalArgumentException("招标单不存在: " + id);
        }
        return inv;
    }

    /**
     * 查询招标单下的应标列表（P2-3.2 隐私：非发起人仅可见本人应标，不得暴露其他应标）
     *
     * @param invitationId    招标单 ID
     * @param currentPersonId 会话用户 ID；等于发起人（createBy）时返回全量
     */
    public List<BidResponse> listResponses(Long invitationId, Long currentPersonId) {
        BidInvitation inv = bidInvitationMapper.selectById(invitationId);
        if (inv == null) {
            throw new IllegalArgumentException("招标单不存在: " + invitationId);
        }
        LambdaQueryWrapper<BidResponse> qw = new LambdaQueryWrapper<BidResponse>()
            .eq(BidResponse::getInvitationId, invitationId)
            .orderByDesc(BidResponse::getCreateTime);
        if (!currentPersonId.equals(inv.getCreateBy())) {
            qw.eq(BidResponse::getRdPmId, currentPersonId);
        }
        return bidResponseMapper.selectList(qw);
    }

    private BidInvitation requireOpen(Long id) {
        // 锁定读（H-1）：遴选/发布/撤回在招标单行上串行化，防止并发 selectResponse 双中标
        BidInvitation inv = bidInvitationMapper.selectByIdForUpdate(id);
        if (inv == null) {
            throw new IllegalArgumentException("招标单不存在: " + id);
        }
        if (!"OPEN".equals(inv.getStatus())) {
            throw new IllegalStateException("招标单状态非 OPEN，当前: " + inv.getStatus());
        }
        return inv;
    }
}
