package org.ruoyi.mcp.service.core;

import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final BuiltinToolRegistry builtinToolRegistry;
    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;

    /**
     * 获取所有已启用的MCP工具的ToolProvider
     *
     * @return ToolProvider实例
     */
    public ToolProvider getAllEnabledMcpToolsProvider() {
        return langChain4jMcpToolProviderService.getAllEnabledToolsProvider();
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
