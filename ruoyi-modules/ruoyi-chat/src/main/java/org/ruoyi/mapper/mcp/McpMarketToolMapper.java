package org.ruoyi.mapper.mcp;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.mcp.McpMarketTool;

/**
 * MCP 市场工具关联 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface McpMarketToolMapper extends BaseMapperPlus<McpMarketTool, Void> {

    /**
     * Atomically resolves a market tool through an owning market in the authenticated tenant.
     * Tenant interception is intentionally bypassed because both tenant predicates are explicit;
     * this remains fail-closed even when the global tenant plugin is disabled or reconfigured.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        SELECT t.id, t.market_id, t.tool_name, t.tool_description, t.tool_version,
               t.tool_metadata, t.is_loaded, t.local_tool_id, t.tenant_id,
               t.create_dept, t.create_by, t.create_time, t.update_by, t.update_time
        FROM mcp_market_tool t
        INNER JOIN mcp_market_info m ON m.id = t.market_id
        WHERE t.id = #{toolId}
          AND t.tenant_id = #{tenantId}
          AND m.tenant_id = #{tenantId}
          AND m.del_flag = '0'
        LIMIT 1
        """)
    McpMarketTool selectOwnedById(@Param("toolId") Long toolId,
                                  @Param("tenantId") String tenantId);

    /**
     * Lists public market-tool fields only when both the child row and its owning market
     * belong to the authenticated tenant. The predicates are intentionally independent
     * from the tenant interceptor so disabling that plugin cannot broaden this query.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        SELECT t.id, t.market_id, t.tool_name, t.tool_description, t.tool_version,
               t.is_loaded, t.local_tool_id, t.create_time, t.update_time
        FROM mcp_market_tool t
        INNER JOIN mcp_market_info m ON m.id = t.market_id
        WHERE t.market_id = #{marketId}
          AND t.tenant_id = #{tenantId}
          AND m.tenant_id = #{tenantId}
          AND m.del_flag = '0'
        ORDER BY t.create_time DESC
        """)
    Page<McpMarketTool> selectOwnedPage(@Param("page") Page<McpMarketTool> page,
                                        @Param("marketId") Long marketId,
                                        @Param("tenantId") String tenantId);
}
