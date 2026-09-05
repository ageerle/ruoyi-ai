package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.util.AuditHashChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审计查询/验链/范围 API /api/v1/audit-logs（P0-5.4）。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET  /}            列表（仅超管，P0-5.4 简化的"旧接口"）
 *   <li>{@code GET  /verify}     全链验签（仅超管）
 *   <li>{@code GET  /export}     导出（仅超管）
 *   <li>{@code GET  /scope}      角色范围分页（AC-AUD-04/05；本人/本组/全局）
 *   <li>{@code GET  /export/scope} 角色范围导出（写一条 EXPORT 审计）
 * </ul>
 *
 * <p>scope 规则：
 * <ul>
 *   <li>SUPER_ADMIN：operatorIds = null（=全库）
 *   <li>GROUP_LEADER：operatorIds = (本组所有 personId)
 *   <li>MARKET_PM / RD_PM：operatorIds = [actor.id]（仅本人）
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogService auditLogService;
    private final IpdPermission ipdPermission;
    private final PersonMapper personMapper;

    /** 分页查询审计：写操作仅超管触发，列表读取按角色范围限定（保留原接口） */
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

    /**
     * 角色范围分页查询（P0-5.4 / AC-AUD-04、AC-AUD-05）。
     * <p>任何内部已登录角色可调用；过滤由 service 透明按 actor 角色解析。
     * <p>未登录游客 → {@code IpdWebSecurityConfig} 拦截 401（AC-AUD-06）。
     */
    @GetMapping("/scope")
    public ApiV1Response<Map<String, Object>> listByScope(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        IpdActor actor = ipdPermission.requireInternal();
        List<Long> operatorIds = resolveOperatorIds(actor);
        IPage<AuditLog> page = auditLogService.listByOperatorIds(operatorIds, pageNo, pageSize);
        return ApiV1Response.ok(Map.of(
            "scope", operatorIds == null ? "GLOBAL" : (operatorIds.size() == 1 && operatorIds.get(0).equals(actor.id())) ? "OWN" : "GROUP",
            "operatorIds", operatorIds == null ? List.of() : operatorIds,
            "page", page));
    }

    /**
     * 角色范围导出：受 actor 范围限定 + 落一条 EXPORT 审计（P0-5.4 / BR-AUD-04）。
     */
    @GetMapping("/export/scope")
    public ApiV1Response<Map<String, Object>> exportByScope() {
        IpdActor actor = ipdPermission.requireInternal();
        List<Long> operatorIds = resolveOperatorIds(actor);
        long count = auditLogService.countByOperatorIds(operatorIds);
        // 导出动作本身写一条审计（独立事务）
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action("EXPORT")
            .entityType("audit_logs")
            .reason("scope=" + (operatorIds == null ? "GLOBAL" : operatorIds.size() + "_ids"))
            .build());
        return ApiV1Response.ok(Map.of(
            "exported", count,
            "scope", operatorIds == null ? "GLOBAL" : (operatorIds.size() == 1 && operatorIds.get(0).equals(actor.id())) ? "OWN" : "GROUP"));
    }

    /**
     * 按 actor 角色解析 operatorIds：SUPER_ADMIN=null（=全库）、GROUP_LEADER=本组 personIds、其他=[actor.id]。
     */
    private List<Long> resolveOperatorIds(IpdActor actor) {
        if ("SUPER_ADMIN".equals(actor.role())) {
            return null; // 全库
        }
        if ("GROUP_LEADER".equals(actor.role())) {
            Long groupId = actor.groupId();
            if (groupId == null) return List.of(actor.id()); // 无组 = 退化为仅本人
            List<Person> members = personMapper.selectList(
                new LambdaQueryWrapper<Person>().eq(Person::getGroupId, groupId));
            List<Long> ids = new ArrayList<>(members.size());
            for (Person p : members) ids.add(p.getId());
            // 兜底：若本组无人（含自己），至少把自己算上（不会查不到自己）
            if (ids.isEmpty()) ids.add(actor.id());
            return ids;
        }
        // MARKET_PM / RD_PM / 其它 = 仅本人
        return List.of(actor.id());
    }
}
