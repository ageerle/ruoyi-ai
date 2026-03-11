package org.ruoyi.mapper.mcp;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.domain.vo.mcp.McpToolVo;

/**
 * MCP 工具信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface McpToolMapper extends BaseMapperPlus<McpTool, McpToolVo> {
}
