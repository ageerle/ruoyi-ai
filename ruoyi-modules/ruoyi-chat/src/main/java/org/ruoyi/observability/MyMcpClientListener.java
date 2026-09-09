package org.ruoyi.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.protocol.*;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 客户端监听器
 * <p>
 * 监听 MCP 工具执行事件，并通过 SSE 推送到前端
 * <p>
 * <b>SSE 推送格式：</b>
 * <pre>
 * {
 *   "event": "mcp",
 *   "content": "{\"name\":\"tool|resource|prompt\",\"status\":\"pending|success|error\",\"result\":null}"
 * }
 * </pre>
 * <b>前端区分方式：</b>
 * <ul>
 *   <li>对话内容：event="content"</li>
 *   <li>MCP 事件：event="mcp"</li>
 * </ul>
 *
 * @author evo
 */
@Slf4j
public class MyMcpClientListener implements McpClientListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String sessionId;

    public MyMcpClientListener(String sessionId) {
        this.sessionId = sessionId;
    }

    public MyMcpClientListener() {
        this.sessionId = null;
    }

    // ==================== 工具执行 ====================
    @Override
    public void beforeExecuteTool(McpCallContext context) {
        McpClientRequest message = (McpClientRequest) context.message();
        McpClientParams params = message.getParams();
        if (params instanceof McpCallToolParams) {
            log.info("mcp_operation operationType=TOOL status=PENDING");
            pushMcpEvent("tool", "pending", null);
        }

    }

    @Override
    public void afterExecuteTool(McpCallContext context, ToolExecutionResult result, Map<String, Object> rawResult) {
        McpClientRequest message = (McpClientRequest) context.message();
        McpClientParams params = message.getParams();
        if (params instanceof McpCallToolParams) {
            int resultItemCount = rawResult == null ? 0 : rawResult.size();
            log.info("mcp_operation operationType=TOOL status=SUCCESS resultItemCount={}", resultItemCount);
            pushMcpEvent("tool", "success", null);
        }
    }

    @Override
    public void onExecuteToolError(McpCallContext context, Throwable error) {
        log.error("mcp_operation operationType=TOOL status=FAILED errorType={}",
            error == null ? "unknown" : error.getClass().getName());
        pushMcpEvent("tool", "error", null);
    }

    // ==================== 资源读取 ====================

    @Override
    public void beforeResourceGet(McpCallContext context) {
        log.info("mcp_operation operationType=RESOURCE status=PENDING");
        pushMcpEvent("resource", "pending", null);
    }

    @Override
    public void afterResourceGet(McpCallContext context, McpReadResourceResult result, Map<String, Object> rawResult) {
        int count = result != null && result.contents() != null ? result.contents().size() : 0;
        log.info("mcp_operation operationType=RESOURCE status=SUCCESS itemCount={}", count);
        pushMcpEvent("resource", "success", null);
    }

    @Override
    public void onResourceGetError(McpCallContext context, Throwable error) {
        log.error("mcp_operation operationType=RESOURCE status=FAILED errorType={}",
            error == null ? "unknown" : error.getClass().getName());
        pushMcpEvent("resource", "error", null);
    }

    // ==================== 提示词获取 ====================

    @Override
    public void beforePromptGet(McpCallContext context) {
        log.info("mcp_operation operationType=PROMPT status=PENDING");
        pushMcpEvent("prompt", "pending", null);
    }

    @Override
    public void afterPromptGet(McpCallContext context, McpGetPromptResult result, Map<String, Object> rawResult) {
        int count = result != null && result.messages() != null ? result.messages().size() : 0;
        log.info("mcp_operation operationType=PROMPT status=SUCCESS messageCount={}", count);
        pushMcpEvent("prompt", "success", null);
    }

    @Override
    public void onPromptGetError(McpCallContext context, Throwable error) {
        log.error("mcp_operation operationType=PROMPT status=FAILED errorType={}",
            error == null ? "unknown" : error.getClass().getName());
        pushMcpEvent("prompt", "error", null);
    }

    // ==================== 辅助方法 ====================

    /**
     * 推送 MCP 事件到前端
     */
    private void pushMcpEvent(String name, String status, String result) {
        if (sessionId == null) {
            log.warn("mcp_event_delivery status=SKIPPED reason=SESSION_ID_MISSING");
            return;
        }
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("name", name);
            content.put("status", status);
            content.put("result", result);

            String json = OBJECT_MAPPER.writeValueAsString(content);
            SseMessageUtils.sendEvent(sessionId, SseEventDto.builder()
                .event("mcp")
                .content(json)
                .build());
        } catch (JsonProcessingException e) {
            log.error("mcp_event_delivery status=FAILED errorType={}", e.getClass().getName());
        }
    }

}
