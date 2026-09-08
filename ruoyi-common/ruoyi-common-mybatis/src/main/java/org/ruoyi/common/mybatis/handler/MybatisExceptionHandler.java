package org.ruoyi.common.mybatis.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.http.HttpStatus;
import com.baomidou.dynamic.datasource.exception.CannotFindDataSourceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Pattern;

/**
 * Mybatis异常处理器
 *
 * @author Lion Li
 */
@Slf4j
@RestControllerAdvice
public class MybatisExceptionHandler {

    private static final Pattern CREDENTIAL_PATH_SEGMENT = Pattern.compile(
        "(?i)(/(?:token(?:[-_]?id)?|access[-_]?token|refresh[-_]?token|api[-_]?key|"
            + "monitor/online(?:/myself)?|myself)/)([^/?#;\\s]+)");

    /**
     * 主键或UNIQUE索引，数据重复异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.error("database_request_failed category=DUPLICATE_KEY method={} path={} exceptionType={}",
            request.getMethod(), safePath(request), e.getClass().getName());
        return R.fail(HttpStatus.HTTP_CONFLICT, "数据库中已存在该记录，请联系管理员确认");
    }

    /**
     * Mybatis系统异常 通用处理
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public R<Void> handleCannotFindDataSourceException(MyBatisSystemException e, HttpServletRequest request) {
        Throwable root = getRootCause(e);
        if (root instanceof NotLoginException) {
            log.error("database_request_failed category=AUTHENTICATION method={} path={} exceptionType={}",
                request.getMethod(), safePath(request), root.getClass().getName());
            return R.fail(HttpStatus.HTTP_UNAUTHORIZED, "认证失败，无法访问系统资源");
        }
        if (root instanceof CannotFindDataSourceException) {
            log.error("database_request_failed category=DATA_SOURCE_NOT_FOUND method={} path={} exceptionType={}",
                request.getMethod(), safePath(request), root.getClass().getName());
            return R.fail(HttpStatus.HTTP_INTERNAL_ERROR, "未找到数据源，请联系管理员确认");
        }
        log.error("database_request_failed category=MYBATIS_SYSTEM method={} path={} exceptionType={} rootType={}",
            request.getMethod(), safePath(request), e.getClass().getName(), root.getClass().getName());
        return R.fail(HttpStatus.HTTP_INTERNAL_ERROR, "数据库访问异常，请联系管理员确认");
    }

    /**
     * 获取异常的根因（递归查找）
     *
     * @param e 当前异常
     * @return 根因异常（最底层的 cause）
     * <p>
     * 逻辑说明：
     * 1. 如果 e 没有 cause，说明 e 本身就是根因，直接返回
     * 2. 如果 e 的 cause 和自身相同（防止循环引用），也返回 e
     * 3. 否则递归调用，继续向下寻找最底层的 cause
     */
    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null || cause == e) {
            return e;
        }
        return getRootCause(cause);
    }

    /**
     * 在异常链中查找指定类型的异常
     *
     * @param e     当前异常
     * @param clazz 目标异常类
     * @return 找到的指定类型异常，如果没有找到返回 null
     */
    public static Throwable findCause(Throwable e, Class<? extends Throwable> clazz) {
        Throwable t = e;
        while (t != null && t != t.getCause()) {
            if (clazz.isInstance(t)) {
                return t;
            }
            t = t.getCause();
        }
        return null;
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
