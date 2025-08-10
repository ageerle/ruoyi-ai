package org.ruoyi.mcp.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;
    import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.McpInfo;
import org.ruoyi.domain.bo.McpInfoBo;
import org.ruoyi.domain.vo.McpInfoVo;
import org.ruoyi.mapper.McpInfoMapper;
import org.ruoyi.mcp.service.McpInfoService;
import org.springframework.stereotype.Service;

import org.ruoyi.common.core.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * MCPService业务层处理
 *
 * @author ageerle
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@RequiredArgsConstructor
@Service
public class McpInfoServiceImpl implements McpInfoService {

    private final McpInfoMapper baseMapper;

    /**
     * 查询MCP
     */
    @Override
    public McpInfoVo queryById(Integer mcpId) {
        return baseMapper.selectVoById(mcpId);
    }

        /**
         * 查询MCP列表
         */
        @Override
        public TableDataInfo<McpInfoVo> queryPageList(McpInfoBo bo, PageQuery pageQuery) {
            LambdaQueryWrapper<McpInfo> lqw = buildQueryWrapper(bo);
            Page<McpInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
            return TableDataInfo.build(result);
        }

    /**
     * 查询MCP列表
     */
    @Override
    public List<McpInfoVo> queryList(McpInfoBo bo) {
        LambdaQueryWrapper<McpInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<McpInfo> buildQueryWrapper(McpInfoBo bo) {
        LambdaQueryWrapper<McpInfo> lqw = Wrappers.lambdaQuery();
                    lqw.like(StringUtils.isNotBlank(bo.getServerName()), McpInfo::getServerName, bo.getServerName());
                    lqw.eq(StringUtils.isNotBlank(bo.getTransportType()), McpInfo::getTransportType, bo.getTransportType());
                    lqw.eq(StringUtils.isNotBlank(bo.getCommand()), McpInfo::getCommand, bo.getCommand());
                    lqw.eq(bo.getStatus() != null, McpInfo::getStatus, bo.getStatus());
        return lqw;
    }

    /**
     * 新增MCP
     */
    @Override
    public Boolean insertByBo(McpInfoBo bo) {
        McpInfo add = MapstructUtils.convert(bo, McpInfo. class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setMcpId(add.getMcpId());
        }
        return flag;
    }

    /**
     * 修改MCP
     */
    @Override
    public Boolean updateByBo(McpInfoBo bo) {
        McpInfo update = MapstructUtils.convert(bo, McpInfo. class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(McpInfo entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除MCP
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
