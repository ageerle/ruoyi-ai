package org.ruoyi.ipd.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * API-01 补漏：基线 GlobalExceptionHandler 在 IPD 路径返回裸 R（HTTP 200 + code=500），
 * 本类以 HIGHEST+1 优先级接管 IPD controller 包，统一转为 ApiV1Response。
 * IpdPermissionExceptionHandler 以 HIGHEST 优先级先接权限拒绝；此类兜底其余异常。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice(basePackages = "org.ruoyi.ipd.controller")
public class IpdServiceExceptionAdvice {

    @ExceptionHandler(IpdBusinessException.class)
    public ResponseEntity<ApiV1Response<Void>> handleIpdBusiness(IpdBusinessException e) {
        ApiV1ErrorCode mapped = e.getErrorCode() != null ? e.getErrorCode() : ApiV1ErrorCode.INTERNAL_ERROR;
        log.warn("[IPD] business exception: code={} msg={}", mapped.getCode(), e.getMessage());
        return ResponseEntity.status(mapped.getHttpStatus())
            .body(ApiV1Response.fail(mapped, e.getMessage()));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiV1Response<Void>> handleServiceException(ServiceException e) {
        Integer code = e.getCode();
        ApiV1ErrorCode mapped = (code != null)
            ? ApiV1ErrorCode.fromCode(code)
            : ApiV1ErrorCode.PARAM_INVALID;
        log.warn("[IPD] service exception: code={} msg={}", mapped.getCode(), e.getMessage());
        return ResponseEntity.status(mapped.getHttpStatus())
            .body(ApiV1Response.fail(mapped, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiV1Response<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
            .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
            .findFirst().orElse("参数校验失败");
        log.warn("[IPD] validation failed: {}", msg);
        return ResponseEntity.status(ApiV1ErrorCode.PARAM_INVALID.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiV1Response<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("[IPD] not found: {}", e.getRequestURL());
        return ResponseEntity.status(ApiV1ErrorCode.NOT_FOUND.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.NOT_FOUND, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiV1Response<Void>> handleUnexpected(Exception e) {
        log.error("[IPD] unexpected exception", e);
        return ResponseEntity.status(ApiV1ErrorCode.INTERNAL_ERROR.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.INTERNAL_ERROR));
    }
}
