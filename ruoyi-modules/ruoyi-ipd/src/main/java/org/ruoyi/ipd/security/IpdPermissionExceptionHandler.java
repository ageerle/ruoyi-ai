package org.ruoyi.ipd.security;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.controller.CertTemplateController;
import org.ruoyi.ipd.controller.DeletionRequestController;
import org.ruoyi.ipd.controller.GateElementController;
import org.ruoyi.ipd.controller.IpdAuthController;
import org.ruoyi.ipd.controller.ProductController;
import org.ruoyi.ipd.controller.ProjectController;
import org.ruoyi.ipd.controller.StageActionController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把 IPD 权限拒绝统一转为 ApiV1Response 包络 + 对应 HTTP 状态码。
 * 优先级 HIGHEST：比基线 SaTokenExceptionHandler / GlobalExceptionHandler 优先。
 * SEC-API-02：同时承接 @SaCheckPermission 抛出的 NotPermissionException。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {IpdAuthController.class, ProductController.class, ProjectController.class,
    StageActionController.class, CertTemplateController.class, GateElementController.class,
    DeletionRequestController.class})
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
