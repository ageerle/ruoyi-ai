package org.ruoyi.mcp.config;

import org.ruoyi.mcp.service.McpInfoService;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态MCP工具回调提供者
 * <p>
 * 这个类有大问题 ，没有测试！！！！！！！
 */
@Component
public class DynamicMcpToolCallbackProvider {

    @Autowired
    private McpInfoService mcpInfoService;

    @Autowired
    private McpProcessManager mcpProcessManager;

    @Autowired
    private McpToolInvoker mcpToolInvoker;

    /**
     * 创建工具回调提供者
     */
    public ToolCallbackProvider createToolCallbackProvider() {
        List<FunctionCallback> callbacks = new ArrayList<>();
        List<String> activeServerNames = mcpInfoService.getActiveServerNames();

        for (String serverName : activeServerNames) {
            FunctionCallback callback = createMcpToolCallback(serverName);
            callbacks.add(callback);
        }

        return ToolCallbackProvider.from(callbacks);
    }

    private FunctionCallback createMcpToolCallback(String serverName) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                // 获取工具配置
                McpServerConfig config = mcpInfoService.getToolConfigByName(serverName);
                if (config == null) {
                    // 返回一个默认的ToolDefinition
                    return ToolDefinition.builder()
                            .name(serverName)
                            .description("MCP tool for " + serverName)
                            .build();
                }
                // 根据config创建ToolDefinition
                return ToolDefinition.builder()
                        .name(serverName)
                        .description(config.getDescription()) // 假设McpServerConfig有getDescription方法
                        .build();
            }

            @Override
            public String call(String toolInput) {
                try {
                    // 获取工具配置
                    McpServerConfig config = mcpInfoService.getToolConfigByName(serverName);
                    if (config == null) {
                        return "{\"error\": \"MCP tool not found: " + serverName + "\", \"serverName\": \"" + serverName + "\"}";
                    }

                    // 确保 MCP 服务器正在运行
                    ensureMcpServerRunning(serverName, config);

                    // 调用 MCP 工具
                    Object result = mcpToolInvoker.invokeTool(serverName, toolInput);

                    return "{\"result\": \"" + result.toString() + "\", \"serverName\": \"" + serverName + "\"}";
                } catch (Exception e) {
                    return "{\"error\": \"MCP tool execution failed: " + e.getMessage() + "\", \"serverName\": \"" + serverName + "\"}";
                }
            }
        };
    }

    private void ensureMcpServerRunning(String serverName, McpServerConfig config) {
        if (!mcpProcessManager.isMcpServerRunning(serverName)) {
            boolean started = mcpProcessManager.startMcpServer(
                    serverName,
                    config
            );
            if (!started) {
                throw new RuntimeException("Failed to start MCP server: " + serverName);
            }
        }
    }
}
