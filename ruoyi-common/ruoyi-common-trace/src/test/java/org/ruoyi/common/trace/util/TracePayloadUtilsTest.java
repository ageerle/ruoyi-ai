package org.ruoyi.common.trace.util;

import org.junit.jupiter.api.Test;

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
}
