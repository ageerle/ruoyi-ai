package org.ruoyi.ipd.service;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.service.executor.CertTemplateSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.PersonSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.ProductSoftDeleteExecutor;
import org.ruoyi.ipd.service.executor.ProjectSoftDeleteExecutor;
import org.ruoyi.ipd.domain.SoftDeletable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * P0-6.2 审批通过后软删除原子执行（G-11 硬约束：一致性）。
 *
 * <p>事务边界（绝对禁止拆分）：<b>审核状态变更 + 目标实体 softDelete + 审计落库 三者同事务</b>，
 * 任一失败全回滚；spring-tx 在 @Transactional(rollbackFor = Exception.class) 默认 RuntimeException + Error 已回滚，
 * 这里显式补上 checked exception 兜底，避免业务异常导致数据不一致。
 *
 * <p>关键约束（来自上轮反思）：
 * <ul>
 *   <li>audit.append() 必须在 softDelete() 之前/之后同事务调，绝不能跨事务边界</li>
 *   <li>不允许在 softDelete() 后单独调用 audit.append()(曾经出 bug)</li>
 *   <li>不允许 catch 后只 append audit(必须抛出，由 @Transactional 回滚)</li>
 * </ul>
 *
 * <p>状态机：DRAFT → LEADER_REVIEW → ADMIN_REVIEW → DELETED / REJECTED；
 * approveAndExecute 在 ADMIN_REVIEW → DELETED 阶段执行。已在 DRAFT/LEADER_REVIEW/REJECTED
 * 阶段调用将抛 ServiceException，由 @Transactional 回滚（无副作用）。
 */
@Service
@Slf4j
public class DeleteAuditService {

    /** 审计 action：审核通过且目标行已被软删 */
    public static final String ACTION_DELETE_EXECUTE = "DELETE_EXECUTE";
    /** 审计 action：审核通过但目标行已不存在或已被软删（幂等成功） */
    public static final String ACTION_DELETE_NOOP = "DELETE_NOOP";

    private final DeletionRequestMapper deletionRequestMapper;
    private final AuditLogService auditLogService;
    private final Map<String, SoftDeleteExecutor<?>> executorsByType;

    public DeleteAuditService(DeletionRequestMapper deletionRequestMapper,
                              AuditLogService auditLogService,
                              List<SoftDeleteExecutor<?>> executors) {
        this.deletionRequestMapper = deletionRequestMapper;
        this.auditLogService = auditLogService;
        this.executorsByType = executors.stream()
            .collect(Collectors.toMap(SoftDeleteExecutor::entityType, Function.identity()));
    }

    /**
     * 超管终审通过后，原子执行软删除与审计（P0-6.2）。
     * <p>调用前请求必须处于 ADMIN_REVIEW；调用后状态变为 DELETED 并填 executedAt。
     * 任意环节抛异常，状态变更、目标行 UPDATE、审计全部回滚。
     *
     * @param requestId DeletionRequest.id
     * @param adminId   操作超管 ID（必填）
     * @return 终态 DeletionRequest
     * @throws ServiceException 状态机不匹配 / entity_type 不支持 / 软删除执行失败
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public DeletionRequest approveAndExecute(Long requestId, Long adminId) {
        if (adminId == null) {
            throw new ServiceException("adminId 不能为空");
        }
        DeletionRequest request = deletionRequestMapper.selectById(requestId);
        if (request == null) {
            throw new ServiceException("删除申请不存在: " + requestId);
        }
        if (!DeletionRequestService.ST_ADMIN_REVIEW.equals(request.getStatus())) {
            throw new ServiceException("状态机不匹配：期望 ADMIN_REVIEW，实际 " + request.getStatus());
        }

        SoftDeleteExecutor<?> executor = executorsByType.get(request.getEntityType());
        if (executor == null) {
            throw new ServiceException("不支持的 entity_type: " + request.getEntityType());
        }

        // 步骤 1：标记申请终态 DELETED（update 提交时刻受事务管辖）
        request.setStatus(DeletionRequestService.ST_DELETED);
        request.setAdminId(adminId);
        request.setAdminDecision("APPROVE");
        request.setAdminDecidedAt(new Date());
        request.setExecutedAt(new Date());
        int updated = deletionRequestMapper.updateById(request);
        if (updated != 1) {
            throw new ServiceException("删除申请状态更新失败: id=" + requestId);
        }

        // 步骤 2：先判定 NOOP，再在同一事务内执行软删除（删后查 del_flag 恒为 1，无法区分）
        boolean noop = executor.isDeleted(request.getEntityId());
        try {
            if (!noop) {
                executor.softDelete(request.getEntityId());
            }
        } catch (RuntimeException e) {
            // 关键：不 catch 后吞；让 @Transactional 回滚
            log.warn("[DeleteAudit] softDelete 失败回滚: requestId={}, entity={}, id={}",
                requestId, request.getEntityType(), request.getEntityId(), e);
            throw e;
        }

        // 步骤 3：同事务内写审计（绝不能跨事务或 catch 异常后补写）
        AuditLog logEntry = AuditLog.builder()
            .operatorId(adminId)
            .action(noop ? ACTION_DELETE_NOOP : ACTION_DELETE_EXECUTE)
            .entityType(request.getEntityType())
            .entityId(request.getEntityId())
            .reason("deletion_request:" + requestId)
            .createTime(new Date())
            .build();
        auditLogService.append(logEntry);

        return request;
    }

    /** 当前已注册的 entity_type（用于校验/调试） */
    public Set<String> supportedEntityTypes() {
        return executorsByType.keySet();
    }

    // 显式列出 4 个内置类型，让 IDE/编译器在缺实现时给出警告
    @SuppressWarnings("unused")
    private static final List<Class<?>> REGISTERED_EXECUTORS = List.of(
        ProjectSoftDeleteExecutor.class,
        ProductSoftDeleteExecutor.class,
        PersonSoftDeleteExecutor.class,
        CertTemplateSoftDeleteExecutor.class
    );

    /** 静态类型约束：确保所有受支持类型都实现 {@link SoftDeletable} */
    static {
        Class<?>[] mustImplement = new Class<?>[] {
            org.ruoyi.ipd.domain.Project.class,
            org.ruoyi.ipd.domain.Product.class,
            org.ruoyi.ipd.domain.Person.class,
            org.ruoyi.ipd.domain.CertTemplate.class
        };
        for (Class<?> c : mustImplement) {
            if (!SoftDeletable.class.isAssignableFrom(c)) {
                throw new ExceptionInInitializerError(c.getName() + " 必须实现 SoftDeletable");
            }
        }
    }
}