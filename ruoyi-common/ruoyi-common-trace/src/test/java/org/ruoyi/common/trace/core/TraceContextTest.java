package org.ruoyi.common.trace.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void beginShouldExposeTraceAndBusinessIdentity() {
        try (TraceScope ignored = TraceContext.begin("trace-1", "RAG_CHAT", "session-1", 10L, "000000")) {
            assertEquals("trace-1", TraceContext.getTraceId());
            assertEquals("RAG_CHAT", TraceContext.getBusinessType());
            assertEquals("session-1", TraceContext.getBusinessId());
            assertEquals(10L, TraceContext.getUserId());
            assertEquals("000000", TraceContext.getTenantId());
        }
        assertNull(TraceContext.getTraceId());
    }

    @Test
    void nodeStackShouldTrackParentAndDepth() {
        TraceContext.pushNode("root");
        assertEquals("root", TraceContext.currentNodeId());
        assertEquals(1, TraceContext.depth());

        TraceContext.pushNode("child");
        assertEquals("child", TraceContext.currentNodeId());
        assertEquals(2, TraceContext.depth());

        TraceContext.popNode();
        assertEquals("root", TraceContext.currentNodeId());

        TraceContext.popNode();
        assertNull(TraceContext.currentNodeId());
        assertEquals(0, TraceContext.depth());
    }
}
