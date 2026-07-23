package org.ruoyi.common.trace.aspect;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ruoyi.common.trace.annotation.TraceNode;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.core.TraceContext;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.common.trace.util.TracePayloadUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * Trace 方法节点采集切面。
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TraceNodeAspect {

    private final TraceRecordService traceRecordService;
    private final TraceProperties traceProperties;

    @Around("@annotation(traceNode)")
    public Object aroundNode(ProceedingJoinPoint joinPoint, TraceNode traceNode) throws Throwable {
        if (!traceProperties.isEnabled() || StrUtil.isBlank(TraceContext.getTraceId())) {
            return joinPoint.proceed();
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String nodeId = IdUtil.getSnowflakeNextIdStr();
        String traceId = TraceContext.getTraceId();
        long startMillis = System.currentTimeMillis();
        Date startTime = new Date();

        org.ruoyi.common.trace.domain.TraceNode node = new org.ruoyi.common.trace.domain.TraceNode();
        node.setTraceId(traceId);
        node.setNodeId(nodeId);
        node.setParentNodeId(TraceContext.currentNodeId());
        node.setDepth(TraceContext.depth());
        node.setNodeName(StrUtil.blankToDefault(traceNode.name(), method.getName()));
        node.setNodeType(StrUtil.blankToDefault(traceNode.type(), TraceConstants.NODE_METHOD));
        node.setClassName(method.getDeclaringClass().getName());
        node.setMethodName(method.getName());
        node.setStatus(TraceConstants.STATUS_RUNNING);
        node.setStartTime(startTime);
        node.setInputPayload(StrUtil.isBlank(traceNode.input()) ? null : traceNode.input());

        try {
            traceRecordService.startNode(node);
        } catch (Exception ex) {
            log.warn("写入 trace 节点失败，traceId={}", traceId, ex);
            return joinPoint.proceed();
        }

        TraceContext.pushNode(nodeId);
        try {
            Object result = joinPoint.proceed();
            safeFinishNode(traceId, nodeId, TraceConstants.STATUS_SUCCESS, null,
                TracePayloadUtils.output(result, traceProperties), startMillis);
            return result;
        } catch (Throwable ex) {
            safeFinishNode(traceId, nodeId, TraceConstants.STATUS_ERROR,
                TracePayloadUtils.error(ex, traceProperties), null, startMillis);
            throw ex;
        } finally {
            TraceContext.popNode();
        }
    }

    private void safeFinishNode(String traceId, String nodeId, String status, String errorMessage, String outputPayload, long startMillis) {
        try {
            traceRecordService.finishNode(traceId, nodeId, status, errorMessage, outputPayload,
                new Date(), System.currentTimeMillis() - startMillis);
        } catch (Exception ex) {
            log.warn("更新 trace 节点失败，traceId={}, nodeId={}", traceId, nodeId, ex);
        }
    }
}
