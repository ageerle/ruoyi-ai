package org.ruoyi.mcp.config;

import org.ruoyi.mcp.service.McpToolManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class McpStartupConfig {

    @Autowired
    private McpToolManagementService mcpToolManagementService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 应用启动时自动初始化 MCP 工具
        try {
            System.out.println("Starting MCP tools initialization...");
            mcpToolManagementService.initializeMcpTools();
            System.out.println("MCP tools initialization completed successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize MCP tools: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
