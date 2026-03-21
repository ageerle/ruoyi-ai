package org.ruoyi.service.mcp;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.mcp.McpMarketBo;
import org.ruoyi.domain.dto.mcp.McpMarketListResult;
import org.ruoyi.domain.dto.mcp.McpMarketRefreshResult;
import org.ruoyi.domain.dto.mcp.McpMarketToolListResult;
import org.ruoyi.domain.vo.mcp.McpMarketVo;

import java.util.List;

/**
 * MCP 市场服务接口
 *
 * @author ruoyi team
 */
public interface IMcpMarketService {

    /**
     * 分页查询市场列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 市场分页列表
     */
    TableDataInfo<McpMarketVo> selectPageList(McpMarketBo bo, PageQuery pageQuery);

    /**
     * 查询市场列表（不分页）
     *
     * @param keyword 关键词
     * @param status  状态
     * @return 市场列表结果
     */
    McpMarketListResult listMarkets(String keyword, String status);

    /**
     * 查询市场列表（用于导出）
     *
     * @param bo 查询条件
     * @return 市场列表
     */
    List<McpMarketVo> queryList(McpMarketBo bo);

    /**
     * 根据ID查询市场
     *
     * @param id 市场ID
     * @return 市场信息
     */
    McpMarketVo selectById(Long id);

    /**
     * 新增市场
     *
     * @param bo 市场信息
     * @return 新增后的市场ID
     */
    String insert(McpMarketBo bo);

    /**
     * 更新市场
     *
     * @param bo 市场信息
     * @return 结果
     */
    String update(McpMarketBo bo);

    /**
     * 删除市场
     *
     * @param ids 市场 ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 更新市场状态
     *
     * @param id     市场 ID
     * @param status 状态
     */
    void updateStatus(Long id, String status);

    /**
     * 获取市场工具列表
     *
     * @param marketId 市场 ID
     * @param page     页码
     * @param size     每页大小
     * @return 工具列表结果
     */
    McpMarketToolListResult getMarketTools(Long marketId, int page, int size);

    /**
     * 刷新市场工具列表
     *
     * @param marketId 市场 ID
     * @return 刷新结果
     */
    McpMarketRefreshResult refreshMarketTools(Long marketId);

    /**
     * 加载工具到本地
     *
     * @param toolId 市场工具 ID
     */
    void loadToolToLocal(Long toolId);

    /**
     * 批量加载工具到本地
     *
     * @param toolIds 工具 ID 列表
     * @return 成功加载的数量
     */
    int batchLoadTools(List<Long> toolIds);
}
