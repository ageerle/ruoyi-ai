package org.ruoyi.ipd.security;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把 IPD 权限拒绝统一转为 ApiV1Response 包络 + 对应 HTTP 状态码。
 * 优先级 HIGHEST：比基线 SaTokenExceptionHandler / GlobalExceptionHandler 优先。
 * SEC-API-02：同时承接 @SaCheckPermission 抛出的 NotPermissionException。
 *
 * Round 8 / R8-P1-B：assignableTypes 改为 basePackages 全局覆盖，
 * 避免新增 controller（如 LaunchDateChangeController）漏登导致权限拒绝走基线 advice 返回 R&lt;&gt; 而非 IPD ApiV1Response。
 * SEC-HIGH-3（CWE-693）：basePackages 方案以 CI 防漂移测试固化（PermissionAdviceCoverageTest）——
 * 静态断言本注解含 basePackages 且模块内所有 @RestController 均落在 org.ruoyi.ipd.controller 包，杜绝再次漏列。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.ruoyi.ipd.controller")
public class IpdPermissionExceptionHandler {

    /**
     * IpdPermission.require* 业务拒绝。
     *
     * @param exception 权限异常
     * @return ApiV1 包络
     */
    @ExceptionHandler(IpdPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> denied(IpdPermissionException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(ApiV1Response.fail(exception.getErrorCode()));
    }

    /**
     * Sa-Token 注解权限不足 → 403 FORBIDDEN。
     *
     * @param exception NotPermissionException
     * @return ApiV1 包络
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> notPermission(NotPermissionException exception) {
        return ResponseEntity.status(403).body(ApiV1Response.fail(ApiV1ErrorCode.FORBIDDEN));
    }

    /**
     * Sa-Token 注解角色不足 → 403 FORBIDDEN。
     *
     * @param exception NotRoleException
     * @return ApiV1 包络
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiV1Response<Void>> notRole(NotRoleException exception) {
        return ResponseEntity.status(403).body(ApiV1Response.fail(ApiV1ErrorCode.FORBIDDEN));
    }
}
