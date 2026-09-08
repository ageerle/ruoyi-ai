package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.SystemConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 系统参数管理 API /api/v1/system-configs（P0-3.2 解阻塞：参数后台化）。
 * 写操作仅 SUPER_ADMIN，operator 从会话推导。
 * 写后立即 invalidate 缓存（PERF-02：用户裁决配置变更立即生效，不允许 TTL 窗口 ）。
 * P0-3.3 增补：版本链查询与时点解析（不可变快照，AC-GLB-09/10）。
 */
@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final IpdPermission ipdPermission;
    /** BUG-P0-3.2-AUDIT-MISSING：参数变更审计写入 audit_logs */
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /** 查询所有参数（鉴权：超管） */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SYSTEM_CONFIG_LIST, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<SystemConfig>> list() {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(systemConfigService.list());
    }

    /** 单点读取某参数（任何已登录会话可读，业务方依赖的热路径） */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SYSTEM_CONFIG_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{key:.+}")
    public ApiV1Response<Map<String, String>> get(@PathVariable @NotBlank String key) {
        return ApiV1Response.ok(Map.of(
            "key", key,
            "value", systemConfigService.getValue(key, "")));
    }

    /** 更新某参数值（仅超管；写后立即失效缓存，PERF-02 强约束；P0-3.3 同事务写版本链，changed_by 绑会话；BUG-P0-3.2-AUDIT-MISSING：调 auditLogService.append 写 audit_logs） */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SYSTEM_CONFIG_UPDATE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{key:.+}")
    public ApiV1Response<Map<String, String>> update(@PathVariable @NotBlank String key,
                                                     @RequestBody @Valid UpdateReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        // BUG-P0-3.2-AUDIT-MISSING：取变更前的值，供 audit before_data + 同值短路
        String oldValue = systemConfigService.getValue(key, null);
        // BUG-P0-3.2-AUDIT-MISSING：值未变不调用 service.update（避免版本链污染），也不写 audit
        if (Objects.equals(oldValue, req.value())) {
            return ApiV1Response.ok(Map.of("key", key, "value",
                systemConfigService.getValue(key, req.value()), "invalidated", "true"));
        }
        systemConfigService.update(key, req.value(), actor.id());
        // BUG-P0-3.2-AUDIT-MISSING：变更审计（独立事务 REQUIRES_NEW；AuditEventData.requireJson 保证 before/after 合法 JSON）
        appendConfigUpdateAudit(actor, key, oldValue, req.value(), req.reason());
        return ApiV1Response.ok(Map.of("key", key, "value",
            systemConfigService.getValue(key, req.value()), "invalidated", "true"));
    }

    /**
     * BUG-P0-3.2-AUDIT-MISSING：参数变更写 audit_logs（独立事务。
     * 业务失败时 audit 已独立提交 — 这是 AuditLogService.append REQUIRES_NEW 的预期行为，
     * 审计框架保 AC-AUD-01 不可篡改 + hash 链连续，胜过「业务回滚跟审计回滚」的次优选。
     * 审计写失败仅记 warn 日志，不阻断业务响应——业务主链（参数变更 + 版本链）不挂审计。
     */
    private void appendConfigUpdateAudit(IpdActor actor, String key, String oldValue, String newValue, String reason) {
        try {
            Map<String, Object> beforeMap = new LinkedHashMap<>();
            beforeMap.put("key", key);
            beforeMap.put("value", oldValue != null ? oldValue : "");
            Map<String, Object> afterMap = new LinkedHashMap<>();
            afterMap.put("key", key);
            afterMap.put("value", newValue);
            AuditLog draft = AuditLog.builder()
                .operatorId(actor.id())
                .operatorName(actor.name())
                .operatorRole(actor.role())
                .action("SYSTEM_CONFIG_UPDATE")
                .entityType("SYSTEM_CONFIG")
                .beforeData(objectMapper.writeValueAsString(beforeMap))
                .afterData(objectMapper.writeValueAsString(afterMap))
                .reason(reason)
                .createTime(new Date())
                .build();
            auditLogService.append(draft);
        } catch (JsonProcessingException e) {
            log.warn("audit append JSON 序列化失败 key={} : {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("audit append 失败 key={} : {}", key, e.getMessage());
        }
    }

    /** P0-3.3 版本链查询（仅超管）：某 key 的不可变版本历史，最新在前 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SYSTEM_CONFIG_LIST, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{key:.+}/versions")
    public ApiV1Response<List<SystemConfigVersion>> versions(@PathVariable @NotBlank String key,
            @RequestParam(defaultValue = "20") int limit) {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(systemConfigService.listVersions(key, limit));
    }

    /**
     * P0-3.3 时点解析（仅超管）：time=ISO-8601（UTC 如 2026-09-05T12:00:00Z 或带偏移）。
     * 返回命中版本或回退源（VERSION|FACTORY_DEFAULT|NONE），供审计/重算对账。
     */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SYSTEM_CONFIG_LIST, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{key:.+}/as-of")
    public ApiV1Response<Map<String, Object>> asOf(@PathVariable @NotBlank String key,
            @RequestParam @NotBlank String time) {
        ipdPermission.requireAdmin();
        Date asOf = parseIsoTime(time);
        return ApiV1Response.ok(systemConfigService.resolveAsOf(key, asOf));
    }

    private static Date parseIsoTime(String raw) {
        try {
            return Date.from(Instant.parse(raw));
        } catch (DateTimeParseException first) {
            try {
                return Date.from(OffsetDateTime.parse(raw).toInstant());
            } catch (DateTimeParseException second) {
                throw new ServiceException("time 须为 ISO-8601（如 2026-09-05T12:00:00Z 或 +08:00 偏移）",
                    ApiV1ErrorCode.PARAM_INVALID.getCode());
            }
        }
    }

    /** BUG-P0-3.2-AUDIT-MISSING：reason 可选（≤500），写 audit_logs.reason，前端不传则 null */
    public record UpdateReq(@NotBlank @Size(max = 2000) String value, @Size(max = 500) String reason) {}
}
