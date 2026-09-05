package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * P0-6.4 归档区数据管理（第二阶段清除流程）。
 * SEC-API-01：身份一律经 IpdPermission.requireAdmin()，禁止 StpUtil 旁路。
 */
@Service
@RequiredArgsConstructor
public class DeletionArchiveService {

    /** remark 前缀；列表查询过滤掉已 PURGED 的条目，避免重复清除。 */
    public static final String PURGED_MARK = "PURGED_BY_SUPER_ADMIN:";

    private final DeletionRequestMapper deletionRequestMapper;
    private final AuditLogService auditLogService;
    private final IpdPermission ipdPermission;

    /**
     * 归档区列表：DELETED 状态 + remark 非 PURGED 前缀（仅超管）。
     *
     * <p>DEF-8（2026-09-05）：原实现只用 {@code notLike(remark, PURGED_MARK)}，而 SQL 是**三值逻辑**——
     * {@code NULL NOT LIKE '%x%'} 求值为 NULL（非 TRUE），WHERE 不成立。remark 默认为 NULL
     * （submit/终审均不写 remark），于是归档区**恒空**：真库实证 2 行 DELETED 且 remark IS NULL，
     * {@code SUM(remark NOT LIKE '%PURGED%')}=NULL（0 行通过）而正确语义应为 2 行 →
     * AC-DEL-02「数据移入归档区」的可见性完全失效（purge 入口也因此永远拿不到候选）。
     * 修复 = 显式放行 NULL：{@code (remark IS NULL OR remark NOT LIKE '%PURGED_BY_SUPER_ADMIN:%')}。
     *
     * @return 未清除的已删除申请
     */
    public List<DeletionRequest> listArchive() {
        ipdPermission.requireAdmin();
        return deletionRequestMapper.selectList(new LambdaQueryWrapper<DeletionRequest>()
            .eq(DeletionRequest::getStatus, DeletionRequestService.ST_DELETED)
            // DEF-8：NULL 安全——notLike 单独用会把 remark IS NULL 的行整条排除
            .and(w -> w.isNull(DeletionRequest::getRemark)
                .or().notLike(DeletionRequest::getRemark, PURGED_MARK))
            .orderByDesc(DeletionRequest::getExecutedAt));
    }

    /**
     * 二次确认清除：IPD 超管会话 → 原子更新 remark → 写 PURGE 审计。
     *
     * @param requestId 删除申请 ID
     * @return 更新后的申请
     */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest purge(Long requestId) {
        IpdActor admin = ipdPermission.requireAdmin();
        Long adminId = admin.id();
        DeletionRequest request = deletionRequestMapper.selectById(requestId);
        if (request == null) {
            throw new ServiceException("删除申请不存在: " + requestId, ApiV1ErrorCode.NOT_FOUND.getCode());
        }
        if (!DeletionRequestService.ST_DELETED.equals(request.getStatus())) {
            throw new ServiceException("仅 DELETED 终态可清除：当前 " + request.getStatus(),
                ApiV1ErrorCode.STATE_CONFLICT.getCode());
        }
        if (request.getRemark() != null && request.getRemark().startsWith(PURGED_MARK)) {
            throw new ServiceException("该记录已清除，不可二次清除", ApiV1ErrorCode.STATE_CONFLICT.getCode());
        }
        long ts = System.currentTimeMillis();
        String purgeMark = PURGED_MARK + adminId + "@" + ts;
        int updated = deletionRequestMapper.update(null, new LambdaUpdateWrapper<DeletionRequest>()
            .eq(DeletionRequest::getId, requestId)
            .eq(DeletionRequest::getStatus, DeletionRequestService.ST_DELETED)
            .notLike(DeletionRequest::getRemark, PURGED_MARK)
            .set(DeletionRequest::getRemark, purgeMark)
            .set(DeletionRequest::getUpdateBy, adminId)
            .set(DeletionRequest::getUpdateTime, new Date(ts)));
        if (updated != 1) {
            throw new ServiceException("清除冲突：并发或已清除", ApiV1ErrorCode.STATE_CONFLICT.getCode());
        }
        auditLogService.append(AuditLog.builder()
            .operatorId(adminId)
            .action("DELETE_ARCHIVE_PURGE")
            .entityType(request.getEntityType())
            .entityId(request.getEntityId())
            .reason("deletion_request:" + requestId + ";purge_by:" + adminId)
            .createTime(new Date(ts))
            .build());
        DeletionRequest refreshed = deletionRequestMapper.selectById(requestId);
        if (refreshed == null) {
            throw new ServiceException("清除后无法读取记录", ApiV1ErrorCode.INTERNAL_ERROR.getCode());
        }
        return refreshed;
    }
}
