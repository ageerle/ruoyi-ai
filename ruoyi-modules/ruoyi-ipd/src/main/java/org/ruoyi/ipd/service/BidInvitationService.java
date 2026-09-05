package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

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
     * 遴选应标（AC-TEAM-05）
     * 市场PM 从应标列表中选定一个研发PM
     */
    @Transactional(rollbackFor = Exception.class)
    public BidInvitation selectResponse(Long invitationId, Long responseId) {
        BidInvitation inv = requireOpen(invitationId);
        BidResponse resp = bidResponseMapper.selectById(responseId);
        if (resp == null || !resp.getInvitationId().equals(invitationId)) {
            throw new IllegalArgumentException("应标记录不存在或不属于该招标单");
        }
        if (!"PENDING".equals(resp.getStatus()) && !"ACCEPTED".equals(resp.getStatus())) {
            throw new IllegalStateException("应标记录状态不允许遴选: " + resp.getStatus());
        }
        inv.setStatus("SELECTED");
        inv.setSelectedResponseId(responseId);
        bidInvitationMapper.updateById(inv);
        resp.setStatus("ACCEPTED");
        resp.setRespondedAt(new Date());
        bidResponseMapper.updateById(resp);
        return inv;
    }

    /**
     * 过期扫描（定时任务）
     * AC-TEAM-08：招标到期无人应标 ⇒ 自动关闭
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        Date now = new Date();
        List<BidInvitation> overdue = bidInvitationMapper.selectList(
            new LambdaQueryWrapper<BidInvitation>()
                .eq(BidInvitation::getStatus, "OPEN")
                .lt(BidInvitation::getExpireAt, now)
        );
        for (BidInvitation inv : overdue) {
            long responseCount = bidResponseMapper.selectCount(
                new LambdaQueryWrapper<BidResponse>()
                    .eq(BidResponse::getInvitationId, inv.getId())
            );
            inv.setStatus(responseCount == 0 ? "EXPIRED" : "EXPIRED");
            bidInvitationMapper.updateById(inv);
        }
        return overdue.size();
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
     * 查询招标单下的应标列表
     */
    public List<BidResponse> listResponses(Long invitationId) {
        return bidResponseMapper.selectList(
            new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getInvitationId, invitationId)
                .orderByDesc(BidResponse::getCreateTime)
        );
    }

    private BidInvitation requireOpen(Long id) {
        BidInvitation inv = bidInvitationMapper.selectById(id);
        if (inv == null) {
            throw new IllegalArgumentException("招标单不存在: " + id);
        }
        if (!"OPEN".equals(inv.getStatus())) {
            throw new IllegalStateException("招标单状态非 OPEN，当前: " + inv.getStatus());
        }
        return inv;
    }
}
