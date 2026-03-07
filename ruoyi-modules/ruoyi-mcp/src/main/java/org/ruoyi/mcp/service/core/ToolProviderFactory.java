package org.ruoyi.mcp.service.core;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.mcp.domain.entity.McpTool;
import org.ruoyi.mcp.enums.McpToolStatus;
import org.ruoyi.mcp.mapper.McpToolMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统一工具提供工厂
 * 整合所有类型的MCP工具提供者，为Agent和Chat服务提供统一的工具获取入口
 *
 * <p>支持的工具类型：
 * <ul>
 *   <li>BUILTIN - 内置工具（如文件操作工具）</li>
 *   <li>LOCAL - 本地STDIO工具（通过命令行启动的MCP服务器）</li>
 *   <li>REMOTE - 远程HTTP/SSE工具（通过网络连接的MCP服务器）</li>
 * </ul>
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolProviderFactory {

    /**
     * 工具类型常量
     */
    public static final String TYPE_BUILTIN = "BUILTIN";
    public static final String TYPE_LOCAL = "LOCAL";
    public static final String TYPE_REMOTE = "REMOTE";
    private final BuiltinToolRegistry builtinToolRegistry;
    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;
    private final McpToolMapper mcpToolMapper;

    /**
     * 根据工具ID列表获取LangChain4j的ToolProvider
     * 用于LangChain4j Agent框架使用工具
     *
     * @param toolIds 工具ID列表
     * @return ToolProvider实例
     */
    public ToolProvider getToolProvider(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        // 只获取非内置工具（LangChain4j的MCP工具）
        List<Long> mcpToolIds = new ArrayList<>();

        for (Long toolId : toolIds) {
            McpTool tool = mcpToolMapper.selectById(toolId);
            if (tool != null && McpToolStatus.isEnabled(tool.getStatus())) {
                if (!TYPE_BUILTIN.equals(tool.getType())) {
                    mcpToolIds.add(toolId);
                }
            }
        }

        // 使用LangChain4j服务获取MCP工具的ToolProvider
        return langChain4jMcpToolProviderService.getToolProvider(mcpToolIds);
    }

    /**
     * 根据工具名称列表获取LangChain4j的ToolProvider
     *
     * @param toolNames 工具名称列表
     * @return ToolProvider实例
     */
    public ToolProvider getToolProviderByNames(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        // 直接使用LangChain4j服务，它已经实现了按名称查询
        return langChain4jMcpToolProviderService.getToolProviderByNames(toolNames);
    }

    /**
     * 获取所有已启用的MCP工具的ToolProvider
     *
     * @return ToolProvider实例
     */
    public ToolProvider getAllEnabledMcpToolsProvider() {
        return langChain4jMcpToolProviderService.getAllEnabledToolsProvider();
    }

    /**
     * 检查工具是否为内置工具
     *
     * @param toolName 工具名称
     * @return 是否为内置工具
     */
    public boolean isBuiltinTool(String toolName) {
        return builtinToolRegistry.hasTool(toolName);
    }

    /**
     * 根据工具名称获取工具ID
     *
     * @param toolName 工具名称
     * @return 工具ID，未找到返回null
     */
    public Long getToolIdByName(String toolName) {
        McpTool tool = mcpToolMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<McpTool>()
                .eq(McpTool::getName, toolName)
                .last("LIMIT 1")
        );
        return tool != null ? tool.getId() : null;
    }

    /**
     * 根据工具名称列表获取工具ID列表
     *
     * @param toolNames 工具名称列表
     * @return 工具ID列表
     */
    public List<Long> getToolIdsByNames(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        List<McpTool> tools = mcpToolMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<McpTool>()
                .in(McpTool::getName, toolNames)
                .eq(McpTool::getStatus, McpToolStatus.ENABLED.getValue())
        );

        return tools.stream()
            .map(McpTool::getId)
            .toList();
    }

    /**
     * 刷新工具连接
     *
     * @param toolId 工具ID
     */
    public void refreshTool(Long toolId) {
        langChain4jMcpToolProviderService.refreshClient(toolId);
        log.info("已刷新工具连接: toolId={}", toolId);
    }

    /**
     * 获取工具健康状态
     *
     * @return 工具ID -> 健康状态的映射
     */
    public Map<Long, Boolean> getToolsHealthStatus() {
        return langChain4jMcpToolProviderService.getAllToolsHealthStatus();
    }

    /**
     * 获取所有 BUILTIN 工具对象
     * 这些对象包含 @Tool 注解的方法，可直接用于 AgenticServices
     *
     * @return BUILTIN 工具对象列表
     */
    public List<Object> getAllBuiltinToolObjects() {
        return builtinToolRegistry.getAllBuiltinToolObjects();
    }
}
