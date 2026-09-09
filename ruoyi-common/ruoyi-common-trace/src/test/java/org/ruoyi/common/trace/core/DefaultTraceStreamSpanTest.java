package org.ruoyi.common.trace.core;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
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
    void finishErrorShouldRecordOnlyBoundedExceptionType() {
        TraceProperties properties = new TraceProperties();
        properties.getPayload().setMaxErrorLength(200);
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, properties, "trace-1", "node-1", System.currentTimeMillis());

        span.finishError(new IllegalStateException("provider-credential-path-canary"));

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(traceRecordService).finishNode(eq("trace-1"), eq("node-1"), eq(TraceConstants.STATUS_ERROR),
            errorCaptor.capture(), isNull(), any(Date.class), anyLong());
        assertEquals("TRACE_FAILURE errorType=java.lang.IllegalStateException", errorCaptor.getValue());
        assertFalse(errorCaptor.getValue().contains("provider-credential-path-canary"));
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
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultTraceStreamSpan.class);
        ListAppender<ILoggingEvent> appender = attach(logger);
        doThrow(new IllegalStateException("trace-storage-credential-canary")).when(traceRecordService)
            .finishNode(eq("trace-1"), eq("node-1"), eq(TraceConstants.STATUS_SUCCESS), isNull(), isNull(), any(Date.class), anyLong());
        DefaultTraceStreamSpan span = new DefaultTraceStreamSpan(traceRecordService, new TraceProperties(), "trace-1", "node-1", System.currentTimeMillis());

        try {
            span.finishSuccess();

            String logs = formattedLogs(appender);
            assertNull(TraceContext.currentNodeId());
            assertFalse(logs.contains("trace-storage-credential-canary"));
            assertTrue(logs.contains("java.lang.IllegalStateException"));
        } finally {
            detach(logger, appender);
        }
    }

    private static ListAppender<ILoggingEvent> attach(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(Logger logger, ListAppender<ILoggingEvent> appender) {
        logger.detachAppender(appender);
        appender.stop();
    }

    private static String formattedLogs(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage)
            .reduce("", (left, right) -> left + "\n" + right);
    }
}
