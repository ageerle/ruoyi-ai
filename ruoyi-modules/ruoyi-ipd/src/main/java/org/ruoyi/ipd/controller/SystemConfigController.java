package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
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
import java.util.List;
import java.util.Map;

/**
 * 系统参数管理 API /api/v1/system-configs（P0-3.2 解阻塞：参数后台化）。
 * 写操作仅 SUPER_ADMIN，operator 从会话推导。
 * 写后立即 invalidate 缓存（PERF-02：用户裁决配置变更立即生效，不允许 TTL 窗口 ）。
 * P0-3.3 增补：版本链查询与时点解析（不可变快照，AC-GLB-09/10）。
 */
@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final IpdPermission ipdPermission;

    /** 查询所有参数（鉴权：超管） */
    @SaCheckPermission(value = "ipd:system-config:list", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<SystemConfig>> list() {
        ipdPermission.requireAdmin();
        return ApiV1Response.ok(systemConfigService.list());
    }

    /** 单点读取某参数（任何已登录会话可读，业务方依赖的热路径） */
    @SaCheckPermission(value = "ipd:system-config:read", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{key:.+}")
    public ApiV1Response<Map<String, String>> get(@PathVariable @NotBlank String key) {
        return ApiV1Response.ok(Map.of(
            "key", key,
            "value", systemConfigService.getValue(key, "")));
    }

    /** 更新某参数值（仅超管；写后立即失效缓存，PERF-02 强约束；P0-3.3 同事务写版本链，changed_by 绑会话） */
    @SaCheckPermission(value = "ipd:system-config:update", type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{key:.+}")
    public ApiV1Response<Map<String, String>> update(@PathVariable @NotBlank String key,
                                                     @RequestBody @Valid UpdateReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        systemConfigService.update(key, req.value(), actor.id());
        return ApiV1Response.ok(Map.of("key", key, "value",
            systemConfigService.getValue(key, req.value()), "invalidated", "true"));
    }

    /** P0-3.3 版本链查询（仅超管）：某 key 的不可变版本历史，最新在前 */
    @SaCheckPermission(value = "ipd:system-config:list", type = IpdAuthSession.LOGIN_TYPE)
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
    @SaCheckPermission(value = "ipd:system-config:list", type = IpdAuthSession.LOGIN_TYPE)
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

    public record UpdateReq(@NotBlank @Size(max = 2000) String value) {}
}
