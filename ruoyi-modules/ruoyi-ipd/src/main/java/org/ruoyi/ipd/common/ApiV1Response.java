package org.ruoyi.ipd.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * IPD 统一响应（v3 TS-09）。与基线 {@code R<T>}（code=200）不混用：本类 code=0 为成功。
 * 形状：{ code, message, data, timestamp, traceId }
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiV1Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;

    private int code;
    private String message;
    private T data;
    /**
     * 统一 UTC ISO-8601 瞬时（秒级精度，时区无关）。
     * P0-4.1：大整数 ID = 全局 BigNumberSerializer 保真；时间 = ISO-8601 字符串，避免 epoch millis 数字。
     * <p>用自定义 {@link InstantIso8601Serializer} 而非全局 {@code @JsonFormat} + JavaTimeModule，
     * 是因为裸 {@code new ObjectMapper()}（如 advice/Api03 测试路径）不会自动注册
     * {@code com.fasterxml.jackson.datatype:jackson-datatype-jsr310}，会抛
     * {@code InvalidDefinitionException: Java 8 date/time type java.time.Instant not supported by default}。
     * 显式绑定 serializer 后，三种 ObjectMapper 形态（裸 / JavaTimeModule / advice MockMvc）输出完全一致。
     */
    @JsonSerialize(using = InstantIso8601Serializer.class)
    private Instant timestamp;
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

    /**
     * 失败响应，允许覆盖默认文案（校验细节等）。
     *
     * @param errorCode 错误码
     * @param message   对外文案
     * @param <T>       数据类型
     * @return 失败包络
     */
    public static <T> ApiV1Response<T> fail(ApiV1ErrorCode errorCode, String message) {
        return build(errorCode.getCode(), message != null ? message : errorCode.getMessage(), null);
    }

    public static <T> ApiV1Response<T> fail(int code, String message) {
        return build(code, message, null);
    }

    /** API-01：advice 显式注入 traceId；MDC 不可达时由调用方兜底。 */
    public static <T> ApiV1Response<T> fail(int code, String message, String traceId) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.code = code;
        r.message = message;
        r.timestamp = Instant.now();
        r.traceId = traceId != null ? traceId
            : (MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("X-Trace-Id"));
        return r;
    }

    private static <T> ApiV1Response<T> build(int code, String message, T data) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.timestamp = Instant.now();
        r.traceId = MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("X-Trace-Id");
        return r;
    }
}
