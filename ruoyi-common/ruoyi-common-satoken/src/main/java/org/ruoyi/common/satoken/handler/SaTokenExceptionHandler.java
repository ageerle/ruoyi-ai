package org.ruoyi.common.satoken.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Pattern;

/**
 * SaToken异常处理器
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    private static final Pattern CREDENTIAL_PATH_SEGMENT = Pattern.compile(
        "(?i)(/(?:token(?:[-_]?id)?|access[-_]?token|refresh[-_]?token|api[-_]?key|"
            + "monitor/online(?:/myself)?|myself)/)([^/?#;\\s]+)");

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.error("authorization_failed category=PERMISSION method={} path={} exceptionType={}",
            request.getMethod(), safePath(request), e.getClass().getName());
        return R.fail(HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权");
    }

    /**
     * 角色权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.error("authorization_failed category=ROLE method={} path={} exceptionType={}",
            request.getMethod(), safePath(request), e.getClass().getName());
        return R.fail(HttpStatus.HTTP_FORBIDDEN, "没有访问权限，请联系管理员授权");
    }

    /**
     * 认证失败
     */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.error("authorization_failed category=NOT_LOGIN method={} path={} exceptionType={}",
            request.getMethod(), safePath(request), e.getClass().getName());
        return R.fail(HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源");
    }

    private static String safePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return null;
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        return CREDENTIAL_PATH_SEGMENT.matcher(path).replaceAll("$1[REDACTED]");
    }

}
