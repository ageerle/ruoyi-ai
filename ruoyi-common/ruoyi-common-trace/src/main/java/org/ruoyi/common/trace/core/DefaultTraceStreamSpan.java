package org.ruoyi.common.trace.core;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.common.trace.util.TracePayloadUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认流式 trace 节点实现。
 */
@Slf4j
public class DefaultTraceStreamSpan implements TraceStreamSpan {

    private final TraceRecordService traceRecordService;
    private final TraceProperties traceProperties;
    private final String traceId;
    private final String nodeId;
    private final long startMillis;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean detached = new AtomicBoolean(false);

    public DefaultTraceStreamSpan(TraceRecordService traceRecordService,
                                  TraceProperties traceProperties,
                                  String traceId,
                                  String nodeId,
                                  long startMillis) {
        this.traceRecordService = traceRecordService;
        this.traceProperties = traceProperties;
        this.traceId = traceId;
        this.nodeId = nodeId;
        this.startMillis = startMillis;
    }

    @Override
    public void detach() {
        if (detached.compareAndSet(false, true)) {
            TraceContext.popNode();
        }
    }

    @Override
    public void finishSuccess(String outputPayload) {
        finish(TraceConstants.STATUS_SUCCESS, null, outputPayload);
    }

    @Override
    public void finishError(Throwable throwable) {
        finish(TraceConstants.STATUS_ERROR, TracePayloadUtils.error(throwable, traceProperties), null);
    }

    @Override
    public void finishCancelledIfRunning() {
        finish(TraceConstants.STATUS_CANCELLED, null, null);
    }

    private void finish(String status, String errorMessage, String outputPayload) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        try {
            traceRecordService.finishNode(traceId, nodeId, status, errorMessage, outputPayload,
                new Date(), System.currentTimeMillis() - startMillis);
        } catch (Exception e) {
            log.warn("结束 trace stream span 失败，traceId={}, nodeId={}", traceId, nodeId, e);
        }
    }
}
