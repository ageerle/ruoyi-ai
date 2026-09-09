package org.ruoyi.domain.dto.mcp;

import org.ruoyi.domain.entity.mcp.McpTool;

import java.util.Date;

/** Public MCP tool projection. Connection configuration is deliberately write-only. */
public record McpToolListItem(
    Long id,
    String name,
    String description,
    String type,
    String status,
    Date createTime,
    Date updateTime
) {

    public static McpToolListItem from(McpTool tool) {
        return new McpToolListItem(tool.getId(), tool.getName(), tool.getDescription(),
            tool.getType(), tool.getStatus(), tool.getCreateTime(), tool.getUpdateTime());
    }
}
