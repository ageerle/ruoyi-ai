package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.util.Workdays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 删除审核引擎（BR-DEL / F29：两级差异化审核；G-02 禁止直接物理删除）
 * 状态机：DRAFT → LEADER_REVIEW → ADMIN_REVIEW → DELETED / REJECTED；终态不可逆。
 * 期限：组长 2 工作日（deletion.leaderDeadlineDays）、超管 2 工作日（deletion.adminDeadlineDays），
 *       组长逾期自动升级超管；撤回时限 deletion.withdrawHours=24。
 * ⚠️ 本引擎只负责申请流与判定记录；DELETED 后的目标行软删除由各域服务在 execute 钩子后自行完成
 *    （引擎不感知各表结构），全程写审计。
 */
@Service
@RequiredArgsConstructor
public class DeletionRequestService {

    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_LEADER_REVIEW = "LEADER_REVIEW";
    public static final String ST_ADMIN_REVIEW = "ADMIN_REVIEW";
    public static final String ST_DELETED = "DELETED";
    public static final String ST_REJECTED = "REJECTED";
    public static final String ST_WITHDRAWN = "WITHDRAWN";

    private final DeletionRequestMapper deletionRequestMapper;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    /** 提交删除申请：存快照、进组长初审、算期限、写审计 */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest submit(String entityType, Long entityId, String snapshot, String reason, Long requesterId) {
        DeletionRequest request = DeletionRequest.builder()
            .entityType(entityType)
            .entityId(entityId)
            .entitySnapshot(snapshot)
            .reason(reason)
            .requesterId(requesterId)
            .status(ST_LEADER_REVIEW)
            .leaderDueAt(Workdays.add(new Date(), leaderDeadlineDays()))
            .build();
        // ⚠️ @Builder 只覆盖本类字段，BaseEntity 的 createTime 须走 setter
        request.setCreateTime(new Date());
        deletionRequestMapper.insert(request);
        audit(entityType, entityId, requesterId, "DELETE_REQUEST_SUBMIT", request.getId());
        return request;
    }

    /** 撤回：仅申请人在 withdrawHours 内且未终态 */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest withdraw(Long requestId, Long requesterId) {
        DeletionRequest request = getOrThrow(requestId);
        if (!request.getRequesterId().equals(requesterId)) {
            throw new ServiceException("仅申请人可撤回");
        }
        if (isTerminal(request.getStatus())) {
            throw new ServiceException("已终态，不可撤回");
        }
        int withdrawHours = systemConfigService.getIntValue("deletion.withdrawHours", 24);
        Date deadline = new Date(request.getCreateTime().getTime() + withdrawHours * 3600_000L);
        if (new Date().after(deadline)) {
            throw new ServiceException("已超过 " + withdrawHours + " 小时撤回时限");
        }
        request.setStatus(ST_WITHDRAWN);
        deletionRequestMapper.updateById(request);
        audit(request.getEntityType(), request.getEntityId(), requesterId, "DELETE_REQUEST_WITHDRAW", request.getId());
        return request;
    }

    /** 组长初审：APPROVE → 超管终审；REJECT → 终态 */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest leaderDecision(Long requestId, Long leaderId, boolean approve, String opinion) {
        DeletionRequest request = getOrThrow(requestId);
        requireStatus(request, ST_LEADER_REVIEW);
        request.setLeaderId(leaderId);
        request.setLeaderDecision(approve ? "APPROVE" : "REJECT");
        request.setLeaderDecidedAt(new Date());
        request.setStatus(approve ? ST_ADMIN_REVIEW : ST_REJECTED);
        if (approve) {
            request.setAdminDueAt(Workdays.add(new Date(), adminDeadlineDays()));
        }
        deletionRequestMapper.updateById(request);
        audit(request.getEntityType(), request.getEntityId(), leaderId, approve ? "DELETE_LEADER_APPROVE" : "DELETE_LEADER_REJECT", request.getId());
        return request;
    }

    /** 超管终审：APPROVE → DELETED（目标行软删除由域服务执行）；REJECT → 终态 */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest adminDecision(Long requestId, Long adminId, boolean approve, String opinion) {
        DeletionRequest request = getOrThrow(requestId);
        requireStatus(request, ST_ADMIN_REVIEW);
        request.setAdminId(adminId);
        request.setAdminDecision(approve ? "APPROVE" : "REJECT");
        request.setAdminDecidedAt(new Date());
        request.setStatus(approve ? ST_DELETED : ST_REJECTED);
        if (approve) {
            request.setExecutedAt(new Date());
        }
        deletionRequestMapper.updateById(request);
        audit(request.getEntityType(), request.getEntityId(), adminId, approve ? "DELETE_ADMIN_APPROVE" : "DELETE_ADMIN_REJECT", request.getId());
        return request;
    }

    /** 组长逾期升级：LEADER_REVIEW 且 leaderDueAt 已过 → 转 ADMIN_REVIEW；返回升级条数 */
    @Transactional(rollbackFor = Exception.class)
    public int escalateOverdueLeaderReview() {
        List<DeletionRequest> overdue = deletionRequestMapper.selectList(new LambdaQueryWrapper<DeletionRequest>()
            .eq(DeletionRequest::getStatus, ST_LEADER_REVIEW)
            .lt(DeletionRequest::getLeaderDueAt, new Date()));
        for (DeletionRequest request : overdue) {
            request.setStatus(ST_ADMIN_REVIEW);
            request.setAdminDueAt(Workdays.add(new Date(), adminDeadlineDays()));
            deletionRequestMapper.updateById(request);
            audit(request.getEntityType(), request.getEntityId(), null, "DELETE_LEADER_OVERDUE_ESCALATE", request.getId());
        }
        return overdue.size();
    }

    /** 超管逾期清单（仅提醒，不自动通过——涉删权限保守处理） */
    public List<DeletionRequest> listOverdueAdminReview() {
        return deletionRequestMapper.selectList(new LambdaQueryWrapper<DeletionRequest>()
            .eq(DeletionRequest::getStatus, ST_ADMIN_REVIEW)
            .lt(DeletionRequest::getAdminDueAt, new Date()));
    }

    private DeletionRequest getOrThrow(Long id) {
        DeletionRequest request = deletionRequestMapper.selectById(id);
        if (request == null) {
            throw new ServiceException("删除申请不存在: " + id);
        }
        return request;
    }

    private void requireStatus(DeletionRequest request, String expect) {
        if (!expect.equals(request.getStatus())) {
            throw new ServiceException("状态机不匹配：期望 " + expect + "，实际 " + request.getStatus());
        }
    }

    private boolean isTerminal(String status) {
        return ST_DELETED.equals(status) || ST_REJECTED.equals(status) || ST_WITHDRAWN.equals(status);
    }

    private int leaderDeadlineDays() {
        return systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2);
    }

    private int adminDeadlineDays() {
        return systemConfigService.getIntValue("deletion.adminDeadlineDays", 2);
    }

    private void audit(String entityType, Long entityId, Long operatorId, String action, Long requestId) {
        AuditLog log = AuditLog.builder()
            .operatorId(operatorId)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .reason("deletion_request:" + requestId)
            .createTime(new Date())
            .build();
        auditLogService.append(log);
    }
}