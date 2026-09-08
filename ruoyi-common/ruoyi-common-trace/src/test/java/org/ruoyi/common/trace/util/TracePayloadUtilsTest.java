package org.ruoyi.common.trace.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.ruoyi.common.trace.config.TraceProperties;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
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
    void errorShouldPersistOnlyTypeWithoutReadingThrowableMessage() {
        AtomicBoolean messageRead = new AtomicBoolean();
        Throwable failure = new IllegalStateException() {
            @Override
            public String getMessage() {
                messageRead.set(true);
                return "provider-credential-path-canary";
            }
        };

        String summary = TracePayloadUtils.error(failure, new TraceProperties());
        TraceProperties missingPayloadPolicy = new TraceProperties();
        missingPayloadPolicy.setPayload(null);

        assertFalse(messageRead.get());
        assertEquals("TRACE_FAILURE errorType=" + failure.getClass().getName(), summary);
        assertEquals(summary, TracePayloadUtils.error(failure, missingPayloadPolicy));
        assertFalse(summary.contains("provider-credential-path-canary"));
    }
}
