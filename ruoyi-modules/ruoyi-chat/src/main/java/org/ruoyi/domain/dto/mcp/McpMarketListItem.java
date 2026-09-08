package org.ruoyi.domain.dto.mcp;

import org.ruoyi.domain.entity.mcp.McpMarket;

import java.util.Date;

/** Public MCP market projection. Authentication configuration is deliberately write-only. */
public record McpMarketListItem(
    Long id,
    String name,
    String url,
    String description,
    String status,
    Date createTime,
    Date updateTime
) {

    public static McpMarketListItem from(McpMarket market) {
        return new McpMarketListItem(market.getId(), market.getName(), market.getUrl(),
            market.getDescription(), market.getStatus(), market.getCreateTime(),
            market.getUpdateTime());
    }
}
