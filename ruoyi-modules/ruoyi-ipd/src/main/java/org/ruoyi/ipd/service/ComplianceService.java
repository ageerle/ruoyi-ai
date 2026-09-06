package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.dto.AuditEntryVO;
import org.ruoyi.ipd.dto.DataDeletionRequestDTO;
import org.ruoyi.ipd.dto.DataDeletionRequestVO;
import org.ruoyi.ipd.dto.DataRetentionRuleVO;
import org.ruoyi.ipd.dto.PermissionSeparationVO;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 合规卡服务（P2-5.1；ZK-IPD §九 合规）。
 *
 * <p>提供 4 个核心能力：
 * <ul>
 *   <li>数据保留规则查询（{@link #getRetentionRules()}）—— sys_config 兜底 + 默认值</li>
 *   <li>数据删除请求创建（{@link #createDeletionRequest}）—— 30 天 deadline + 强制审计</li>
 *   <li>资源审计链查询（{@link #getAuditTrail}）—— 按 resourceType+resourceId + 角色范围</li>
 *   <li>R/W 权限分离判定（{@link #checkPermissionSeparation}）—— AC-COMP-05</li>
 * </ul>
 *
 * <p>约束（G-02 / BR-COMP-AUDIT）：写操作必审计；本服务对 {@link AuditLogService#append} 单点收口，
 * 不接受任何「绕过审计」的写路径。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    /** 个保法 / GDPR Art.12.3 默认响应时限：30 自然日（owner 拍板，写死常量）。 */
    public static final int DELETION_DEADLINE_DAYS = 30;

    /** 默认保留规则兜底。 */
    private static final int DEFAULT_RETENTION_DAYS = 2557; // 7 年
    private static final String DEFAULT_DELETION_POLICY = "SOFT_DELETE";
    private static final String DEFAULT_LEGAL_BASIS = "DSL-内部留存";

    /** sys_config 配置前缀：{@code compliance.retention.<resourceType>} → JSON。 */
    private static final String RETENTION_CONFIG_PREFIX = "compliance.retention.";

    /** AC-COMP-05：合规域 R/W 权限码字面量。 */
    public static final String PERM_COMPLIANCE_READ = "ipd:compliance:read";
    public static final String PERM_COMPLIANCE_WRITE = "ipd:compliance:write";

    /**
     * AC-COMP-05 R/W 来源映射（owner 拍板；与 IpdPermissionCode 字符串一致）。
     * <ul>
     *   <li>SUPER_ADMIN → 持 R + W（治理角色豁免，conflict=true 但需超管审视）</li>
     *   <li>GROUP_LEADER → 持 R + W（组长可代提删申请，conflict=true 警告）</li>
     *   <li>MARKET_PM / RD_PM → 仅持 R（合法）</li>
     * </ul>
     */
    private static final Set<String> ROLES_WITH_WRITE = Set.of("SUPER_ADMIN", "GROUP_LEADER");
    private static final Set<String> ROLES_WITH_READ = Set.of("SUPER_ADMIN", "GROUP_LEADER", "MARKET_PM", "RD_PM");

    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;
    private final PersonMapper personMapper;
    private final ObjectMapper objectMapper;

    /**
     * AC-COMP-01：返回数据保留规则清单（按 sys_config + 默认兜底）。
     * <p>预置规则覆盖：projects / requirements / persons / audit_logs / deletion_requests / ai_documents。
     *
     * @return 规则列表
     */
    public List<DataRetentionRuleVO> getRetentionRules() {
        List<String> known = List.of(
            "projects", "requirements", "persons", "audit_logs", "deletion_requests", "ai_documents"
        );
        List<DataRetentionRuleVO> out = new ArrayList<>(known.size());
        for (String rt : known) {
            String raw = systemConfigService.getValue(RETENTION_CONFIG_PREFIX + rt, null);
            if (raw == null || raw.isBlank()) {
                out.add(DataRetentionRuleVO.builder()
                    .resourceType(rt)
                    .retentionDays(DEFAULT_RETENTION_DAYS)
                    .deletionPolicy(DEFAULT_DELETION_POLICY)
                    .legalBasis(DEFAULT_LEGAL_BASIS)
                    .build());
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(raw);
                out.add(DataRetentionRuleVO.builder()
                    .resourceType(rt)
                    .retentionDays(node.path("retentionDays").asInt(DEFAULT_RETENTION_DAYS))
                    .deletionPolicy(node.path("deletionPolicy").asText(DEFAULT_DELETION_POLICY))
                    .legalBasis(node.path("legalBasis").asText(DEFAULT_LEGAL_BASIS))
                    .build());
            } catch (Exception e) {
                // 配置 JSON 非法 → 兜底默认 + WARN 日志（不抛错，运营侧另行修复）
                log.warn("[compliance] retention config parse failed for {}: {}", rt, e.getMessage());
                out.add(DataRetentionRuleVO.builder()
                    .resourceType(rt)
                    .retentionDays(DEFAULT_RETENTION_DAYS)
                    .deletionPolicy(DEFAULT_DELETION_POLICY)
                    .legalBasis(DEFAULT_LEGAL_BASIS)
                    .build());
            }
        }
        return out;
    }

    /**
     * AC-COMP-02 / AC-COMP-03：创建数据删除请求。
     * <p>步骤：1) 校验 resource 存在 → 2) 计算 30 天 deadline → 3) 写审计 → 4) 返回 VO。
     * <p>本方法仅创建请求记录；真实删除仍走 DeletionRequestController（避免越权直删）。
     *
     * @param dto   入参
     * @param actor 当前操作人
     * @return VO（含 deadlineAt）
     */
    @Transactional(rollbackFor = Exception.class)
    public DataDeletionRequestVO createDeletionRequest(DataDeletionRequestDTO dto, IpdActor actor) {
        if (dto == null) throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        if (actor == null || actor.id() == null) throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);

        // 1) 30 天 deadline（个保法 / GDPR Art.12.3）
        Date now = new Date();
        Date deadline = new Date(now.getTime() + DELETION_DEADLINE_DAYS * 86_400_000L);

        // 2) 落审计（写操作必走 AuditLogService，不可绕过）
        AuditLog auditDraft = AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action("COMPLIANCE_DELETION_REQUEST")
            .entityType(dto.getResourceType())
            .entityId(dto.getResourceId())
            .afterData(String.format("{\"reason\":\"%s\",\"deadlineAt\":\"%s\",\"status\":\"PENDING\"}",
                escapeJson(dto.getReason()), iso(deadline)))
            .reason("deadlineDays=" + DELETION_DEADLINE_DAYS)
            .build();
        AuditLog written = auditLogService.append(auditDraft);

        // 3) 组装 VO（不带物理表，符合「合规请求 = 审计事件」轻量模型）
        return DataDeletionRequestVO.builder()
            .id(written.getId())
            .resourceType(dto.getResourceType())
            .resourceId(dto.getResourceId())
            .requesterId(actor.id())
            .reason(dto.getReason())
            .status("PENDING")
            .deadlineAt(deadline)
            .createdAt(written.getCreateTime() != null ? written.getCreateTime() : now)
            .build();
    }

    /**
     * AC-COMP-04：按 resourceType+resourceId 查询审计链。
     * <p>「同组/本人/全局」范围规则由 service 透明按 actor 角色解析（与 AuditLogController 一致）。
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @param actor        当前操作人
     * @param pageNo       页码
     * @param pageSize     每页条数（最大 200）
     * @return 审计条目页
     */
    public IPage<AuditEntryVO> getAuditTrail(String resourceType, Long resourceId, IpdActor actor,
                                             int pageNo, int pageSize) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (resourceId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);
        }
        int limit = Math.min(Math.max(pageSize, 1), 200);
        Page<AuditLog> page = new Page<>(Math.max(pageNo, 1), limit);

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getEntityType, resourceType)
            .eq(AuditLog::getEntityId, resourceId)
            .orderByDesc(AuditLog::getSeq);

        // 角色范围：SUPER_ADMIN 不限；其他角色限定 actor.id
        if (!"SUPER_ADMIN".equals(actor.role())) {
            wrapper.eq(AuditLog::getOperatorId, actor.id());
        }

        IPage<AuditLog> raw = auditLogMapper.selectPage(page, wrapper);
        IPage<AuditEntryVO> mapped = raw.convert(this::toAuditEntryVO);
        return mapped;
    }

    /**
     * AC-COMP-05：检测用户 R/W 权限分离。
     * <p>语义：以 person_type 为唯一来源，超管/组长 = R+W 同源 → conflict=true。
     * <p>MARKET_PM / RD_PM = 仅 R，conflict=false（合规要求下「读合规不算权」）。
     *
     * @param userId 用户 ID
     * @return 判定 VO
     */
    public PermissionSeparationVO checkPermissionSeparation(Long userId) {
        if (userId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        Person person = personMapper.selectById(userId);
        if (person == null || person.getDelFlag() != null && !"0".equals(person.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        String role = person.getPersonType();
        boolean hasRead = ROLES_WITH_READ.contains(role);
        boolean hasWrite = ROLES_WITH_WRITE.contains(role);
        boolean conflict = hasRead && hasWrite; // 同源持 R + W = 越权风险
        return PermissionSeparationVO.builder()
            .userId(userId)
            .hasReadRole(hasRead)
            .hasWriteRole(hasWrite)
            .conflict(conflict)
            .roleList(role == null ? List.of() : List.of(role))
            .build();
    }

    // ---------- 私有辅助 ----------

    private AuditEntryVO toAuditEntryVO(AuditLog log) {
        if (log == null) return null;
        return AuditEntryVO.builder()
            .seq(log.getSeq())
            .actorId(log.getOperatorId())
            .actorName(log.getOperatorName())
            .action(log.getAction())
            .before(log.getBeforeData())
            .after(log.getAfterData())
            .createTime(log.getCreateTime())
            .entityType(log.getEntityType())
            .entityId(log.getEntityId())
            .build();
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String iso(Date d) {
        // 简化 ISO yyyy-MM-dd HH:mm:ss（与 DTO 期望的格式一致）
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }
}
