package org.ruoyi.ipd.security;

import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.controller.CertTemplateController;
import org.ruoyi.ipd.controller.GateElementController;
import org.ruoyi.ipd.controller.ProductController;
import org.ruoyi.ipd.controller.ProjectController;
import org.ruoyi.ipd.controller.StageActionController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 只转换SEC-01拒绝，不覆盖P071认证或基线接口异常协议。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {ProductController.class, ProjectController.class,
    StageActionController.class, CertTemplateController.class, GateElementController.class})
public class IpdPermissionExceptionHandler {
    @ExceptionHandler(IpdPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> denied(IpdPermissionException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(ApiV1Response.fail(exception.getErrorCode()));
    }
}
