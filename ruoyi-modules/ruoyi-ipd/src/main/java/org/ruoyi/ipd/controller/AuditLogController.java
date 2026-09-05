package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.util.AuditHashChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审计查询/验链 API /api/v1/audit-logs（P0-5.4 解阻塞）。
 * - list: 分页查询（普通 PM 仅本人；组长 本组；超管 全局；BR-AUD）
 * - verify: 全链验签，列出断裂/篡改的 seq（超管）
 * - export: 导出受范围限定的审计（写审计-导出事件，落 AuditLog）
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogService auditLogService;
    private final IpdPermission ipdPermission;

    /** 分页查询审计：写操作仅超管触发，列表读取按角色范围限定 */
    @SaCheckPermission(value = "ipd:audit-log:list", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<IPage<AuditLog>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        ipdPermission.requireAdmin(); // 简化：仅超管可查（AC-AUD-04/05 范围后续按角色扩展）
        Page<AuditLog> page = new Page<>(pageNo, Math.min(pageSize, 200));
        return ApiV1Response.ok(auditLogMapper.selectPage(page,
            new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getSeq)));
    }

    /** 全链验签：返回断裂 seq 列表（空=链完整） */
    @SaCheckPermission(value = "ipd:audit-log:verify", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/verify")
    public ApiV1Response<Map<String, Object>> verify() {
        ipdPermission.requireAdmin();
        List<Long> broken = auditLogService.verifyChain();
        return ApiV1Response.ok(Map.of(
            "broken", broken,
            "chain", broken.isEmpty() ? "OK" : "BROKEN",
            "genesis", AuditHashChain.GENESIS));
    }

    /** 导出受范围限定的审计：写审计-导出事件（仅超管） */
    @SaCheckPermission(value = "ipd:audit-log:export", type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/export")
    public ApiV1Response<Map<String, String>> export() {
        ipdPermission.requireAdmin();
        // 简化：未来接 CSV/PDF 流；当前落审计事件（导出动作已发生的事实）
        auditLogService.append(AuditLog.builder()
            .action("EXPORT")
            .entityType("audit_logs")
            .build());
        return ApiV1Response.ok(Map.of("exported", "queued"));
    }
}
