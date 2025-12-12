package org.ruoyi.mcpserve.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具动态配置类
 * 支持通过配置文件启用/禁用各个工具
 *
 * @author OpenX
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp.tools")
public class McpToolsConfig {

    /**
     * 工具启用配置
     * key: 工具名称
     * value: 是否启用
     */
    private Map<String, Boolean> enabled = new HashMap<>();

    /**
     * 检查工具是否启用
     * 默认情况下，如果未配置则启用
     *
     * @param toolName 工具名称
     * @return 是否启用
     */
    public boolean isToolEnabled(String toolName) {
        return enabled.getOrDefault(toolName, true);
    }

    /**
     * 动态启用工具
     *
     * @param toolName 工具名称
     */
    public void enableTool(String toolName) {
        enabled.put(toolName, true);
    }

    /**
     * 动态禁用工具
     *
     * @param toolName 工具名称
     */
    public void disableTool(String toolName) {
        enabled.put(toolName, false);
    }

    /**
     * 动态设置工具启用状态
     *
     * @param toolName 工具名称
     * @param enable   是否启用
     */
    public void setToolEnabled(String toolName, boolean enable) {
        enabled.put(toolName, enable);
    }

    /**
     * 批量设置工具启用状态
     *
     * @param toolStates 工具状态映射
     */
    public void setToolsEnabled(Map<String, Boolean> toolStates) {
        enabled.putAll(toolStates);
    }
}
