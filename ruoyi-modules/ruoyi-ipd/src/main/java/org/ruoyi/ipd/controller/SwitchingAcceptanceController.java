package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.SwitchingAcceptanceReport;
import org.ruoyi.ipd.dto.SwitchingAcceptanceUnlockReq;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.SwitchingAcceptanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P3-7.1 月度账务切换验收 API（BR-INC-12；AC-INC-50/51）
 *
 * <p>端点：
 * <ul>
 *   <li>POST /api/v1/switching-acceptance/{month}/run  — 运行对账</li>
 *   <li>GET  /api/v1/switching-acceptance/{month}       — 获取报告</li>
 *   <li>POST /api/v1/switching-acceptance/{month}/lock  — 月度锁定（仅超管）</li>
 *   <li>POST /api/v1/switching-acceptance/{month}/unlock — 月度解锁（仅超管）</li>
 *   <li>GET  /api/v1/switching-acceptance              — 列出已 run 月份</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/switching-acceptance")
@RequiredArgsConstructor
public class SwitchingAcceptanceController {

    private final SwitchingAcceptanceService switchingAcceptanceService;

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{month}/run")
    public ApiV1Response<SwitchingAcceptanceReport> run(@PathVariable String month) {
        return ApiV1Response.ok(switchingAcceptanceService.run(month));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/{month}")
    public ApiV1Response<SwitchingAcceptanceReport> get(@PathVariable String month) {
        return ApiV1Response.ok(switchingAcceptanceService.get(month));
    }

    // R-NEW A-1：锁定动作改用专属码 lock；保留 _ADMIN 作历史别名（同字面量值不重复登记以防污染目录）。
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_LOCK, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{month}/lock")
    public ApiV1Response<SwitchingAcceptanceReport> lock(@PathVariable String month) {
        return ApiV1Response.ok(switchingAcceptanceService.lock(month));
    }

    // R-NEW A-1：解锁动作改用专属码 unlock。
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_UNLOCK, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{month}/unlock")
    public ApiV1Response<SwitchingAcceptanceReport> unlock(@PathVariable String month,
                                                          @RequestBody @Valid SwitchingAcceptanceUnlockReq req) {
        return ApiV1Response.ok(switchingAcceptanceService.unlock(month, req));
    }

    @SaCheckPermission(value = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<List<SwitchingAcceptanceReport>> list() {
        return ApiV1Response.ok(switchingAcceptanceService.list());
    }
}