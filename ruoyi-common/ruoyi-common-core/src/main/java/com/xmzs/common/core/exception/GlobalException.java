package com.xmzs.common.core.exception;

import java.io.Serial;

/**
 * 全局异常
 *
 * @author ruoyi
 */
public class GlobalException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public GlobalException() {
    }

    public GlobalException(String message) {
        this.message = message;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public GlobalException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public GlobalException setMessage(String message) {
        this.message = message;
        return this;
    }
}
