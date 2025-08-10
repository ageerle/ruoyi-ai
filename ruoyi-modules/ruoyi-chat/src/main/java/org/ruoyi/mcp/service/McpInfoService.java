package org.ruoyi.mcp.service;

    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;
    import org.ruoyi.domain.bo.McpInfoBo;
    import org.ruoyi.domain.vo.McpInfoVo;

    import java.util.Collection;
import java.util.List;

/**
 * MCPService接口
 *
 * @author ageerle
 * @date Sat Aug 09 16:50:58 CST 2025
 */
public interface McpInfoService {

    /**
     * 查询MCP
     */
        McpInfoVo queryById(Integer mcpId);

        /**
         * 查询MCP列表
         */
        TableDataInfo<McpInfoVo> queryPageList(McpInfoBo bo, PageQuery pageQuery);

    /**
     * 查询MCP列表
     */
    List<McpInfoVo> queryList(McpInfoBo bo);

    /**
     * 新增MCP
     */
    Boolean insertByBo(McpInfoBo bo);

    /**
     * 修改MCP
     */
    Boolean updateByBo(McpInfoBo bo);

    /**
     * 校验并批量删除MCP信息
     */
    Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid);
}
