package org.ruoyi.mcp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.mcp.domain.entity.McpMarket;

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
    private List<McpMarket> data;

    /**
     * 总数
     */
    private int total;

    public static McpMarketListResult of(List<McpMarket> data) {
        return McpMarketListResult.builder()
            .success(true)
            .data(data)
            .total(data != null ? data.size() : 0)
            .build();
    }
}
