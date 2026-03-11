package org.ruoyi.mcp.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.enums.McpToolStatus;
import org.ruoyi.mapper.mcp.McpToolMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j MCP 工具提供者服务
 * 从数据库读取 MCP 工具配置，创建 LangChain4j 的 McpToolProvider 供 Agent 使用
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jMcpToolProviderService {

    /**
     * 最大失败次数，超过此次数将暂时禁用工具
     */
    private static final int MAX_FAILURE_COUNT = 3;
    /**
     * 工具禁用时长（毫秒），默认 5 分钟
     */
    private static final long DISABLE_DURATION = 5 * 60 * 1000;
    private final McpToolMapper mcpToolMapper;
    private final ObjectMapper objectMapper;
    /**
     * 缓存活跃的 MCP Client
     */
    private final Map<Long, McpClient> activeClients = new ConcurrentHashMap<>();
    /**
     * 工具健康状态缓存（工具ID -> 是否健康）
     */
    private final Map<Long, Boolean> toolHealthStatus = new ConcurrentHashMap<>();
    /**
     * 工具失败次数（工具ID -> 失败次数）
     */
    private final Map<Long, Integer> toolFailureCount = new ConcurrentHashMap<>();
    /**
     * 工具禁用时间（工具ID -> 禁用截止时间戳）
     */
    private final Map<Long, Long> toolDisabledUntil = new ConcurrentHashMap<>();

    /**
     * 根据工具 ID 列表获取 ToolProvider
     *
     * @param toolIds 工具 ID 列表
     * @return ToolProvider 实例
     */
    public ToolProvider getToolProvider(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        List<McpClient> clients = new ArrayList<>();
        for (Long toolId : toolIds) {
            try {
                McpClient client = getOrCreateClient(toolId);
                if (client != null) {
                    clients.add(client);
                }
            } catch (Exception e) {
                log.error("Failed to create MCP client for tool {}: {}", toolId, e.getMessage());
            }
        }

        if (clients.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        return McpToolProvider.builder()
            .mcpClients(clients)
            .build();
    }

    /**
     * 获取所有启用的 MCP 工具的 ToolProvider
     *
     * @return ToolProvider 实例
     */
    public ToolProvider getAllEnabledToolsProvider() {
        List<McpTool> enabledTools = mcpToolMapper.selectList(
            new LambdaQueryWrapper<McpTool>()
                .eq(McpTool::getStatus, McpToolStatus.ENABLED.getValue())
        );

        if (enabledTools.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        List<Long> toolIds = enabledTools.stream()
            .map(McpTool::getId)
            .toList();

        return getToolProvider(toolIds);
    }

    /**
     * 获取指定名称的 MCP 工具的 ToolProvider
     *
     * @param toolNames 工具名称列表
     * @return ToolProvider 实例
     */
    public ToolProvider getToolProviderByNames(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        List<McpTool> tools = mcpToolMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<McpTool>()
                .in(McpTool::getName, toolNames)
                .eq(McpTool::getStatus, McpToolStatus.ENABLED.getValue())
        );

        if (tools.isEmpty()) {
            return McpToolProvider.builder().build();
        }

        List<Long> toolIds = tools.stream()
            .map(McpTool::getId)
            .toList();

        return getToolProvider(toolIds);
    }

    /**
     * 获取或创建 MCP Client
     * 包含健康检查和失败重试逻辑
     */
    private McpClient getOrCreateClient(Long toolId) {
        // 检查工具是否被禁用
        if (isToolDisabled(toolId)) {
            log.warn("Tool {} is temporarily disabled due to previous failures", toolId);
            return null;
        }

        // 尝试从缓存获取
        McpClient cachedClient = activeClients.get(toolId);
        if (cachedClient != null && isToolHealthy(toolId)) {
            return cachedClient;
        }

        // 创建新的客户端
        return activeClients.compute(toolId, (id, existingClient) -> {
            McpTool tool = mcpToolMapper.selectById(id);
            if (tool == null || !McpToolStatus.isEnabled(tool.getStatus())) {
                return null;
            }

            // 跳过内置工具（BUILTIN 类型）
            if ("BUILTIN".equals(tool.getType())) {
                log.debug("Skipping builtin tool: {}", tool.getName());
                return null;
            }

            try {
                McpClient client = createMcpClient(tool);
                // 标记工具为健康状态
                markToolHealthy(id);
                log.info("Successfully created LangChain4j MCP client for tool: {}", tool.getName());
                return client;
            } catch (Exception e) {
                log.error("Failed to create MCP client for tool {}: {}", tool.getName(), e.getMessage());
                // 记录失败并可能禁用工具
                handleToolFailure(id);
                return null;
            }
        });
    }

    /**
     * 检查工具是否被暂时禁用
     */
    private boolean isToolDisabled(Long toolId) {
        Long disabledUntil = toolDisabledUntil.get(toolId);
        if (disabledUntil == null) {
            return false;
        }
        if (System.currentTimeMillis() > disabledUntil) {
            // 禁用时间已过，重新启用
            toolDisabledUntil.remove(toolId);
            toolFailureCount.put(toolId, 0);
            log.info("Tool {} is re-enabled after disable period", toolId);
            return false;
        }
        return true;
    }

    /**
     * 检查工具是否健康
     */
    private boolean isToolHealthy(Long toolId) {
        return toolHealthStatus.getOrDefault(toolId, true);
    }

    /**
     * 标记工具为健康状态
     */
    private void markToolHealthy(Long toolId) {
        toolHealthStatus.put(toolId, true);
        toolFailureCount.put(toolId, 0);
    }

    /**
     * 处理工具失败
     */
    private void handleToolFailure(Long toolId) {
        int failures = toolFailureCount.getOrDefault(toolId, 0) + 1;
        toolFailureCount.put(toolId, failures);
        toolHealthStatus.put(toolId, false);

        if (failures >= MAX_FAILURE_COUNT) {
            // 禁用工具一段时间
            long disableUntil = System.currentTimeMillis() + DISABLE_DURATION;
            toolDisabledUntil.put(toolId, disableUntil);
            log.warn("Tool {} has failed {} times, disabling until {}",
                toolId, failures, new java.util.Date(disableUntil));
        } else {
            log.warn("Tool {} has failed {} times (max: {})",
                toolId, failures, MAX_FAILURE_COUNT);
        }
    }

    /**
     * 手动检查工具健康状态
     *
     * @param toolId 工具 ID
     * @return 工具是否健康
     */
    public boolean checkToolHealth(Long toolId) {
        McpTool tool = mcpToolMapper.selectById(toolId);
        if (tool == null || !McpToolStatus.isEnabled(tool.getStatus())) {
            return false;
        }

        try {
            // 尝试创建客户端来验证连接
            McpClient client = createMcpClient(tool);
            if (client != null) {
                markToolHealthy(toolId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Health check failed for tool {}: {}", tool.getName(), e.getMessage());
            handleToolFailure(toolId);
            return false;
        }
    }

    /**
     * 获取所有工具的健康状态
     *
     * @return 工具 ID -> 健康状态的映射
     */
    public Map<Long, Boolean> getAllToolsHealthStatus() {
        List<McpTool> allTools = mcpToolMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<McpTool>()
                .eq(McpTool::getStatus, McpToolStatus.ENABLED.getValue())
        );

        Map<Long, Boolean> statusMap = new ConcurrentHashMap<>();
        for (McpTool tool : allTools) {
            boolean isHealthy = isToolHealthy(tool.getId()) && !isToolDisabled(tool.getId());
            statusMap.put(tool.getId(), isHealthy);
        }
        return statusMap;
    }

    /**
     * 根据工具配置创建 MCP Client
     */
    private McpClient createMcpClient(McpTool tool) throws Exception {
        if ("LOCAL".equals(tool.getType())) {
            return createStdioClient(tool);
        } else if ("REMOTE".equals(tool.getType())) {
            return createRemoteClient(tool);
        }
        return null;
    }

    /**
     * 创建 STDIO Client (本地命令行工具)
     */
    private McpClient createStdioClient(McpTool tool) throws Exception {
        String configJson = tool.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Config JSON is required for LOCAL type tool");
        }

        JsonNode configNode = objectMapper.readTree(configJson);

        // 解析命令
        String command = null;
        List<String> args = new ArrayList<>();

        if (configNode.has("command")) {
            command = configNode.get("command").asText();
        }

        if (configNode.has("args") && configNode.get("args").isArray()) {
            for (JsonNode arg : configNode.get("args")) {
                args.add(arg.asText());
            }
        }

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command is required in config JSON");
        }

        // 处理 Windows 系统的命令
        command = resolveCommand(command);

        // 检查命令是否可用
        if (!isCommandAvailable(command)) {
            throw new IllegalArgumentException("Command '" + command + "' is not available on this system. Please install the required package or use a different tool.");
        }

        // 构建完整命令列表
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(args);

        log.info("Creating STDIO MCP client for tool: {}, command: {}", tool.getName(), fullCommand);

        // 创建传输层
        McpTransport transport = StdioMcpTransport.builder()
            .command(fullCommand)
            .logEvents(true)
            .build();

        // 创建客户端
        return new DefaultMcpClient.Builder()
            .transport(transport)
            .build();
    }

    /**
     * 检查命令是否在系统上可用
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            // 对于某些命令，--version 可能返回非零退出码，所以我们只检查进程是否能启动
            // 如果进程能启动并退出（无论退出码是什么），我们认为命令可用
            return true;
        } catch (Exception e) {
            log.debug("Command '{}' is not available: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * 创建远程 HTTP/SSE Client
     */
    private McpClient createRemoteClient(McpTool tool) throws Exception {
        String configJson = tool.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Config JSON is required for REMOTE type tool");
        }

        JsonNode configNode = objectMapper.readTree(configJson);

        if (!configNode.has("baseUrl")) {
            throw new IllegalArgumentException("baseUrl is required in config JSON for REMOTE type tool");
        }

        String baseUrl = configNode.get("baseUrl").asText();
        log.info("Creating HTTP/SSE MCP client for tool: {}, baseUrl: {}", tool.getName(), baseUrl);

        // 创建 HTTP/SSE 传输层
        McpTransport transport = StreamableHttpMcpTransport.builder()
            .url(baseUrl)
            .logRequests(true)
            .build();

        // 创建客户端
        return new DefaultMcpClient.Builder()
            .transport(transport)
            .build();
    }

    /**
     * 解析命令，处理 Windows 系统的兼容性问题
     */
    private String resolveCommand(String command) {
        if (command == null || command.isBlank()) {
            return command;
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows) {
            String lowerCommand = command.toLowerCase();
            if (lowerCommand.equals("npx") || lowerCommand.equals("npm") ||
                lowerCommand.equals("node") || lowerCommand.equals("pnpm") ||
                lowerCommand.equals("yarn") || lowerCommand.equals("uvx") ||
                lowerCommand.equals("uv")) {
                String resolvedCommand = command + ".cmd";
                log.debug("Windows detected, resolved command: {} -> {}", command, resolvedCommand);
                return resolvedCommand;
            }
        }

        return command;
    }

    /**
     * 刷新指定工具的客户端连接
     */
    public void refreshClient(Long toolId) {
        closeClient(toolId);
        log.info("Refreshed MCP client for tool: {}", toolId);
    }

    /**
     * 关闭指定工具的客户端连接
     */
    private void closeClient(Long toolId) {
        McpClient client = activeClients.remove(toolId);
        if (client != null) {
            try {
                // LangChain4j McpClient 没有 close 方法，直接移除即可
                log.info("Removed MCP client for tool: {}", toolId);
            } catch (Exception e) {
                log.warn("Error closing MCP client for tool {}: {}", toolId, e.getMessage());
            }
        }
    }

    /**
     * 应用关闭时清理所有连接
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up {} MCP clients...", activeClients.size());
        activeClients.keySet().forEach(this::closeClient);
    }

    /**
     * 获取当前活跃的客户端数量
     */
    public int getActiveClientCount() {
        return activeClients.size();
    }
}
