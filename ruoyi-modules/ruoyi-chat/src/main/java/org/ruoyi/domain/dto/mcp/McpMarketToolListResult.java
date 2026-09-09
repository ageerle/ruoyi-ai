package org.ruoyi.domain.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.domain.entity.mcp.McpMarketTool;

import java.util.List;

/**
 * MCP 市场工具列表返回结果（分页）
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMarketToolListResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 工具列表
     */
    private List<McpMarketToolListItem> data;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private long pages;

    public static McpMarketToolListResult of(List<McpMarketTool> data, long total, int page, int size) {
        long pages = (total + size - 1) / size;
        List<McpMarketToolListItem> publicData = data == null
            ? List.of()
            : data.stream().map(McpMarketToolListItem::from).toList();
        return McpMarketToolListResult.builder()
            .success(true)
            .data(publicData)
            .total(total)
            .page(page)
            .size(size)
            .pages(pages)
            .build();
    }
}
