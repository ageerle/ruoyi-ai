package org.ruoyi.mcp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.mcp.domain.entity.McpMarket;
import org.ruoyi.mcp.domain.vo.McpMarketVo;

/**
 * MCP 市场信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface McpMarketMapper extends BaseMapperPlus<McpMarket, McpMarketVo> {
}
