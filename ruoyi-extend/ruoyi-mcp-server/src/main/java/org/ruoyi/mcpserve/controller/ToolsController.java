package org.ruoyi.mcpserve.controller;
import org.ruoyi.mcpserve.config.DynamicToolCallbackProvider;
import org.ruoyi.mcpserve.config.McpToolsConfig;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MCP工具测试Controller
 * 用于查看已加载的工具信息
 *
 * @author OpenX
 */
@RestController
@RequestMapping("/tools")
public class ToolsController {

    private final DynamicToolCallbackProvider toolCallbackProvider;
    private final McpToolsConfig mcpToolsConfig;

    public ToolsController(DynamicToolCallbackProvider toolCallbackProvider, McpToolsConfig mcpToolsConfig) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.mcpToolsConfig = mcpToolsConfig;
    }

    /**
     * 获取所有工具信息
     */
    @GetMapping
    public Map<String, Object> getToolsInfo() {
        Map<String, Object> result = new HashMap<>();
        
        // 所有已注册的工具
        result.put("registered", toolCallbackProvider.getRegisteredToolNames());
        
        // 已加载的工具回调详情
        List<Map<String, String>> callbacks = Stream.of(toolCallbackProvider.getToolCallbacks())
                .map(callback -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("name", callback.getToolDefinition().name());
                    info.put("description", callback.getToolDefinition().description());
                    return info;
                })
                .collect(Collectors.toList());
        result.put("callbacks", callbacks);
        
        return result;
    }

    /**
     * 刷新工具缓存
     */
    @PostMapping("/refresh")
    public Map<String, String> refreshTools() {
        toolCallbackProvider.refreshTools();
        Map<String, String> result = new HashMap<>();
        result.put("message", "工具缓存已刷新");
        return result;
    }

    /**
     * 启用指定工具
     */
    @PostMapping("/enable/{toolName}")
    public Map<String, Object> enableTool(@PathVariable String toolName) {
        mcpToolsConfig.enableTool(toolName);
        toolCallbackProvider.refreshTools();
        
        Map<String, Object> result = new HashMap<>();
        result.put("toolName", toolName);
        result.put("enabled", true);
        result.put("message", "工具已启用");
        return result;
    }

    /**
     * 禁用指定工具
     */
    @PostMapping("/disable/{toolName}")
    public Map<String, Object> disableTool(@PathVariable String toolName) {
        mcpToolsConfig.disableTool(toolName);
        toolCallbackProvider.refreshTools();
        
        Map<String, Object> result = new HashMap<>();
        result.put("toolName", toolName);
        result.put("enabled", false);
        result.put("message", "工具已禁用");
        return result;
    }

    /**
     * 批量设置工具状态
     * 请求体示例: {"basic": true, "terminal": false, "plantuml": true}
     */
    @PostMapping("/batch")
    public Map<String, Object> batchSetTools(@RequestBody Map<String, Boolean> toolStates) {
        mcpToolsConfig.setToolsEnabled(toolStates);
        toolCallbackProvider.refreshTools();
        
        Map<String, Object> result = new HashMap<>();
        result.put("updated", toolStates);
        result.put("enabled", toolCallbackProvider.getEnabledToolNames());
        result.put("message", "工具状态已更新");
        return result;
    }

    /**
     * 获取所有工具的启用状态
     */
    @GetMapping("/status")
    public Map<String, Object> getToolsStatus() {
        Map<String, Object> result = new HashMap<>();
        List<String> registered = toolCallbackProvider.getRegisteredToolNames();
        
        Map<String, Boolean> status = new HashMap<>();
        for (String toolName : registered) {
            status.put(toolName, mcpToolsConfig.isToolEnabled(toolName));
        }
        
        result.put("status", status);
        return result;
    }
}