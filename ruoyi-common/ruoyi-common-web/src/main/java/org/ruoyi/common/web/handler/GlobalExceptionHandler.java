package org.ruoyi.common.web.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.fasterxml.jackson.core.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.exception.SseException;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;

/**
 * 全局异常处理器
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return R.fail(HttpStatus.HTTP_BAD_METHOD, e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error(e.getMessage());
        Integer code = e.getCode();
        return ObjectUtil.isNotNull(code) ? R.fail(code, e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * 认证失败
     */
    @ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SseException.class)
    public String handleNotLoginException(SseException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.debug("请求地址'{}',认证失败'{}',无法访问系统资源", requestURI, e.getMessage());
        return JsonUtils.toJsonString(R.fail(HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源"));
    }

    /**
     * servlet异常
     */
    @ExceptionHandler(ServletException.class)
    public R<Void> handleServletException(ServletException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return R.fail(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BaseException.class)
    public R<Void> handleBaseException(BaseException e, HttpServletRequest request) {
        log.error(e.getMessage());
        return R.fail(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public R<Void> handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI);
        return R.fail(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI);
        return R.fail(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), e.getValue()));
    }

    /**
     * 找不到路由
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}'不存在.", requestURI);
        return R.fail(HttpStatus.HTTP_NOT_FOUND, e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     * 注意：对于文件下载/导出等场景，IOException 可能是正常流程的一部分，
     * 需要排除 export/download 等路径，避免干扰文件导出
     */
    @ExceptionHandler(IOException.class)
    public R<Void> handleIoException(IOException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("sse")) {
            // sse 经常性连接中断 例如关闭浏览器 直接屏蔽
            return null;
        }
        // 排除文件下载/导出相关的 IOException，让异常正常传播以便上层处理
        if (requestURI.contains("/export") || requestURI.contains("/download")) {
            // 重新抛出，让调用方处理
            throw new RuntimeException("文件导出/下载IO异常: " + e.getMessage(), e);
        }
        log.error("请求地址'{}',连接中断", requestURI, e);
        return R.fail(e.getMessage());
    }

    /**
     * sse 连接超时异常 不需要处理
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleRuntimeException(AsyncRequestTimeoutException e) {
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        // 对于文件导出相关异常，不进行封装处理，让原始异常信息传播
        Throwable cause = e.getCause();
        if (requestURI.contains("/export") || requestURI.contains("/download")) {
            log.error("请求地址'{}',文件导出/下载异常.", requestURI, e);
            // 对于文件导出，直接返回异常信息，不进行额外封装
            return R.fail(cause != null ? cause.getMessage() : e.getMessage());
        }
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return R.fail(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return R.fail(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        log.error(e.getMessage());
        String message = StreamUtils.join(e.getAllErrors(), DefaultMessageSourceResolvable::getDefaultMessage, ", ");
        return R.fail(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> constraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage());
        String message = StreamUtils.join(e.getConstraintViolations(), ConstraintViolation::getMessage, ", ");
        return R.fail(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage());
        String message = StreamUtils.join(e.getBindingResult().getAllErrors(), DefaultMessageSourceResolvable::getDefaultMessage, ", ");
        return R.fail(message);
    }

    /**
     * JSON 解析异常（Jackson 在处理 JSON 格式出错时抛出）
     * 可能是请求体格式非法，也可能是服务端反序列化失败
     */
    @ExceptionHandler(JsonParseException.class)
    public R<Void> handleJsonParseException(JsonParseException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}' 发生 JSON 解析异常: {}", requestURI, e.getMessage());
        return R.fail(HttpStatus.HTTP_BAD_REQUEST, "请求数据格式错误（JSON 解析失败）：" + e.getMessage());
    }

    /**
     * 请求体读取异常（通常是请求参数格式非法、字段类型不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("请求地址'{}', 参数解析失败: {}", request.getRequestURI(), e.getMessage());
        return R.fail(HttpStatus.HTTP_BAD_REQUEST, "请求参数格式错误：" + e.getMostSpecificCause().getMessage());
    }

}
