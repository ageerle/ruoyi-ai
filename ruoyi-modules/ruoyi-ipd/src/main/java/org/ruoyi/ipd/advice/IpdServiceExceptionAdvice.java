package org.ruoyi.ipd.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.common.IpdBusinessException;
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
 * 例外：权限三型（IpdPermissionException / NotPermissionException / NotRoleException）
 * 由 IpdPermissionExceptionHandler 独占，不得在本类重复注册（详见下方注释）。
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

    // 权限三型（IpdPermissionException / NotPermissionException / NotRoleException）不在此处理：
    // 它们由 IpdPermissionExceptionHandler（@Order(HIGHEST_PRECEDENCE)，basePackages 覆盖
    // org.ruoyi.ipd.controller 及其子包）独占。本类是 @Order(HIGHEST + 1)，一旦在此重复注册
    // 即成生产不可达的死代码：缺陷B 时代该 handler 用 assignableTypes 只覆盖 7 个控制器，兜底有必要；
    // R8-P1-B 改 basePackages 后 14 个 controller 已全部覆盖，该前提已消失。
    // 回归探针见 DefectBAdviceAcceptanceTest（已改为同时注册两个 advice，走生产真实链）。
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
