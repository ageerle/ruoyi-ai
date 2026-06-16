package org.ruoyi.common.trace.core;

/**
 * 可跨回调结束的流式 trace 节点。
 */
public interface TraceStreamSpan {

    void detach();

    void finishSuccess();

    void finishError(Throwable throwable);

    void finishCancelledIfRunning();
}
