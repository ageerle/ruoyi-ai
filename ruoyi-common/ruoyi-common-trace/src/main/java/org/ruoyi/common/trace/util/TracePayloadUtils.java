package org.ruoyi.common.trace.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.ruoyi.common.json.handler.BigNumberSerializer;
import org.ruoyi.common.trace.config.TraceProperties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

/**
 * Trace payload 序列化、截断与解析工具。
 */
public final class TracePayloadUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        SimpleModule jsSafeModule = new SimpleModule("js-safe-numbers");
        jsSafeModule.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
        jsSafeModule.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
        jsSafeModule.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
        jsSafeModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        OBJECT_MAPPER.registerModule(jsSafeModule);
    }

    private TracePayloadUtils() {
    }

    public static String error(Throwable throwable, TraceProperties properties) {
        if (throwable == null) {
            return null;
        }
        int maxLength = properties == null ? 1000 : properties.getPayload().getMaxErrorLength();
        String message = throwable.getClass().getSimpleName() + ": " + (throwable.getMessage() == null ? "" : throwable.getMessage());
        return truncate(message, maxLength);
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"summary\":\"payload serialization failed\"}";
        }
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 将 JSON 字符串解析为 Map，用于前端展示结构化数据。
     * 解析失败时返回空 Map，不影响主流程。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            // 非 JSON 字符串，返回包含原始文本的 Map
            return Collections.singletonMap("_raw", json);
        }
    }
}
