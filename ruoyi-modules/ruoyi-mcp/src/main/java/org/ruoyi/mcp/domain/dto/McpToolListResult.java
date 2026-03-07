package org.ruoyi.mcp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.mcp.domain.entity.McpTool;

import java.util.List;

/**
 * MCP 工具列表返回结果
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolListResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 工具列表
     */
    private List<McpTool> data;

    /**
     * 总数
     */
    private int total;

    public static McpToolListResult of(List<McpTool> data) {
        return McpToolListResult.builder()
            .success(true)
            .data(data)
            .total(data != null ? data.size() : 0)
            .build();
    }
}
