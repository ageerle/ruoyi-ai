package org.ruoyi.mcp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 工具测试结果
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolTestResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 发现的工具数量
     */
    private Integer toolCount;

    /**
     * 工具名称列表
     */
    private List<String> tools;

    public static McpToolTestResult success(String message, int toolCount, List<String> tools) {
        return McpToolTestResult.builder()
            .success(true)
            .message(message)
            .toolCount(toolCount)
            .tools(tools)
            .build();
    }

    public static McpToolTestResult fail(String message) {
        return McpToolTestResult.builder()
            .success(false)
            .message(message)
            .build();
    }
}
