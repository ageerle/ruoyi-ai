package org.ruoyi.ipd.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
     * <p>字段类型为 {@code String}（已是 JSON 序列化后的形状），工厂方法写当前 UTC ISO 字符串。
     * 选 String 而非 {@code Instant} + 自定义 Serializer，是因为基线 {@code JacksonConfig} 全局注册了
     * {@code JavaTimeModule}，会绑定 {@code Instant} 的默认 {@code InstantSerializer}，
     * 字段级 {@code @JsonSerialize(using=...)} 注解无法覆盖 module 路径，导致真库仍输出 epoch millis。
     * 改用 String 后所有序列化路径（裸 / Module / MockMvc / Spring MVC）输出一致。
     */
    private String timestamp;
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
        r.timestamp = nowIsoUtc();
        r.traceId = traceId != null ? traceId
            : (MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("X-Trace-Id"));
        return r;
    }

    private static <T> ApiV1Response<T> build(int code, String message, T data) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.timestamp = nowIsoUtc();
        r.traceId = MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("X-Trace-Id");
        return r;
    }

    /** P0-4.1：当前 UTC ISO-8601 字符串（秒级精度，秒尾加 'Z' 显式 UTC）。 */
    private static String nowIsoUtc() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
    }
}
