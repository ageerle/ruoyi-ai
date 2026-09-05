package org.ruoyi.ipd.common;

import lombok.Getter;

/**
 * IPD 业务异常（不可继承 final 的 ServiceException）。
 * Advice 按 {@link #errorCode} 映射 HTTP / ApiV1Response。
 */
@Getter
public class IpdBusinessException extends RuntimeException {

    private final ApiV1ErrorCode errorCode;

    /**
     * 使用登记错误码构造业务异常。
     *
     * @param code 业务错误码
     */
    public IpdBusinessException(ApiV1ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }

    /**
     * 自定义文案，默认 PARAM_INVALID。
     *
     * @param message 对外提示
     */
    public IpdBusinessException(String message) {
        super(message);
        this.errorCode = ApiV1ErrorCode.PARAM_INVALID;
    }
}
