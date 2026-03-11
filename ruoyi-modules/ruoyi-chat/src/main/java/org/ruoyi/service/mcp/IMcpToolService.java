package org.ruoyi.service.mcp;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.mcp.McpToolBo;
import org.ruoyi.domain.dto.mcp.McpToolListResult;
import org.ruoyi.domain.dto.mcp.McpToolTestResult;
import org.ruoyi.domain.vo.mcp.McpToolVo;

import java.util.List;

/**
 * MCP 工具服务接口
 *
 * @author ruoyi team
 */
public interface IMcpToolService {

    /**
     * 分页查询工具列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 工具分页列表
     */
    TableDataInfo<McpToolVo> selectPageList(McpToolBo bo, PageQuery pageQuery);

    /**
     * 查询工具列表（不分页）
     *
     * @param keyword 关键词
     * @param type    类型
     * @param status  状态
     * @return 工具列表结果
     */
    McpToolListResult listTools(String keyword, String type, String status);

    /**
     * 查询工具列表（用于导出）
     *
     * @param bo 查询条件
     * @return 工具列表
     */
    List<McpToolVo> queryList(McpToolBo bo);

    /**
     * 根据ID查询工具
     *
     * @param id 工具ID
     * @return 工具信息
     */
    McpToolVo selectById(Long id);

    /**
     * 新增工具
     *
     * @param bo 工具信息
     * @return 新增后的工具ID
     */
    String insert(McpToolBo bo);

    /**
     * 更新工具
     *
     * @param bo 工具信息
     * @return 结果
     */
    String update(McpToolBo bo);

    /**
     * 删除工具
     *
     * @param ids 工具 ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 更新工具状态
     *
     * @param id     工具 ID
     * @param status 状态
     */
    void updateStatus(Long id, String status);

    /**
     * 测试工具连接
     *
     * @param id 工具 ID
     * @return 测试结果
     */
    McpToolTestResult testTool(Long id);
}
