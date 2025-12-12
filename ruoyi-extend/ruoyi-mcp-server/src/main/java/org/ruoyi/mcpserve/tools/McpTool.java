package org.ruoyi.mcpserve.tools;

/**
 * MCP工具标记接口
 * 所有MCP工具类都需要实现此接口，以便动态加载器识别
 *
 * @author OpenX
 */
public interface McpTool {

    /**
     * 获取工具名称，用于配置文件中的启用/禁用控制
     *
     * @return 工具名称
     */
    String getToolName();
}
