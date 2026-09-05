package org.ruoyi.ipd.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.json.handler.BigNumberSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-4.1 验收测试：分页/ID/时间序列化契约
 * <p>契约要点：
 * <ul>
 *   <li>列表分页/排序稳定：ApiV1Response 形态分页键全 5 项（current/size/total/pages/records）</li>
 *   <li>大整数 ID 不失真：Long/BigInteger/BigDecimal &gt; Number.MAX_SAFE_INTEGER → 字符串</li>
 *   <li>UTC 时间统一：timestamp 字段为 ISO-8601 字符串（Instant + UTC）</li>
 *   <li>空资源错误明确：records=空 + total=0 + code=0，调用方易判</li>
 *   <li>OpenAPI 与响应例一致：5 顶层字段固定（code/message/data/timestamp/traceId）</li>
 * </ul>
 */
@Tag("dev")
class P041AcceptanceTest {

    private static ObjectMapper newIpLikeMapper() {
        JavaTimeModule jt = new JavaTimeModule();
        jt.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
        jt.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
        jt.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
        jt.addSerializer(BigDecimal.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        return JsonMapper.builder()
            .addModule(jt)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }

    @Test
    @DisplayName("ApiV1Response.timestamp 序列化为 UTC ISO-8601 字符串（不是 epoch millis 数字）")
    void timestampIsUtcIsoString() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        Instant fixed = Instant.parse("2026-09-05T14:30:00Z");
        ApiV1Response<String> r = ApiV1Response.ok("hello");
        var f = ApiV1Response.class.getDeclaredField("timestamp");
        f.setAccessible(true);
        f.set(r, fixed);

        String json = m.writeValueAsString(r);
        assertThat(json).contains("\"timestamp\":\"2026-09-05T14:30:00Z\"");
        assertThat(json).doesNotContain("\"timestamp\":1");
    }

    @Test
    @DisplayName("BigNumberSerializer：超过 JS 安全上限的 Long（POJO 顶层字段）序列化为字符串，不丢精度")
    void longBeyondJsSafeSerializedAsString() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        // POJO 顶层 Long 字段才能让 Jackson 类型感知走到 BigNumberSerializer（Map<String,Object> 内 Long 被 Object 序列化器接管）
        BigLongPojo p = new BigLongPojo();
        p.id = 2096351556126396418L;  // > 2^53 = 9007199254740992
        p.seq = 2096351556126396418L; // 同样 > 2^53 → 字符串

        String json = m.writeValueAsString(p);
        assertThat(json).contains("\"id\":\"2096351556126396418\"");
        assertThat(json).contains("\"seq\":\"2096351556126396418\"");
    }

    @Test
    @DisplayName("BigNumberSerializer：安全范围内 Long 仍以数字形式输出（避免无谓字符串化）")
    void longWithinSafeRangeSerializedAsNumber() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        BigLongPojo p = new BigLongPojo();
        p.id = 100L;  // < 2^53 安全
        p.seq = 1L;

        String json = m.writeValueAsString(p);
        assertThat(json).contains("\"id\":100");
        assertThat(json).contains("\"seq\":1");
    }

    @Test
    @DisplayName("BigDecimal 序列化为字符串（不出现浮点科学计数法）")
    void bigDecimalSerializedAsPlainString() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        BigDecimalPojo p = new BigDecimalPojo();
        p.amount = new BigDecimal("0.30000000000000004");
        p.amount2 = new BigDecimal("1234567890.123456789");

        String json = m.writeValueAsString(p);
        assertThat(json).contains("\"amount\":\"0.30000000000000004\"");
        assertThat(json).contains("\"amount2\":\"1234567890.123456789\"");
    }

    @Test
    @DisplayName("分页 5 字段：current/size/total/pages/records 齐（IPD IPage 形态）")
    void pageFieldsAreStable() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        // 模拟 IPD 端点返回的 page 形态（MyBatis-Plus Page 序列化）
        Map<String, Object> paged = Map.of(
            "records", List.of(
                Map.of("id", "row1", "name", "A"),
                Map.of("id", "row2", "name", "B")
            ),
            "total", 47,
            "size", 20,
            "current", 1,
            "pages", 3
        );
        ApiV1Response<Object> resp = ApiV1Response.ok(paged);

        String json = m.writeValueAsString(resp);
        assertThat(json).contains("\"records\":[");
        assertThat(json).contains("\"total\":47");
        assertThat(json).contains("\"size\":20");
        assertThat(json).contains("\"current\":1");
        assertThat(json).contains("\"pages\":3");
        for (String k : new String[]{"code", "message", "data", "timestamp", "traceId"}) {
            assertThat(json).contains("\"" + k + "\":");
        }
    }

    @Test
    @DisplayName("空资源分页：records=[] + total=0 + pages=0（不报 5xxxx）")
    void emptyResourcePageIsExplicit() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        Map<String, Object> empty = Map.of(
            "records", List.of(),
            "total", 0,
            "size", 20,
            "current", 1,
            "pages", 0
        );
        ApiV1Response<Object> resp = ApiV1Response.ok(empty);

        String json = m.writeValueAsString(resp);
        assertThat(json).contains("\"records\":[]");
        assertThat(json).contains("\"total\":0");
        assertThat(json).contains("\"pages\":0");
        assertThat(json).contains("\"code\":0");
    }

    @Test
    @DisplayName("成功包络固定形状：5 顶层字段齐 + code=0 + message=ok")
    void successEnvelopeShape() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        String json = m.writeValueAsString(ApiV1Response.ok("payload"));
        assertThat(json).contains("\"code\":0");
        assertThat(json).contains("\"message\":\"ok\"");
        assertThat(json).contains("\"data\":\"payload\"");
        assertThat(json).contains("\"timestamp\":");
        assertThat(json).contains("\"traceId\":");
    }

    @Test
    @DisplayName("失败包络：code/PARAM_INVALID=10001 + message 文本；data=null")
    void failureEnvelopeShape() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        ApiV1Response<Object> fail = ApiV1Response.fail(ApiV1ErrorCode.PARAM_INVALID, "pageNo 必须 ≥ 1");
        String json = m.writeValueAsString(fail);
        assertThat(json).contains("\"code\":10001");
        assertThat(json).contains("\"message\":\"pageNo 必须 ≥ 1\"");
        assertThat(json).contains("\"data\":null");
    }

    @Test
    @DisplayName("JSON 解析回环：序列化字符串再 parse 回 ApiV1Response，timestamp 不丢信息")
    void timestampRoundTrip() throws Exception {
        ObjectMapper m = newIpLikeMapper();
        ApiV1Response<String> original = ApiV1Response.ok("x");
        var f = ApiV1Response.class.getDeclaredField("timestamp");
        f.setAccessible(true);
        Instant fixed = Instant.parse("2026-01-15T08:00:00Z");
        f.set(original, fixed);

        String json = m.writeValueAsString(original);
        var rt = m.readValue(json, ApiV1Response.class);
        var tF = ApiV1Response.class.getDeclaredField("timestamp");
        tF.setAccessible(true);
        Instant back = (Instant) tF.get(rt);
        assertThat(back).isEqualTo(fixed);
    }

    /** 测试 POJO：顶层 Long 字段，触发 Jackson 类型感知序列化（BigNumberSerializer / NumberSerializer）。 */
    static class BigLongPojo {
        public Long id;
        public Long seq;
    }

    /** 测试 POJO：顶层 BigDecimal 字段，触发 ToStringSerializer。 */
    static class BigDecimalPojo {
        public BigDecimal amount;
        public BigDecimal amount2;
    }
}
