package org.ruoyi.domain.dto.mcp;

import org.ruoyi.domain.entity.mcp.McpMarketTool;

import java.util.Date;

/** Public market-tool projection. Provider metadata is deliberately excluded. */
public record McpMarketToolListItem(
    Long id,
    Long marketId,
    String toolName,
    String toolDescription,
    String toolVersion,
    Boolean isLoaded,
    Long localToolId,
    Date createTime,
    Date updateTime
) {

    public static McpMarketToolListItem from(McpMarketTool tool) {
        return new McpMarketToolListItem(tool.getId(), tool.getMarketId(), tool.getToolName(),
            tool.getToolDescription(), tool.getToolVersion(), tool.getIsLoaded(),
            tool.getLocalToolId(), tool.getCreateTime(), tool.getUpdateTime());
    }
}
