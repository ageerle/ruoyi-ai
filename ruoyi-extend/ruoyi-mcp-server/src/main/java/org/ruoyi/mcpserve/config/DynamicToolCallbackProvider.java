package org.ruoyi.mcpserve.config;

import org.ruoyi.mcpserve.tools.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态工具回调提供者
 * 根据配置动态加载启用的MCP工具
 *
 * @author OpenX
 */
@Component
public class DynamicToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolCallbackProvider.class);

    private final McpToolsConfig mcpToolsConfig;
    private final List<McpTool> allTools;
    private volatile ToolCallback[] cachedCallbacks;

    public DynamicToolCallbackProvider(McpToolsConfig mcpToolsConfig, List<McpTool> allTools) {
        this.mcpToolsConfig = mcpToolsConfig;
        this.allTools = allTools;
        log.info("发现 {} 个MCP工具", allTools.size());
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        if (cachedCallbacks == null) {
            synchronized (this) {
                if (cachedCallbacks == null) {
                    cachedCallbacks = buildToolCallbacks();
                }
            }
        }
        return cachedCallbacks;
    }

    /**
     * 构建工具回调数组
     */
    private ToolCallback[] buildToolCallbacks() {
        List<Object> enabledTools = allTools.stream()
                .filter(tool -> {
                    boolean enabled = mcpToolsConfig.isToolEnabled(tool.getToolName());
                    if (enabled) {
                        log.info("启用工具: {}", tool.getToolName());
                    } else {
                        log.info("禁用工具: {}", tool.getToolName());
                    }
                    return enabled;
                })
                .collect(Collectors.toList());

        if (enabledTools.isEmpty()) {
            log.warn("没有启用任何MCP工具");
            return new ToolCallback[0];
        }

        // 使用 MethodToolCallbackProvider 构建工具回调
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(enabledTools.toArray())
                .build();

        return provider.getToolCallbacks();
    }

    /**
     * 刷新工具缓存，用于配置变更后重新加载
     */
    public void refreshTools() {
        synchronized (this) {
            cachedCallbacks = null;
            log.info("工具缓存已清除，将在下次调用时重新加载");
        }
    }

    /**
     * 获取所有已注册的工具名称
     */
    public List<String> getRegisteredToolNames() {
        return allTools.stream()
                .map(McpTool::getToolName)
                .collect(Collectors.toList());
    }

    /**
     * 获取已启用的工具名称
     */
    public List<String> getEnabledToolNames() {
        return allTools.stream()
                .filter(tool -> mcpToolsConfig.isToolEnabled(tool.getToolName()))
                .map(McpTool::getToolName)
                .collect(Collectors.toList());
    }
}
