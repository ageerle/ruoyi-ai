package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 应标记录服务（P2-3.2 BR-TEAM-03/05、BR-REC-BID-01/02/03）
 * 状态机：PENDING → ACCEPTED（中标，遴选回写）/ REJECTED（落选，遴选时批量）/ WITHDRAWN（本人撤回）
 * BR-TEAM-03：研发PM 拒绝应标不留痕（不记录/不通知/不写拒绝审计），接口以 code=0 + data=null 表达 204 语义。
 * W5-E-2.4（P0 #5 IDOR 修复）：公开方法一律以 {@link IpdActor} 为第一参数并做入口校验——
 * service 层不信任 controller 必传；listByRdPm 增加本人/SUPER_ADMIN/关联项目在职成员三分支越权校验。
 */
@Service
@RequiredArgsConstructor
public class BidResponseService {

    /** 方案摘要（solution_summary）承载列 response_note 的最小长度，spec 页21：accept 必填 ≥40 字 */
    static final int SOLUTION_SUMMARY_MIN_CHARS = 40;

    private final BidResponseMapper bidResponseMapper;
    private final BidInvitationMapper bidInvitationMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final AuditLogService auditLogService;

    /**
     * W5-E-2.4 件 1.2：actor 入口校验——service 层不信任 controller 必传（防御性兜底）。
     * actor == null 或 actor.id() == null → UNAUTHORIZED。
     */
    private static void requireAuthenticated(IpdActor actor) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未登录");
        }
    }

    /**
     * 提交应标（研发PM）。rdPmId 以会话用户为准（服务端权威），不信任请求体；
     * BR-REC-BID-03：同一研发PM同一招标单仅一份最新有效应标，重复提交覆盖更新不产生第二行。
     *
     * @param actor    会话用户身份（W5-E-2.4：actor 入口校验，取 actor.id() 为应标人）
     * @param response 应标内容（decision=accept|reject；accept 时 responseNote 承载方案摘要）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse submit(IpdActor actor, BidResponse response) {
        requireAuthenticated(actor);
        Long currentPersonId = actor.id();
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
     * 撤回应标（仅应标本人；横向越权防御）。
     * W5-E-2.4：actor 入口校验（UNAUTHORIZED 兜底），撤回人取 actor.id()，归属校验逻辑零改。
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResponse withdraw(IpdActor actor, Long id) {
        requireAuthenticated(actor);
        Long currentPersonId = actor.id();
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
     * 查询某研发PM的所有应标（W5-E-2.4 P0 #5 IDOR 修复）。
     * 三分支放行：本人（actor.id == rdPmId）/ SUPER_ADMIN / 关联项目在职 ProjectMember
     * （该研发PM应标所隶属招标单 → 项目 → project_member 在职行，KpiSharedCollectionService 同款 exitDate IS NULL 口径）；
     * 其余一律 FORBIDDEN——目标无应标行时第三方同样拒绝（fail-closed，不泄露「有无应标」布尔 oracle）。
     */
    public List<BidResponse> listByRdPm(IpdActor actor, Long rdPmId) {
        requireAuthenticated(actor);
        if (rdPmId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "rdPmId 不能为空");
        }
        List<BidResponse> rows = bidResponseMapper.selectList(
            new LambdaQueryWrapper<BidResponse>()
                .eq(BidResponse::getRdPmId, rdPmId)
                .orderByDesc(BidResponse::getCreateTime)
        );
        if (actor.id().equals(rdPmId) || "SUPER_ADMIN".equals(actor.role())
            || isRelatedProjectMember(actor, rows)) {
            return rows == null ? List.of() : rows;
        }
        throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权查看他人应标");
    }

    /**
     * actor 是否为该研发PM应标所涉项目（招标单 → 项目链）的在职 ProjectMember。
     * 与 KpiSharedCollectionService 同款判定：personId 匹配且 exit_date IS NULL（排除已退出）。
     */
    private boolean isRelatedProjectMember(IpdActor actor, List<BidResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        List<Long> invitationIds = rows.stream()
            .map(BidResponse::getInvitationId).filter(Objects::nonNull).distinct()
            .collect(Collectors.toList());
        if (invitationIds.isEmpty()) {
            return false;
        }
        List<Long> projectIds = bidInvitationMapper.selectBatchIds(invitationIds).stream()
            .filter(Objects::nonNull)
            .map(BidInvitation::getProjectId).filter(Objects::nonNull).distinct()
            .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return false;
        }
        return projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .in(ProjectMember::getProjectId, projectIds)
                .eq(ProjectMember::getPersonId, actor.id())
                .isNull(ProjectMember::getExitDate))
            > 0;
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
