package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.AuditEntryVO;
import org.ruoyi.ipd.dto.DataDeletionRequestDTO;
import org.ruoyi.ipd.dto.DataDeletionRequestVO;
import org.ruoyi.ipd.dto.DataRetentionRuleVO;
import org.ruoyi.ipd.dto.PermissionSeparationVO;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ComplianceService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 合规卡 REST 端点（P2-5.1；ZK-IPD §九 合规）。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET  /data-retention-rules}                数据保留规则（AC-COMP-01）</li>
 *   <li>{@code POST /data-deletion-request}               创建删除请求（AC-COMP-02/03）</li>
 *   <li>{@code GET  /audit-trail/{type}/{id}}             资源审计链（AC-COMP-04）</li>
 *   <li>{@code GET  /permission-separation/{userId}}      R/W 分离判定（AC-COMP-05）</li>
 * </ul>
 *
 * <p>SEC-API-01：写操作 actor 仅从会话推导，前端不传 operatorId。
 */
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;
    private final IpdPermission ipdPermission;

    /** AC-COMP-01：数据保留规则。内部全员可查。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COMPLIANCE_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/data-retention-rules")
    public ApiV1Response<List<DataRetentionRuleVO>> retentionRules() {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(complianceService.getRetentionRules());
    }

    /** AC-COMP-02/03：创建数据删除请求（30 天 deadline + 强制审计）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COMPLIANCE_WRITE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/data-deletion-request")
    @Transactional(rollbackFor = Exception.class)
    public ApiV1Response<DataDeletionRequestVO> requestDeletion(@Valid @RequestBody DataDeletionRequestDTO dto) {
        return ApiV1Response.ok(complianceService.createDeletionRequest(dto, ipdPermission.requireInternal()));
    }

    /** AC-COMP-04：按资源类型+ID 查询审计链（角色范围由 service 透明按 actor 解析）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COMPLIANCE_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/audit-trail/{resourceType}/{resourceId}")
    public ApiV1Response<IPage<AuditEntryVO>> auditTrail(
            @PathVariable String resourceType,
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiV1Response.ok(complianceService.getAuditTrail(
            resourceType, resourceId, ipdPermission.requireInternal(), pageNo, pageSize));
    }

    /** AC-COMP-05：用户 R/W 权限分离判定。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_COMPLIANCE_READ, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/permission-separation/{userId}")
    public ApiV1Response<PermissionSeparationVO> permissionSeparation(@PathVariable Long userId) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(complianceService.checkPermissionSeparation(userId));
    }
}
