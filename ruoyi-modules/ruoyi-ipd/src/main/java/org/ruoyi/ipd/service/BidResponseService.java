package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 应标记录服务（P2-3.2 BR-TEAM）
 * 状态机：PENDING → ACCEPTED / REJECTED / WITHDRAWN
 */
@Service
@RequiredArgsConstructor
public class BidResponseService {

    private final BidResponseMapper bidResponseMapper;

    /**
     * 提交应标（研发PM）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse submit(BidResponse response) {
        response.setStatus("PENDING");
        response.setRespondedAt(new Date());
        response.setCreateTime(new Date());
        bidResponseMapper.insert(response);
        return response;
    }

    /**
     * 撤回应标
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse withdraw(Long id) {
        BidResponse resp = bidResponseMapper.selectById(id);
        if (resp == null) {
            throw new IllegalArgumentException("应标记录不存在: " + id);
        }
        if (!"PENDING".equals(resp.getStatus())) {
            throw new IllegalStateException("仅 PENDING 状态可撤回，当前: " + resp.getStatus());
        }
        resp.setStatus("WITHDRAWN");
        bidResponseMapper.updateById(resp);
        return resp;
    }

    /**
     * 拒绝应标（市场PM 操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse reject(Long id, String reason) {
        BidResponse resp = bidResponseMapper.selectById(id);
        if (resp == null) {
            throw new IllegalArgumentException("应标记录不存在: " + id);
        }
        resp.setStatus("REJECTED");
        resp.setResponseNote(reason);
        bidResponseMapper.updateById(resp);
        return resp;
    }

    /**
     * 查询某招标单下所有应标
     */
    public List<BidResponse> listByInvitation(Long invitationId) {
        return bidResponseMapper.selectList(
            new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getInvitationId, invitationId)
                .orderByDesc(BidResponse::getCreateTime)
        );
    }

    /**
     * 查询某研发PM的所有应标
     */
    public List<BidResponse> listByRdPm(Long rdPmId) {
        return bidResponseMapper.selectList(
            new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getRdPmId, rdPmId)
                .orderByDesc(BidResponse::getCreateTime)
        );
    }
}
