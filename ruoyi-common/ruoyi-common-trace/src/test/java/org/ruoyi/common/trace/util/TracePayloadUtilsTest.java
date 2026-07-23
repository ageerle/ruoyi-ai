package org.ruoyi.common.trace.util;

import org.junit.jupiter.api.Test;
import org.ruoyi.common.trace.config.TraceProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePayloadUtilsTest {

    @Test
    void truncateShouldRespectMaxLength() {
        assertEquals("abc", TracePayloadUtils.truncate("abcdef", 3));
    }

    @Test
    void truncateShouldHandleNull() {
        assertNull(TracePayloadUtils.truncate(null, 3));
    }

    @Test
    void toJsonShouldSerializeMap() {
        String json = TracePayloadUtils.toJson(Map.of("count", 2));
        assertTrue(json.contains("\"count\":2"));
    }

    @Test
    void detailEnabledShouldTruncateInputAndOutput() {
        TraceProperties properties = new TraceProperties();
        properties.getPayload().setRecordDetail(true);
        properties.getPayload().setMaxInputLength(4);
        properties.getPayload().setMaxOutputLength(5);

        assertEquals("1234", TracePayloadUtils.input("123456", properties));
        assertEquals("12345", TracePayloadUtils.output("1234567", properties));
    }
}
