package org.ruoyi.ipd.common;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * IPD 统一响应（v3 TS-09）。与基线 {@code R<T>}（code=200）不混用：本类 code=0 为成功。
 * 形状：{ code, message, data, timestamp, traceId }
 */
@Data
public class ApiV1Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;

    private int code;
    private String message;
    private T data;
    private Date timestamp;
    private String traceId;

    public static <T> ApiV1Response<T> ok(T data) {
        return build(ApiV1ErrorCode.OK.getCode(), ApiV1ErrorCode.OK.getMessage(), data);
    }

    public static <T> ApiV1Response<T> ok() {
        return ok(null);
    }

    public static <T> ApiV1Response<T> fail(ApiV1ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiV1Response<T> fail(int code, String message) {
        return build(code, message, null);
    }

    private static <T> ApiV1Response<T> build(int code, String message, T data) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.timestamp = new Date();
        r.traceId = MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("X-Trace-Id");
        return r;
    }
}