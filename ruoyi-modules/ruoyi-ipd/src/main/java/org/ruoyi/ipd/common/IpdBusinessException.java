package org.ruoyi.ipd.common;

import lombok.Getter;

/**
 * IPD 业务异常（统一异常治理方向）。
 *
 * <p>三种构造语义：
 * <ul>
 *   <li>{@link #IpdBusinessException(String)}：单参文案，默认错误码 PARAM_INVALID（取代 ruoyi ServiceException 单参用法）</li>
 *   <li>{@link #IpdBusinessException(ApiV1ErrorCode)}：单参错误码，文案 = code.message（历史约定）</li>
 *   <li>{@link #IpdBusinessException(ApiV1ErrorCode, String)}：双参（错误码 + 文案），显式登记业务码</li>
 * </ul>
 */
@Getter
public class IpdBusinessException extends RuntimeException {

    private final ApiV1ErrorCode errorCode;

    public IpdBusinessException(String message) {
        super(message);
        this.errorCode = ApiV1ErrorCode.PARAM_INVALID;
    }

    public IpdBusinessException(ApiV1ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }

    public IpdBusinessException(ApiV1ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }
}
