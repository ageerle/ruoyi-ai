package org.ruoyi.ipd.advice;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.service.IpdAuthInputException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @ExceptionHandler(IpdAuthInputException.class)
    public ResponseEntity<ApiV1Response<Void>> handleIpdAuthInput(IpdAuthInputException e) {
        // 认证输入错误（原密码不符/密码强度不足等）属 4xx 参数/凭据问题，不得落入兜底 500。
        log.warn("[IPD] auth input rejected: {}", e.getMessage());
        return ResponseEntity.status(ApiV1ErrorCode.PARAM_INVALID.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.PARAM_INVALID, e.getMessage()));
    }

    @ExceptionHandler(IpdPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> handleIpdPermission(IpdPermissionException e) {
        // 缺陷B：IpdPermissionExceptionHandler.assignableTypes 未覆盖的控制器（AuditLog/SystemConfig/
        // Coefficient/LaunchDate）其身份/角色拒绝会漏到 Exception 兜底成 500/90001；此处按异常自带状态码忠实映射。
        log.warn("[IPD] permission denied: status={} code={}", e.getHttpStatus(), e.getErrorCode());
        return ResponseEntity.status(e.getHttpStatus()).body(ApiV1Response.fail(e.getErrorCode()));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiV1Response<Void>> handleNotPermission(NotPermissionException e) {
        // 缺陷B：@SaCheckPermission 拒绝（如 ipd:audit-log:list）在非白名单控制器上应 403/30001，而非落 500/90001。
        log.warn("[IPD] sa-token not permission: {}", e.getMessage());
        return ResponseEntity.status(ApiV1ErrorCode.FORBIDDEN.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiV1Response<Void>> handleNotRole(NotRoleException e) {
        log.warn("[IPD] sa-token not role: {}", e.getMessage());
        return ResponseEntity.status(ApiV1ErrorCode.FORBIDDEN.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiV1Response<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        // DEF-2：请求体缺失/不可读属客户端错误，应 400/10001 而非落 500。
        log.warn("[IPD] request body not readable: {}", e.getMessage());
        return ResponseEntity.status(ApiV1ErrorCode.PARAM_INVALID.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.PARAM_INVALID, "请求体缺失或格式错误"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiV1Response<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[IPD] missing request parameter: {}", e.getParameterName());
        return ResponseEntity.status(ApiV1ErrorCode.PARAM_INVALID.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.PARAM_INVALID, "缺少必需参数: " + e.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiV1Response<Void>> handleUnexpected(Exception e) {
        log.error("[IPD] unexpected exception", e);
        return ResponseEntity.status(ApiV1ErrorCode.INTERNAL_ERROR.getHttpStatus())
            .body(ApiV1Response.fail(ApiV1ErrorCode.INTERNAL_ERROR));
    }
}
