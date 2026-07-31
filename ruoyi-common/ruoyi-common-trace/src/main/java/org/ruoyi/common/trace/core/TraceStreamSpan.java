package org.ruoyi.common.trace.core;

/**
 * 可跨回调结束的流式 trace 节点。
 */
public interface TraceStreamSpan {

    void detach();

    /**
     * 结束成功的流式节点，并可写入输出摘要。
     */
    default void finishSuccess() {
        finishSuccess(null);
    }

    void finishSuccess(String outputPayload);

    void finishError(Throwable throwable);

    void finishCancelledIfRunning();
}
