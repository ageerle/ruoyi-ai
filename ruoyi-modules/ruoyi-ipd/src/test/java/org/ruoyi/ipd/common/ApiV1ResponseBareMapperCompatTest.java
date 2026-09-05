package org.ruoyi.ipd.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 探测裸 ObjectMapper 对 ApiV1Response.timestamp (Instant) 的兼容性。
 * 如果哪天 ApiV1Response 改动破坏了裸 ObjectMapper 用例（如 Api03AcceptanceTest
 * 直接 new ObjectMapper()），这条会红。
 */
@Tag("dev")
class ApiV1ResponseBareMapperCompatTest {

    @Test
    @DisplayName("裸 ObjectMapper 序列化 ApiV1Response.timestamp(Instant) 不抛异常")
    void bareMapperCanSerializeInstant() throws Exception {
        ObjectMapper m = new ObjectMapper();
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 输出 ISO 字符串
        ApiV1Response<String> r = ApiV1Response.ok("x");
        var f = ApiV1Response.class.getDeclaredField("timestamp");
        f.setAccessible(true);
        f.set(r, Instant.parse("2026-09-05T14:00:00Z"));
        String json = m.writeValueAsString(r);
        // 任何 JSON 形状都可，只要不抛异常
        assertThat(json).contains("\"data\":\"x\"");
    }
}
