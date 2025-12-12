package org.ruoyi.mcp.config;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class McpSSEToolInvoker {


    private final Map<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestIdCounter = new AtomicLong(0);

    /**
     * 调用 MCP 工具（SSE 模式）
     */
    public Object invokeTool(String serverName, Object parameters) {
        try {
            // 生成请求ID
            String requestId = "req_" + requestIdCounter.incrementAndGet();

            // 创建 CompletableFuture 等待响应
            CompletableFuture<Object> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);

            // 构造 MCP 调用请求
            Map<String, Object> callRequest = new HashMap<>();
            callRequest.put("requestId", requestId);
            callRequest.put("serverName", serverName);
            callRequest.put("parameters", convertToMap(parameters));
            callRequest.put("timestamp", System.currentTimeMillis());

            System.out.println("通过 SSE 调用 MCP 工具 [" + serverName + "] 参数: " + parameters);

            // 发送请求到 MCP 服务器（通过 HTTP POST）
            sendSseToolCall(serverName, callRequest);

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
     * 发送 SSE 工具调用请求
     */
    private void sendSseToolCall(String serverName, Map<String, Object> callRequest) {
        try {
            // 通过 HTTP POST 发送工具调用请求
            WebClient webClient = WebClient.builder()
                    .baseUrl("http://localhost:3000")
                    .build();

            String toolCallUrl = "/tool/" + serverName;

            webClient.post()
                    .uri(toolCallUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(callRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .subscribe(
                            response -> System.out.println("工具调用请求发送成功: " + response),
                            error -> System.err.println("工具调用请求发送失败: " + error.getMessage())
                    );

        } catch (Exception e) {
            System.err.println("发送 SSE 工具调用请求失败: " + e.getMessage());
        }
    }

    /**
     * 处理 SSE 响应
     */
    public void handleSseResponse(String serverName, Map<String, Object> message) {
        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            CompletableFuture<Object> future = pendingRequests.remove(requestId);
            if (future != null) {
                Object data = message.get("data");
                future.complete(data != null ? data : message);
            }
        }
    }

    /**
     * 处理 SSE 错误
     */
    public void handleSseError(String serverName, Map<String, Object> message) {
        String requestId = (String) message.get("requestId");
        if (requestId != null) {
            CompletableFuture<Object> future = pendingRequests.remove(requestId);
            if (future != null) {
                String errorMessage = (String) message.get("message");
                future.completeExceptionally(new RuntimeException(errorMessage));
            }
        }
    }

    /**
     * 流式调用 MCP 工具（支持实时进度）
     */
    public Flux<Object> invokeToolStream(String serverName, Object parameters) {
        return Flux.create(emitter -> {
            try {
                // 生成请求ID
                String requestId = "req_" + requestIdCounter.incrementAndGet();

                // 构造 MCP 调用请求
                Map<String, Object> callRequest = new HashMap<>();
                callRequest.put("requestId", requestId);
                callRequest.put("serverName", serverName);
                callRequest.put("parameters", convertToMap(parameters));
                callRequest.put("stream", true); // 标记为流式调用
                callRequest.put("timestamp", System.currentTimeMillis());

                // 创建流式处理器
                StreamHandler streamHandler = new StreamHandler(emitter);
                pendingRequests.put(requestId + "_stream", null); // 占位符

                // 发送流式调用请求
                sendSseToolCall(serverName, callRequest);

                // 注册流式处理器
                registerStreamHandler(requestId, streamHandler);

                emitter.onDispose(() -> {
                    // 清理资源
                    pendingRequests.remove(requestId + "_stream");
                });

            } catch (Exception e) {
                emitter.error(e);
            }
        });
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

    private void registerStreamHandler(String requestId, StreamHandler streamHandler) {
        // 实现流式处理器注册逻辑
    }

    /**
     * 流式处理器
     */
    private static class StreamHandler {
        private final FluxSink<Object> emitter;

        public StreamHandler(FluxSink<Object> emitter) {
            this.emitter = emitter;
        }

        public void onNext(Object data) {
            emitter.next(data);
        }

        public void onComplete() {
            emitter.complete();
        }

        public void onError(Throwable error) {
            emitter.error(error);
        }
    }
}
