package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 应标记录服务（P2-3.2 BR-TEAM-03/05、BR-REC-BID-01/02/03）
 * 状态机：PENDING → ACCEPTED（中标，遴选回写）/ REJECTED（落选，遴选时批量）/ WITHDRAWN（本人撤回）
 * BR-TEAM-03：研发PM 拒绝应标不留痕（不记录/不通知/不写拒绝审计），接口以 code=0 + data=null 表达 204 语义。
 */
@Service
@RequiredArgsConstructor
public class BidResponseService {

    /** 方案摘要（solution_summary）承载列 response_note 的最小长度，spec 页21：accept 必填 ≥40 字 */
    static final int SOLUTION_SUMMARY_MIN_CHARS = 40;

    private final BidResponseMapper bidResponseMapper;
    private final BidInvitationMapper bidInvitationMapper;
    private final AuditLogService auditLogService;

    /**
     * 提交应标（研发PM）。rdPmId 以会话用户为准（服务端权威），不信任请求体；
     * BR-REC-BID-03：同一研发PM同一招标单仅一份最新有效应标，重复提交覆盖更新不产生第二行。
     *
     * @param response        应标内容（decision=accept|reject；accept 时 responseNote 承载方案摘要）
     * @param currentPersonId 会话用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse submit(BidResponse response, Long currentPersonId) {
        // 锁定读（H-1/M-1）：同一招标单上的并发应标/遴选串行化；获锁后的幂等读能看到前序事务已提交的应标行
        BidInvitation inv = bidInvitationMapper.selectByIdForUpdate(response.getInvitationId());
        if (inv == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if ("ONE_TO_ONE".equals(inv.getMode()) && !currentPersonId.equals(inv.getTargetPersonId())) {
            // spec 页21 错误码 30001（不在邀请名单）与现役 FORBIDDEN 同码，直接复用
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN);
        }
        // decision 白名单：null 默认 accept（兼容 P2-3.1 无 decision 的既有调用语义），拼错值拒绝（L-2）
        String decision = response.getDecision() == null ? "accept" : response.getDecision().trim().toLowerCase();
        if (!"accept".equals(decision) && !"reject".equals(decision)) {
            throw new IpdBusinessException("decision 仅允许 accept|reject");
        }
        if ("reject".equals(decision)) {
            // BR-TEAM-03 / AC-TEAM-03：拒绝不留痕——不写业务表、不写审计、不通知
            return null;
        }
        if (!"OPEN".equals(inv.getStatus())) {
            // spec 页21 错误码 40001（状态机非法）：40001 已被 GATE_NOT_PASSED 占用，映射现役 STATE_CONFLICT（HTTP 409）
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        String note = response.getResponseNote() == null ? "" : response.getResponseNote().trim();
        if (note.length() < SOLUTION_SUMMARY_MIN_CHARS || note.length() > 500) {
            throw new IpdBusinessException("应标方案摘要须为 40-500 字（spec 页21 solution_summary，映射列 response_note）");
        }
        response.setRdPmId(currentPersonId);
        BidResponse existing = bidResponseMapper.selectOne(new LambdaQueryWrapper<BidResponse>()
            .eq(BidResponse::getInvitationId, inv.getId())
            .eq(BidResponse::getRdPmId, currentPersonId)
            .in(BidResponse::getStatus, "PENDING", "ACCEPTED")
            .last("limit 1"));
        Date now = new Date();
        if (existing != null) {
            if ("ACCEPTED".equals(existing.getStatus())) {
                // 已中标（遴选已定），状态机不允许再应标
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
            }
            existing.setResponseNote(note);
            existing.setRespondedAt(now);
            bidResponseMapper.updateById(existing);
            audit(existing, currentPersonId);
            return existing;
        }
        response.setResponseNote(note);
        response.setStatus("PENDING");
        response.setRespondedAt(now);
        response.setCreateTime(now);
        bidResponseMapper.insert(response);
        audit(response, currentPersonId);
        return response;
    }

    /**
     * 撤回应标（仅应标本人；横向越权防御）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse withdraw(Long id, Long currentPersonId) {
        BidResponse resp = bidResponseMapper.selectById(id);
        if (resp == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (!currentPersonId.equals(resp.getRdPmId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN);
        }
        if (!"PENDING".equals(resp.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        resp.setStatus("WITHDRAWN");
        bidResponseMapper.updateById(resp);
        return resp;
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

    /**
     * 应标审计：仅 accept 写入（BR-TEAM-03 拒绝不写审计）；entityType 对齐 spec 页21 全局枚举 bid_response
     */
    private void audit(BidResponse resp, Long operatorId) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action("accept").entityType("bid_response").entityId(resp.getId())
            .reason("invitation=" + resp.getInvitationId())
            .createTime(new Date()).build());
    }
}
