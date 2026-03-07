package org.ruoyi.mcp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 市场工具刷新结果
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMarketRefreshResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 新增工具数量
     */
    private int addedCount;

    /**
     * 更新工具数量
     */
    private int updatedCount;
}
