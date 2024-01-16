package com.xmzs.common.core.exception;

import java.io.Serial;

/**
 * 工具类异常
 *
 * @author ruoyi
 */
public class UtilException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8247610319171014183L;

    public UtilException(Throwable e) {
        super(e.getMessage(), e);
    }

    public UtilException(String message) {
        super(message);
    }

    public UtilException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
