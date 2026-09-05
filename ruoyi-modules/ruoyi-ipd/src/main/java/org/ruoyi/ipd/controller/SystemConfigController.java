package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.SystemConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统参数管理 API /api/v1/system-configs（P0-3.2 解阻塞：参数后台化）。
 * 写操作仅 SUPER_ADMIN，operator 从会话推导。
 * 写后立即 invalidate 缓存（PERF-02：用户裁决配置变更立即生效，不允许 TTL 窗口）。
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
    @GetMapping("/{key}")
    public ApiV1Response<Map<String, String>> get(@PathVariable @NotBlank String key) {
        return ApiV1Response.ok(Map.of(
            "key", key,
            "value", systemConfigService.getValue(key, "")));
    }

    /** 更新某参数值（仅超管；写后立即失效缓存，PERF-02 强约束） */
    @SaCheckPermission(value = "ipd:system-config:update", type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/{key}")
    public ApiV1Response<Map<String, String>> update(@PathVariable @NotBlank String key,
                                                     @RequestBody @Valid UpdateReq req) {
        ipdPermission.requireAdmin();
        systemConfigService.update(key, req.value());
        return ApiV1Response.ok(Map.of("key", key, "value", req.value(), "invalidated", "true"));
    }

    public record UpdateReq(@NotBlank @Size(max = 2000) String value) {}
}
