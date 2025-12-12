package org.ruoyi.mcp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class McpConfig {
    @JsonProperty("mcpServers")
    private Map<String, McpServerConfig> mcpServers;

    // getters and setters
    public Map<String, McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(Map<String, McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }
}


