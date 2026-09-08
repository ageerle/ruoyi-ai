package org.ruoyi.domain.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.domain.entity.mcp.McpMarket;

import java.util.List;

/**
 * MCP 市场列表返回结果
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMarketListResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 市场列表
     */
    private List<McpMarketListItem> data;

    /**
     * 总数
     */
    private int total;

    public static McpMarketListResult of(List<McpMarket> data) {
        List<McpMarketListItem> publicData = data == null
            ? List.of()
            : data.stream().map(McpMarketListItem::from).toList();
        return McpMarketListResult.builder()
            .success(true)
            .data(publicData)
            .total(publicData.size())
            .build();
    }
}
