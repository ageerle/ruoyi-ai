package org.ruoyi.common.trace.core;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.trace.config.TraceProperties;
import org.ruoyi.common.trace.constant.TraceConstants;
import org.ruoyi.common.trace.domain.TraceNode;
import org.ruoyi.common.trace.service.TraceRecordService;
import org.ruoyi.common.trace.util.TracePayloadUtils;

import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * 链路追踪节点模板，封装节点创建、上下文压栈、执行、结束、出栈的标准生命周期。
 * <p>
 * 用于同步方法内的 trace 埋点，消除手写 start/finish/pop 的重复代码。
 * 对于需要异步结束的场景（如流式响应），请使用 {@link DefaultTraceStreamSpan}。
 *
 * @see DefaultTraceStreamSpan
 */
@Slf4j
public final class TraceNodeTemplate {

    private TraceNodeTemplate() {
    }

    /**
     * 在 trace 节点上下文中执行业务逻辑，成功后使用 outputBuilder 生成输出摘要。
     *
     * @param traceRecordService 记录服务
     * @param traceProperties    配置
     * @param nodeName           节点名称
     * @param nodeType           节点类型
     * @param className          类名
     * @param methodName         方法名
     * @param inputPayload       输入摘要
     * @param action             业务逻辑
     * @param successOutput      成功时从结果构建输出摘要
     * @param <T>                业务返回值类型
     * @return 业务执行结果
     */
    public static <T> T withNode(
            TraceRecordService traceRecordService,
            TraceProperties traceProperties,
            String nodeName,
            String nodeType,
            String className,
            String methodName,
            String inputPayload,
            NodeAction<T> action,
            Function<T, String> successOutput) {

        if (!traceProperties.isEnabled() || StringUtils.isBlank(TraceContext.getTraceId())) {
            return unwrap(action);
        }

        String traceId = TraceContext.getTraceId();
        String nodeId = UUID.randomUUID().toString().replace("-", "");
        long startMillis = System.currentTimeMillis();

        TraceNode node = buildNode(traceId, nodeId, nodeName, nodeType,
                className, methodName, inputPayload, startMillis);

        try {
            traceRecordService.startNode(node);
        } catch (Exception e) {
            log.warn("写入 trace 节点失败，traceId={}, nodeId={}", traceId, nodeId, e);
            return unwrap(action);
        }

        TraceContext.pushNode(nodeId);
        try {
            T result = action.execute();
            String output = successOutput != null && result != null ? successOutput.apply(result) : null;
            finishNode(traceRecordService, traceProperties, traceId, nodeId,
                    TraceConstants.STATUS_SUCCESS, null, output, startMillis);
            return result;
        } catch (Throwable ex) {
            finishNode(traceRecordService, traceProperties, traceId, nodeId,
                    TraceConstants.STATUS_ERROR, ex, null, startMillis);
            throw rethrow(ex);
        } finally {
            TraceContext.popNode();
        }
    }

    /**
     * 在 trace 节点上下文中执行业务逻辑（无输出摘要）。
     */
    public static <T> T withNode(
            TraceRecordService traceRecordService,
            TraceProperties traceProperties,
            String nodeName,
            String nodeType,
            String className,
            String methodName,
            String inputPayload,
            NodeAction<T> action) {
        return withNode(traceRecordService, traceProperties, nodeName, nodeType,
                className, methodName, inputPayload, action, null);
    }

    // ======================== 内部工具方法 ========================

    private static TraceNode buildNode(String traceId, String nodeId, String nodeName,
                                       String nodeType, String className, String methodName,
                                       String inputPayload, long startMillis) {
        TraceNode node = new TraceNode();
        node.setTraceId(traceId);
        node.setNodeId(nodeId);
        node.setParentNodeId(TraceContext.currentNodeId());
        node.setDepth(TraceContext.depth());
        node.setNodeName(nodeName);
        node.setNodeType(nodeType);
        node.setClassName(className);
        node.setMethodName(methodName);
        node.setStatus(TraceConstants.STATUS_RUNNING);
        node.setStartTime(new Date(startMillis));
        node.setInputPayload(inputPayload);
        return node;
    }

    private static void finishNode(TraceRecordService service, TraceProperties props,
                                   String traceId, String nodeId, String status,
                                   Throwable error, String outputPayload, long startMillis) {
        try {
            service.finishNode(traceId, nodeId, status,
                    TracePayloadUtils.error(error, props), outputPayload,
                    new Date(), System.currentTimeMillis() - startMillis);
        } catch (Exception e) {
            log.warn("结束 trace 节点失败，traceId={}, nodeId={}", traceId, nodeId, e);
        }
    }

    private static <T> T unwrap(NodeAction<T> action) {
        try {
            return action.execute();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T rethrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * 可抛出 Throwable 的业务动作。
     */
    @FunctionalInterface
    public interface NodeAction<T> {
        T execute() throws Throwable;
    }
}
