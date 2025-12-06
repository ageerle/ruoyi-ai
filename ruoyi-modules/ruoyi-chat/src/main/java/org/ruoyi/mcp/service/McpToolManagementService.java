package org.ruoyi.mcp.service;

import org.ruoyi.domain.McpInfo;
import org.ruoyi.mcp.config.McpConfig;
import org.ruoyi.mcp.config.McpProcessManager;
import org.ruoyi.mcp.config.McpServerConfig;
import org.ruoyi.mcp.domain.McpInfoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class McpToolManagementService {

    @Autowired
    private McpInfoService mcpInfoService;

    @Autowired
    private McpProcessManager mcpProcessManager;

    /**
     * 初始化所有 MCP 工具（应用启动时调用）
     */
    public void initializeMcpTools() {
        System.out.println("Initializing MCP tools...");

        McpConfig config = mcpInfoService.getAllActiveMcpConfig();
        if (config.getMcpServers() != null) {
            int successCount = 0;
            int totalCount = config.getMcpServers().size();

            for (Map.Entry<String, McpServerConfig> entry : config.getMcpServers().entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();

                System.out.println("Starting MCP server: " + serverName);
                System.out.println("Starting MCP serverConfig: " + serverConfig);
                // 启动 MCP 服务器进程
                boolean started = mcpProcessManager.startMcpServer(serverName, serverConfig);

                if (started) {
                    successCount++;
                    System.out.println("✓ MCP server [" + serverName + "] started successfully");
                } else {
                    System.err.println("✗ Failed to start MCP server [" + serverName + "]");
                }
            }

            System.out.println("MCP tools initialization completed. " +
                    successCount + "/" + totalCount + " tools started.");
        }
    }

    /**
     * 添加新的 MCP 工具并启动
     */
    public boolean addMcpTool(McpInfoRequest request) {
        try {
            McpInfo tool = mcpInfoService.saveToolConfig(request);

            // 启动新添加的工具
            McpServerConfig config = new McpServerConfig();
            config.setCommand(request.getCommand());
            config.setArgs(request.getArgs());
            config.setEnv(request.getEnv());

            boolean started = mcpProcessManager.startMcpServer(
                    request.getServerName(),
                    config
            );

            return started;
        } catch (Exception e) {
            System.err.println("Failed to add MCP tool: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取 MCP 工具状态
     */
    public Map<String, Object> getMcpToolStatus() {
        List<String> activeTools = mcpInfoService.getActiveServerNames();
        Map<String, Object> status = new HashMap<>();

        for (String serverName : activeTools) {
            boolean isRunning = mcpProcessManager.isMcpServerRunning(serverName);
            McpProcessManager.McpServerProcess processInfo = mcpProcessManager.getProcessInfo(serverName);

            Map<String, Object> toolStatus = new HashMap<>();
            toolStatus.put("running", isRunning);
            toolStatus.put("processInfo", processInfo);

            status.put(serverName, toolStatus);
        }

        return status;
    }

    /**
     * 重启指定的 MCP 工具
     */
    public boolean restartMcpTool(String serverName) {
        McpServerConfig config = mcpInfoService.getToolConfigByName(serverName);
        if (config == null) {
            return false;
        }

        return mcpProcessManager.restartMcpServer(
                serverName,
                config.getCommand(),
                config.getArgs(),
                config.getEnv()
        );
    }

    /**
     * 停止指定的 MCP 工具
     */
    public boolean stopMcpTool(String serverName) {
        return mcpProcessManager.stopMcpServer(serverName);
    }

    /**
     * 获取所有运行中的工具
     */
    public Set<String> getRunningTools() {
        return mcpProcessManager.getRunningMcpServers();
    }
}
