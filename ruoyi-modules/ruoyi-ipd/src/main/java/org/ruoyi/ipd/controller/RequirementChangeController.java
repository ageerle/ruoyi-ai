package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.RequirementChangeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 需求变更单 API（P2-6.1）
 *
 * <p>端点：
 * <ul>
 *   <li>POST   /api/v1/requirement-changes              创建变更单（市场PM/研发PM）</li>
 *   <li>PUT    /api/v1/requirement-changes/{id}/submit  提交双签（DRAFT ⇒ PENDING_SIGN）</li>
 *   <li>PUT    /api/v1/requirement-changes/{id}/sign    双签签署（市场PM/研发PM）</li>
 *   <li>GET    /api/v1/requirement-changes/{id}          详情（含影响快照+签名进度）</li>
 *   <li>GET    /api/v1/requirement-changes              分页查询</li>
 *   <li>GET    /api/v1/requirement-changes/open          项目未闭环变更单（供 P2-6.2 门禁）</li>
 * </ul>
 *
 * <p>BR-GATE-07：双签通过才更新需求和PRD新版本，由 P2-6.2 接手。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RequirementChangeController {

    private final RequirementChangeService requirementChangeService;
    private final IpdPermission ipdPermission;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/requirement-changes")
    public ApiV1Response<RequirementChange> create(@RequestBody RequirementChange change) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.create(change, actor));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/requirement-changes/{id}/submit")
    public ApiV1Response<RequirementChange> submit(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.submit(id, actor));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    @PutMapping("/requirement-changes/{id}/sign")
    public ApiV1Response<RequirementChange> sign(@PathVariable Long id,
                                                  @RequestParam String decision,
                                                  @RequestParam(required = false) String opinion) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.sign(id, decision, opinion, actor));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/requirement-changes/{id}")
    public ApiV1Response<Map<String, Object>> detail(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.detail(id));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/requirement-changes")
    public ApiV1Response<IPage<RequirementChange>> list(@RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) String status) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.listByProject(pageNo, pageSize, projectId, status));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/requirement-changes/open")
    public ApiV1Response<List<RequirementChange>> listOpen(@RequestParam Long projectId) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(requirementChangeService.listOpenByProject(projectId));
    }
}
