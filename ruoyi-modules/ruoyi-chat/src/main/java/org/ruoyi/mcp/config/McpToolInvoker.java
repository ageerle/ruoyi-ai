package org.ruoyi.mcp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class McpToolInvoker {

    private final Map<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestIdCounter = new AtomicLong(0);
    @Autowired
    private McpProcessManager mcpProcessManager;


    /**
     * 调用 MCP 工具（Studio 模式）
     */
    public Object invokeTool(String serverName, Object parameters) {
        try {
            // 生成请求ID
            String requestId = "req_" + requestIdCounter.incrementAndGet();

            // 创建 CompletableFuture 等待响应
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);

            // 构造 MCP 调用消息
            Map<String, Object> callMessage = new HashMap<>();
            callMessage.put("type", "tool_call");
            callMessage.put("requestId", requestId);
            callMessage.put("serverName", serverName);
            callMessage.put("parameters", convertToMap(parameters));
            callMessage.put("timestamp", System.currentTimeMillis());

            System.out.println("调用 MCP 工具 [" + serverName + "] 参数: " + parameters);

            // 发送消息到 MCP 服务器
            boolean sent = mcpProcessManager.sendMcpMessage(serverName, callMessage);
            if (!sent) {
                pendingRequests.remove(requestId);
                throw new RuntimeException("无法发送消息到 MCP 服务器: " + serverName);
            }

            // 等待响应（超时 30 秒）
            Object result = future.get(30, TimeUnit.SECONDS);

            System.out.println("MCP 工具 [" + serverName + "] 调用成功，响应: " + result);

            return result;

        } catch (Exception e) {
            System.err.println("调用 MCP 服务器 [" + serverName + "] 失败: " + e.getMessage());
            e.printStackTrace();

            return Map.of(
                    "serverName", serverName,
                    "status", "failed",
                    "message", "Tool invocation failed: " + e.getMessage(),
                    "parameters", parameters
            );
        }
    }

    /**
     * 处理 MCP 服务器的响应消息
     */
    public void handleMcpResponse(String serverName, Map<String, Object> message) {
        String type = (String) message.get("type");
        if ("tool_response".equals(type)) {
            String requestId = (String) message.get("requestId");
            if (requestId != null) {
                CompletableFuture<Object> future = pendingRequests.remove(requestId);
                if (future != null) {
                    Object data = message.get("data");
                    future.complete(data != null ? data : message);
                }
            }
        } else if ("tool_error".equals(type)) {
            String requestId = (String) message.get("requestId");
            if (requestId != null) {
                CompletableFuture<Object> future = pendingRequests.remove(requestId);
                if (future != null) {
                    String errorMessage = (String) message.get("message");
                    future.completeExceptionally(new RuntimeException(errorMessage));
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object parameters) {
        if (parameters instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            Map<?, ?> paramMap = (Map<?, ?>) parameters;
            for (Map.Entry<?, ?> entry : paramMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
            return result;
        }
        return new HashMap<>();
    }
}


