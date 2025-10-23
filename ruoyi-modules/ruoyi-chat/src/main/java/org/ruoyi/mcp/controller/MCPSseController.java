package org.ruoyi.mcp.controller;

import org.ruoyi.mcp.config.McpSSEToolInvoker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/sse")
public class MCPSseController {

    @Autowired
    private McpSSEToolInvoker mcpToolInvoker;

    /**
     * SSE 流式响应端点
     */
    @GetMapping(value = "/{serverName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMcpResponse(@PathVariable String serverName) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {
            // 发送连接建立消息
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("serverName", serverName, "status", "connected")));

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 调用 MCP 工具（流式）
     */
    @PostMapping("/tool/{serverName}")
    public ResponseEntity<?> callMcpTool(
            @PathVariable String serverName,
            @RequestBody Map<String, Object> request) {

        try {
            boolean isStream = (Boolean) request.getOrDefault("stream", false);
            Object parameters = request.get("parameters");

            if (isStream) {
                // 流式调用
                return ResponseEntity.ok(Map.of("status", "streaming_started"));
            } else {
                // 普通调用
                Object result = mcpToolInvoker.invokeTool(serverName, parameters);
                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
