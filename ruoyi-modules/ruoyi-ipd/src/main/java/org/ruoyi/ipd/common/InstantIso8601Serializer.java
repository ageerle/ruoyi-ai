package org.ruoyi.ipd.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 强一致 ISO-8601 UTC 序列化器：把 {@link Instant} 序列化为 {@code yyyy-MM-dd'T'HH:mm:ss'Z'} 字符串。
 * <p>P0-4.1：ApiV1Response.timestamp 字段专用，目标是「裸 {@code new ObjectMapper()} + advice 链
 * + MockMvc dispatch」三种场景都能稳定输出 ISO 字符串，避免
 * {@code com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Java 8 date/time type
 * java.time.Instant not supported by default}。
 * <p>与基线 JacksonConfig 注册的 JavaTimeModule 不冲突；二者都输出同一字符串。
 */
public class InstantIso8601Serializer extends JsonSerializer<Instant> {

    public static final InstantIso8601Serializer INSTANCE = new InstantIso8601Serializer();

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(FMT.format(value));
        }
    }

    @Override
    public Class<Instant> handledType() {
        return Instant.class;
    }
}
