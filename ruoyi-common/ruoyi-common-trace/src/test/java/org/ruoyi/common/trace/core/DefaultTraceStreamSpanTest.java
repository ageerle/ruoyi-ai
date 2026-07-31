package org.ruoyi.common.trace.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.service.TraceRecordService;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultTraceStreamSpanTest {

    @Mock
    private TraceRecordService traceRecordService;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void finishShouldBeIdempotent() {
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, new TraceProperties(), "trace-1", "node-1", System.currentTimeMillis());

        span.finishSuccess();
        span.finishError(new IllegalStateException("ignored"));

        verify(traceRecordService, times(1)).finishNode(eq("trace-1"), eq("node-1"), eq(TraceConstants.STATUS_SUCCESS),
            isNull(), isNull(), any(Date.class), anyLong());
    }

    @Test
    void finishErrorShouldRecordTruncatedError() {
        TraceProperties properties = new TraceProperties();
        properties.getPayload().setMaxErrorLength(8);
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, properties, "trace-1", "node-1", System.currentTimeMillis());

        span.finishError(new IllegalStateException("abcdef"));

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(traceRecordService).finishNode(eq("trace-1"), eq("node-1"), eq(TraceConstants.STATUS_ERROR),
            errorCaptor.capture(), isNull(), any(Date.class), anyLong());
        assertEquals("IllegalS", errorCaptor.getValue());
    }

    @Test
    void detachShouldPopOnlyOnce() {
        TraceContext.pushNode("root");
        TraceContext.pushNode("stream");
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, new TraceProperties(), "trace-1", "stream", System.currentTimeMillis());

        span.detach();
        span.detach();

        assertEquals("root", TraceContext.currentNodeId());
    }

    @Test
    void writeFailureShouldNotEscape() {
        doThrow(new IllegalStateException("db down")).when(traceRecordService)
            .finishNode(eq("trace-1"), eq("node-1"), eq(TraceConstants.STATUS_SUCCESS), isNull(), isNull(), any(Date.class), anyLong());
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, new TraceProperties(), "trace-1", "node-1", System.currentTimeMillis());

        span.finishSuccess();

        assertNull(TraceContext.currentNodeId());
    }
}
