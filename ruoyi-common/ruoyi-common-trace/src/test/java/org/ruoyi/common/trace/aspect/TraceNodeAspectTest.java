package org.ruoyi.common.trace.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.trace.annotation.TraceNode;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.core.TraceContext;
import org.ruoyi.common.trace.service.TraceRecordService;

import java.lang.reflect.Method;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceNodeAspectTest {

    @Mock
    private TraceRecordService traceRecordService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void shouldProceedWithoutRecordingWhenNoTraceContext() throws Throwable {
        TraceNodeAspect aspect = new TraceNodeAspect(traceRecordService, new TraceProperties());
        Method method = SampleService.class.getDeclaredMethod("sample");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.aroundNode(joinPoint, method.getAnnotation(TraceNode.class));

        assertEquals("ok", result);
        verify(traceRecordService, never()).startNode(any());
    }

    @Test
    void shouldRecordSuccessNode() throws Throwable {
        TraceProperties properties = new TraceProperties();
        properties.getPayload().setRecordDetail(true);
        TraceNodeAspect aspect = new TraceNodeAspect(traceRecordService, properties);
        Method method = SampleService.class.getDeclaredMethod("sample");
        prepareSignature(method);
        when(joinPoint.proceed()).thenReturn("ok");

        try (var ignored = TraceContext.begin("trace-1", "TEST", "biz-1", 1L, "000000")) {
            Object result = aspect.aroundNode(joinPoint, method.getAnnotation(TraceNode.class));

            assertEquals("ok", result);
            assertNull(TraceContext.currentNodeId());
        }

        ArgumentCaptor<org.ruoyi.common.trace.domain.TraceNode> nodeCaptor =
            ArgumentCaptor.forClass(org.ruoyi.common.trace.domain.TraceNode.class);
        verify(traceRecordService).startNode(nodeCaptor.capture());
        org.ruoyi.common.trace.domain.TraceNode node = nodeCaptor.getValue();
        assertEquals("trace-1", node.getTraceId());
        assertEquals("sample-node", node.getNodeName());
        assertEquals(TraceConstants.NODE_METHOD, node.getNodeType());
        assertEquals("safe-input", node.getInputPayload());
        verify(traceRecordService).finishNode(eq("trace-1"), eq(node.getNodeId()), eq(TraceConstants.STATUS_SUCCESS),
            isNull(), eq("ok"), any(Date.class), anyLong());
    }

    @Test
    void shouldRethrowBusinessExceptionAfterErrorRecord() throws Throwable {
        TraceNodeAspect aspect = new TraceNodeAspect(traceRecordService, new TraceProperties());
        Method method = SampleService.class.getDeclaredMethod("sample");
        prepareSignature(method);
        IllegalArgumentException error = new IllegalArgumentException("bad");
        when(joinPoint.proceed()).thenThrow(error);

        try (var ignored = TraceContext.begin("trace-1", "TEST", "biz-1", 1L, "000000")) {
            assertThrows(IllegalArgumentException.class,
                () -> aspect.aroundNode(joinPoint, method.getAnnotation(TraceNode.class)));
        }

        ArgumentCaptor<org.ruoyi.common.trace.domain.TraceNode> nodeCaptor =
            ArgumentCaptor.forClass(org.ruoyi.common.trace.domain.TraceNode.class);
        verify(traceRecordService).startNode(nodeCaptor.capture());
        verify(traceRecordService).finishNode(eq("trace-1"), eq(nodeCaptor.getValue().getNodeId()), eq(TraceConstants.STATUS_ERROR),
            eq("IllegalArgumentException: bad"), isNull(), any(Date.class), anyLong());
    }

    @Test
    void writeFailureShouldNotAffectBusinessReturn() throws Throwable {
        TraceNodeAspect aspect = new TraceNodeAspect(traceRecordService, new TraceProperties());
        Method method = SampleService.class.getDeclaredMethod("sample");
        prepareSignature(method);
        when(joinPoint.proceed()).thenReturn("ok");
        doThrow(new IllegalStateException("db down")).when(traceRecordService).startNode(any());

        try (var ignored = TraceContext.begin("trace-1", "TEST", "biz-1", 1L, "000000")) {
            assertEquals("ok", aspect.aroundNode(joinPoint, method.getAnnotation(TraceNode.class)));
        }
    }

    private void prepareSignature(Method method) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    private static class SampleService {

        @TraceNode(name = "sample-node", input = "safe-input")
        String sample() {
            return "ok";
        }
    }
}
