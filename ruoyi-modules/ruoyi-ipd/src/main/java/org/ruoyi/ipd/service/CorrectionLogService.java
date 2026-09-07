package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.CorrectionLog;
import org.ruoyi.ipd.mapper.CorrectionLogMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 字段更正留痕服务（P3-2.2 更正留痕子模块）。
 *
 * <p>写入路径：record() 一律落审计留痕；写入语义为「追加」而非「覆盖」。
 * 读取路径：listByEntity() 仅超管可访问（SEC-API-01 同严）。
 *
 * <p>必填字段校验（PARAM_INVALID 即报错）：
 * <ul>
 *   <li>entityType / entityId / fieldName / reason / newValue 五者非空；oldValue 允许 null（首次建值场景）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CorrectionLogService {

    private final CorrectionLogMapper correctionLogMapper;
    private final IpdPermission permission;

    /**
     * 写入一条字段更正留痕。
     *
     * <p>旧值与新值相同时视为 NO-OP，不入库；reason 必填（强约束业务责任人）。
     *
     * @param actor      当前操作人（必填，可为 requireInternal 结果）
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @param fieldName  字段名
     * @param oldValue   旧值（允许 null）
     * @param newValue   新值
     * @param reason     更正原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(IpdActor actor, String entityType, Long entityId, String fieldName,
                       String oldValue, String newValue, String reason) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (isBlank(entityType)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "entityType 不能为空");
        }
        if (entityId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "entityId 不能为空");
        }
        if (isBlank(fieldName)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "fieldName 不能为空");
        }
        if (newValue == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "newValue 不能为空");
        }
        if (isBlank(reason)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "reason 不能为空");
        }
        // NO-OP 短路：旧值与新值完全相同不写库（避免审计噪声）
        if (safeEquals(oldValue, newValue)) {
            return;
        }

        CorrectionLog row = CorrectionLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .fieldName(fieldName)
            .oldValue(oldValue)
            .newValue(newValue)
            .reason(reason.trim())
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatedAt(new Date())
            .build();
        permission.bindCreateAudit(row, actor);
        correctionLogMapper.insert(row);
    }

    /**
     * 按实体维度反查更正历史，按 operatedAt DESC。
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<CorrectionLog> listByEntity(String entityType, Long entityId) {
        permission.requireAdmin();
        if (isBlank(entityType)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "entityType 不能为空");
        }
        if (entityId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "entityId 不能为空");
        }
        return correctionLogMapper.selectList(
            new LambdaQueryWrapper<CorrectionLog>()
                .eq(CorrectionLog::getEntityType, entityType)
                .eq(CorrectionLog::getEntityId, entityId)
                .orderByDesc(CorrectionLog::getOperatedAt));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}
