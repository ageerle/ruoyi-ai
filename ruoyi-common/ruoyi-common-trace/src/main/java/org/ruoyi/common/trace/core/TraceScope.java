package org.ruoyi.common.trace.core;

/**
 * Trace 上下文作用域。
 */
public final class TraceScope implements AutoCloseable {

    private final String previousTraceId;
    private final String previousBusinessType;
    private final String previousBusinessId;
    private final Long previousUserId;
    private final String previousTenantId;

    TraceScope(String previousTraceId,
               String previousBusinessType,
               String previousBusinessId,
               Long previousUserId,
               String previousTenantId) {
        this.previousTraceId = previousTraceId;
        this.previousBusinessType = previousBusinessType;
        this.previousBusinessId = previousBusinessId;
        this.previousUserId = previousUserId;
        this.previousTenantId = previousTenantId;
    }

    @Override
    public void close() {
        TraceContext.restore(previousTraceId, previousBusinessType, previousBusinessId, previousUserId, previousTenantId);
    }
}
