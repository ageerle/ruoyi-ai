package org.ruoyi.ipd.security;

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

/** 把 IpdPermissionException 统一转为 ApiV1Response 包络 + 对应 HTTP 状态码。
 * 优先级 HIGHEST：比基线 GlobalExceptionHandler 优先，避免 IPD 路径回退到裸 R 包络。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {IpdAuthController.class, ProductController.class, ProjectController.class,
    StageActionController.class, CertTemplateController.class, GateElementController.class,
    DeletionRequestController.class})
public class IpdPermissionExceptionHandler {
    @ExceptionHandler(IpdPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> denied(IpdPermissionException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(ApiV1Response.fail(exception.getErrorCode()));
    }
}
