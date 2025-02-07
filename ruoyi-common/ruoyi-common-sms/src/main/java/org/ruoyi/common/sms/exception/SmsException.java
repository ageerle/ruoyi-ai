package org.ruoyi.common.sms.exception;

import java.io.Serial;

/**
 * Sms异常类
 *
 * @author Lion Li
 */
public class SmsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SmsException(String msg) {
        super(msg);
    }

}
