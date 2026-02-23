package org.ruoyi.mcp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.mcp.domain.entity.McpTool;
import org.ruoyi.mcp.domain.vo.McpToolVo;

/**
 * MCP 工具信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface McpToolMapper extends BaseMapperPlus<McpTool, McpToolVo> {
}
