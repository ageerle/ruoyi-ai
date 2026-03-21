package org.ruoyi.mapper.mcp;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.mcp.McpMarket;
import org.ruoyi.domain.vo.mcp.McpMarketVo;

/**
 * MCP 市场信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface McpMarketMapper extends BaseMapperPlus<McpMarket, McpMarketVo> {
}
