package org.ruoyi.common.trace.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 通用链路追踪上下文。
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> BUSINESS_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<String> BUSINESS_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Deque<String>> NODE_STACK = new ThreadLocal<>();

    private TraceContext() {
    }

    public static TraceScope begin(String traceId, String businessType, String businessId, Long userId, String tenantId) {
        TraceScope scope = new TraceScope(getTraceId(), getBusinessType(), getBusinessId(), getUserId(), getTenantId());
        TRACE_ID.set(traceId);
        BUSINESS_TYPE.set(businessType);
        BUSINESS_ID.set(businessId);
        USER_ID.set(userId);
        TENANT_ID.set(tenantId);
        NODE_STACK.remove();
        return scope;
    }

    static void restore(String traceId, String businessType, String businessId, Long userId, String tenantId) {
        setOrRemove(TRACE_ID, traceId);
        setOrRemove(BUSINESS_TYPE, businessType);
        setOrRemove(BUSINESS_ID, businessId);
        setOrRemove(USER_ID, userId);
        setOrRemove(TENANT_ID, tenantId);
        NODE_STACK.remove();
    }

    private static <T> void setOrRemove(ThreadLocal<T> holder, T value) {
        if (value == null) {
            holder.remove();
        } else {
            holder.set(value);
        }
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static String getBusinessType() {
        return BUSINESS_TYPE.get();
    }

    public static String getBusinessId() {
        return BUSINESS_ID.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static String currentNodeId() {
        Deque<String> stack = NODE_STACK.get();
        return stack == null ? null : stack.peek();
    }

    public static int depth() {
        Deque<String> stack = NODE_STACK.get();
        return stack == null ? 0 : stack.size();
    }

    public static void pushNode(String nodeId) {
        Deque<String> stack = NODE_STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            NODE_STACK.set(stack);
        }
        stack.push(nodeId);
    }

    public static void popNode() {
        Deque<String> stack = NODE_STACK.get();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            NODE_STACK.remove();
        }
    }

    /**
     * 为未来的跨线程上下文传播保留的拷贝入口，避免共享可变栈。
     */
    public static Deque<String> copyNodeStack() {
        Deque<String> stack = NODE_STACK.get();
        return stack == null ? new ArrayDeque<>() : new ArrayDeque<>(stack);
    }

    public static void clear() {
        TRACE_ID.remove();
        BUSINESS_TYPE.remove();
        BUSINESS_ID.remove();
        USER_ID.remove();
        TENANT_ID.remove();
        NODE_STACK.remove();
    }
}
