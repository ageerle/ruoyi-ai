package org.ruoyi.ipd.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * P0-6.4 归档区数据管理（第二阶段清除流程）：
 * - 第一阶段（P0-6.2 已 done）：DRAFT→LEADER_REVIEW→ADMIN_REVIEW→DELETED 状态机；目标行软删除
 * - 第二阶段（本服务）：DELETED 后归档区列表 + 二次确认清除 + 清除动作审计
 *
 * 数据模型说明：owner DDL 无 purge_at 列；本服务复用 DeletionRequest.remark 字段标记"已清除"——以 'PURGED_BY_SUPER_ADMIN:<adminId>@<epoch_ms>' 前缀。
 * 这避免修改 owner DDL 与 entity，符合 allowedPaths 限定（仅 DeletionRequest 相关）。
 *
 * 引用规则：DELETE 后行已被各域 SoftDeleteExecutor 软删（目标表 del_flag='1'）；PURGE 只清 DeletionRequest 自身记录，
 * 目标行物理删除由各域 owner 工作流控制，不在本卡范围内。AC-DEL-08 要求"清除动作本身再写一条审计"已满足。
 *
 * 身份校验：本服务不依赖 owner 已移除的 IpdActor/IpdPermission 抽象；通过 PersonMapper 查 personType='SUPER_ADMIN'
 * 直接校验，避免侵入 owner 的 IpdAuthService 范围。
 */
@Service
@RequiredArgsConstructor
public class DeletionArchiveService {

    /** remark 前缀；列表查询过滤掉已 PURGED 的条目，避免重复清除。 */
    public static final String PURGED_MARK = "PURGED_BY_SUPER_ADMIN:";

    private final DeletionRequestMapper deletionRequestMapper;
    private final AuditLogService auditLogService;
    private final PersonMapper personMapper;

    /** 归档区列表：DELETED 状态 + remark 非 PURGED 前缀（仅超管可访问）。 */
    public List<DeletionRequest> listArchive() {
        requireSuperAdmin();
        return deletionRequestMapper.selectList(new LambdaQueryWrapper<DeletionRequest>()
            .eq(DeletionRequest::getStatus, DeletionRequestService.ST_DELETED)
            .notLike(DeletionRequest::getRemark, PURGED_MARK)
            .orderByDesc(DeletionRequest::getExecutedAt));
    }

    /**
     * 二次确认清除：从 Sa-Token 取当前登录人 → 校验是 SUPER_ADMIN → 原子更新 remark → 写 PURGE 审计。
     * 返回更新后的 DeletionRequest。同一记录只能 purge 一次。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeletionRequest purge(Long requestId) {
        Long adminId = requireSuperAdmin();
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
        // 清除动作本身必须再写一条审计（AC-DEL-08）：PURGE 操作独立写一条审计，与软删除审计区分
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

    private Long requireSuperAdmin() {
        // StpUtil.getLoginId() 返回 String（Sa-Token API 契约），必须走 getLoginIdAsLong()
        Long adminId;
        try {
            adminId = StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            throw new ServiceException("未认证", ApiV1ErrorCode.UNAUTHORIZED.getCode());
        }
        Person person = personMapper.selectById(adminId);
        if (person == null) {
            throw new ServiceException("人员不存在: " + adminId, ApiV1ErrorCode.NOT_FOUND.getCode());
        }
        if (!"SUPER_ADMIN".equals(person.getPersonType())) {
            throw new ServiceException("仅超管可执行归档清除（二次确认）", ApiV1ErrorCode.FORBIDDEN.getCode());
        }
        return adminId;
    }
}