package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.util.Workdays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.List;

/**
 * 删除审核引擎（BR-DEL / F29：两级差异化审核；G-02 禁止直接物理删除）
 * 状态机：DRAFT → LEADER_REVIEW → ADMIN_REVIEW → DELETED / REJECTED；终态不可逆。
 * 期限：组长 2 工作日（deletion.leaderDeadlineDays）、超管 2 工作日（deletion.adminDeadlineDays），
 *       组长逾期自动升级超管；撤回时限 deletion.withdrawHours=24。
 * P0-6.2：超管通过必须经 {@link DeleteAuditService#approveAndExecute} 原子软删目标行（AC-DEL-02），
 * 禁止仅改申请态为 DELETED。
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
    private final DeleteAuditService deleteAuditService;
    /** ROOT-R3-P0-1：跨状态机守卫（可选注入，nullable 兼容旧测试） */
    @Autowired(required = false)
    private org.ruoyi.ipd.service.StateMachineGuard stateMachineGuard;
    /** ROOT-R1 P0-7 字面量迁移：删除申请配置（冷静期/升级超时；B-RULE-05 配套）来源 */
    @Autowired(required = false)
    private BusinessConfigService businessConfigService;

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
        // ROOT-R3-P0-1：守卫 preCheck（跨域联动合法性校验）—— DRAFT->LEADER_REVIEW 合法
        preCheckGuard("deletion_request", "DRAFT", DeletionRequestService.ST_LEADER_REVIEW, "submit");
        deletionRequestMapper.insert(request);
        audit(entityType, entityId, requesterId, "DELETE_REQUEST_SUBMIT", request.getId());
        registerPostCommit("deletion_request", "DRAFT", DeletionRequestService.ST_LEADER_REVIEW, "submit", requesterId, request.getId());
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
        int withdrawHours;
        // ROOT-R1 P0-7：先读 BusinessConfigService.DELETION_ESCALATE_TIMEOUT_HOURS，回退 SystemConfig
        Integer bv = readBusinessInt(BusinessConfigKeys.DELETION_ESCALATE_TIMEOUT_HOURS);
        if (bv != null) {
            withdrawHours = bv;
        } else {
            withdrawHours = systemConfigService.getIntValue("deletion.withdrawHours", 24);
        }
        Date deadline = new Date(request.getCreateTime().getTime() + withdrawHours * 3600_000L);
        if (new Date().after(deadline)) {
            throw new ServiceException("已超过 " + withdrawHours + " 小时撤回时限");
        }
        // ROOT-R3-P0-1：守卫 preCheck —— *->WITHDRAWN 通配收敛
        preCheckGuard("deletion_request", request.getStatus(), DeletionRequestService.ST_WITHDRAWN, "withdraw");
        request.setStatus(ST_WITHDRAWN);
        deletionRequestMapper.updateById(request);
        audit(request.getEntityType(), request.getEntityId(), requesterId, "DELETE_REQUEST_WITHDRAW", request.getId());
        registerPostCommit("deletion_request", request.getStatus(), DeletionRequestService.ST_WITHDRAWN, "withdraw", requesterId, request.getId());
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
        String target = approve ? ST_ADMIN_REVIEW : ST_REJECTED;
        String trigger = approve ? "leaderApprove" : "leaderReject";
        // ROOT-R3-P0-1：守卫 preCheck（LEADER_REVIEW -> ADMIN_REVIEW/REJECTED 合法）
        preCheckGuard("deletion_request", DeletionRequestService.ST_LEADER_REVIEW, target, trigger);
        request.setStatus(approve ? ST_ADMIN_REVIEW : ST_REJECTED);
        if (approve) {
            request.setAdminDueAt(Workdays.add(new Date(), adminDeadlineDays()));
        }
        deletionRequestMapper.updateById(request);
        // ROOT-R3-P0-1：postCommit 跨域副作用
        registerPostCommit("deletion_request", DeletionRequestService.ST_LEADER_REVIEW, target, trigger, leaderId, request.getId());
        audit(request.getEntityType(), request.getEntityId(), leaderId, approve ? "DELETE_LEADER_APPROVE" : "DELETE_LEADER_REJECT", request.getId());
        return request;
    }

    /**
     * 超管终审。
     * <p>APPROVE → 委托 {@link DeleteAuditService#approveAndExecute}：申请态 + 目标软删 + 审计同事务；
     * REJECT → 仅标 REJECTED 并写审计（不触碰目标行）。
     *
     * @param requestId 申请 ID
     * @param adminId   超管 ID
     * @param approve   true=通过并软删；false=驳回
     * @param opinion   意见（驳回时写入审计 reason 后缀）
     * @return 终态申请
     */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest adminDecision(Long requestId, Long adminId, boolean approve, String opinion) {
        DeletionRequest request = getOrThrow(requestId);
        requireStatus(request, ST_ADMIN_REVIEW);
        if (approve) {
            // ROOT-R3-P0-1：守卫 preCheck —— ADMIN_REVIEW -> DELETED 合法（跨域→触发原子软删）
            preCheckGuard("deletion_request", DeletionRequestService.ST_ADMIN_REVIEW, DeletionRequestService.ST_DELETED, "adminApprove");
            // P0-6.2 / AC-DEL-02：必须走原子软删，禁止只改申请态
            DeletionRequest deleted = deleteAuditService.approveAndExecute(requestId, adminId);
            // ROOT-R3-P0-1：postCommit 跨域副作用（事务提交后触发）
            registerPostCommit("deletion_request", DeletionRequestService.ST_ADMIN_REVIEW, DeletionRequestService.ST_DELETED, "adminApprove", adminId, requestId);
            return deleted;
        }
        // ROOT-R3-P0-1：守卫 preCheck —— ADMIN_REVIEW -> REJECTED 合法
        preCheckGuard("deletion_request", DeletionRequestService.ST_ADMIN_REVIEW, DeletionRequestService.ST_REJECTED, "adminReject");
        request.setAdminId(adminId);
        request.setAdminDecision("REJECT");
        request.setAdminDecidedAt(new Date());
        request.setStatus(ST_REJECTED);
        deletionRequestMapper.updateById(request);
        audit(request.getEntityType(), request.getEntityId(), adminId, "DELETE_ADMIN_REJECT", request.getId());
        return request;
    }

    /**
     * 组长逾期升级：LEADER_REVIEW 且 leaderDueAt 已过 → 转 ADMIN_REVIEW；返回升级条数。
     *
     * <p>PERF-P0-1：原实现走 N+1（selectList + 每行 updateById + 每行 audit append ≈ 500 SQL/百条），
     * 现改为单 SQL 条件批量 UPDATE + 补逐条审计（G-02 语义不变）。命中
     * {@code idx_del_status_leader(status, leader_due_at)}，无逾期时短路零额外 SQL 开销。
     *
     * <p>步骤：① selectList 拿受影响行（含 id + entityType/entityId 供 audit 用）→ ② 单 SQL
     * 条件批量 UPDATE（status='ADMIN_REVIEW' AND leader_due_at < now 谓词）→ ③ affected=0
     * 直接返回 0；否则按预取行逐条写 DELETE_LEADER_OVERDUE_ESCALATE 审计（保持 G-02 一行一审）。
     * 事务在单 SQL UPDATE + 逐条 audit 都成功后整体提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public int escalateOverdueLeaderReview() {
        Date now = new Date();
        // 步骤 ①：先用同谓词 selectList 拿受影响行的 id / entityType / entityId（供 audit 用）
        List<DeletionRequest> overdue = deletionRequestMapper.selectList(new LambdaQueryWrapper<DeletionRequest>()
            .eq(DeletionRequest::getStatus, ST_LEADER_REVIEW)
            .lt(DeletionRequest::getLeaderDueAt, now));
        if (overdue.isEmpty()) {
            return 0; // affected=0 短路：零 SQL 额外开销
        }
        Date adminDueAt = Workdays.add(now, adminDeadlineDays());
        // 步骤 ②：单 SQL 条件批量 UPDATE（PERF-P0-1：消除 N+1 写放大）
        int affected = deletionRequestMapper.update(null, new LambdaUpdateWrapper<DeletionRequest>()
            .set(DeletionRequest::getStatus, ST_ADMIN_REVIEW)
            .set(DeletionRequest::getAdminDueAt, adminDueAt)
            .eq(DeletionRequest::getStatus, ST_LEADER_REVIEW)
            .lt(DeletionRequest::getLeaderDueAt, now));
        if (affected == 0) {
            // 谓词扫描与 UPDATE 之间发生状态变迁（极少见——并发方抢先处置）：同样短路
            return 0;
        }
        // ROOT-R3-P0-1：守卫 preCheck —— LEADER_REVIEW -> ADMIN_REVIEW 合法（升级路径）
        preCheckGuard("deletion_request", DeletionRequestService.ST_LEADER_REVIEW, DeletionRequestService.ST_ADMIN_REVIEW, "escalateOverdue");
        // 步骤 ③：按预取行补逐条审计（G-02 语义不变：每条升级单独留痕，可被审计范围查询到）
        for (DeletionRequest request : overdue) {
            audit(request.getEntityType(), request.getEntityId(), null, "DELETE_LEADER_OVERDUE_ESCALATE", request.getId());
        }
        return affected;
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
        // ROOT-R1 P0-7：先读 BusinessConfigService.DELETION_COOLDOWN_DAYS，回退 SystemConfig
        Integer v = readBusinessInt(BusinessConfigKeys.DELETION_COOLDOWN_DAYS);
        if (v != null) return v;
        return systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2);
    }

    private int adminDeadlineDays() {
        // ROOT-R1 P0-7：先读 BusinessConfigService.DELETION_COOLDOWN_DAYS，回退 SystemConfig
        Integer v = readBusinessInt(BusinessConfigKeys.DELETION_COOLDOWN_DAYS);
        if (v != null) return v;
        return systemConfigService.getIntValue("deletion.adminDeadlineDays", 2);
    }

    /**
     * ROOT-R1 P0-7：读 BusinessConfigService 整数；未注入或抛错返回 null（让调用方走 SystemConfig 回退）。
     */
    private Integer readBusinessInt(String key) {
        if (businessConfigService == null) return null;
        try {
            return businessConfigService.getInt(key);
        } catch (Exception ex) {
            return null;
        }
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

    /**
     * ROOT-R3-P0-1：守卫 preCheck 包装（无守卫注入时降级 no-op，兼容旧测试）
     */
    private void preCheckGuard(String entityType, String fromState, String toState, String trigger) {
        if (stateMachineGuard == null) {
            return; // 未注入守卫 → 降级 no-op
        }
        stateMachineGuard.preCheck(entityType, fromState, toState, trigger);
    }

    /**
     * ROOT-R3-P0-1：注册 postCommit 副作用（在事务提交后触发，避免回滚后污染）
     * 无守卫注入时降级 no-op；无事务上下文时直接执行（向后兼容）。
     */
    private void registerPostCommit(String entityType, String fromState, String toState,
                                    String trigger, Long operatorId, Long entityId) {
        if (stateMachineGuard == null) {
            return; // 未注入守卫 → 降级 no-op
        }
        java.util.Date occurredAt = new java.util.Date();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
                }
            });
        } else {
            // 无事务上下文（测试场景）—— 直接执行
            stateMachineGuard.postCommit(entityType, fromState, toState, trigger, operatorId, entityId, occurredAt);
        }
    }
}
