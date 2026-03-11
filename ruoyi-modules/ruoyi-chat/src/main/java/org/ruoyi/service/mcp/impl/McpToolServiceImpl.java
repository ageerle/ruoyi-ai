package org.ruoyi.service.mcp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.mcp.McpToolBo;
import org.ruoyi.domain.dto.mcp.McpToolListResult;
import org.ruoyi.domain.dto.mcp.McpToolTestResult;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.domain.vo.mcp.McpToolVo;
import org.ruoyi.enums.McpToolStatus;
import org.ruoyi.mapper.mcp.McpToolMapper;
import org.ruoyi.service.mcp.IMcpToolService;
import org.ruoyi.mcp.service.core.BuiltinToolRegistry;
import org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具服务实现
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl implements IMcpToolService {

    private final McpToolMapper baseMapper;
    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;
    private final BuiltinToolRegistry builtinToolRegistry;

    @Override
    public TableDataInfo<McpToolVo> selectPageList(McpToolBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<McpTool> wrapper = buildQueryWrapper(bo);
        Page<McpToolVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public McpToolListResult listTools(String keyword, String type, String status) {
        LambdaQueryWrapper<McpTool> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(McpTool::getName, keyword)
                .or()
                .like(McpTool::getDescription, keyword));
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(McpTool::getType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(McpTool::getStatus, status);
        }

        wrapper.orderByDesc(McpTool::getUpdateTime);

        List<McpTool> list = baseMapper.selectList(wrapper);

        return McpToolListResult.of(list);
    }

    @Override
    public List<McpToolVo> queryList(McpToolBo bo) {
        LambdaQueryWrapper<McpTool> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public McpToolVo selectById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional
    public String insert(McpToolBo bo) {
        McpTool tool = MapstructUtils.convert(bo, McpTool.class);
        if (tool.getStatus() == null) {
            tool.setStatus(McpToolStatus.ENABLED.getValue());
        }
        if (tool.getType() == null) {
            tool.setType("LOCAL");
        }
        baseMapper.insert(tool);
        return String.valueOf(tool.getId());
    }

    @Override
    @Transactional
    public String update(McpToolBo bo) {
        McpTool existingTool = baseMapper.selectById(bo.getId());
        if (existingTool != null && BuiltinToolRegistry.TYPE_BUILTIN.equals(existingTool.getType())) {
            throw new ServiceException("内置工具不允许编辑");
        }

        McpTool tool = MapstructUtils.convert(bo, McpTool.class);
        baseMapper.updateById(tool);

        // 如果工具正在使用中，需要刷新连接
        langChain4jMcpToolProviderService.refreshClient(bo.getId());

        return String.valueOf(tool.getId());
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        // 过滤掉内置工具
        List<Long> deletableIds = ids.stream()
            .filter(id -> {
                McpTool tool = baseMapper.selectById(id);
                return tool == null || !BuiltinToolRegistry.TYPE_BUILTIN.equals(tool.getType());
            })
            .toList();

        if (deletableIds.isEmpty()) {
            throw new ServiceException("所选工具均为内置工具，不允许删除");
        }

        // 刷新连接（LangChain4j会自动处理）
        deletableIds.forEach(id -> langChain4jMcpToolProviderService.refreshClient(id));
        baseMapper.deleteBatchIds(deletableIds);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        McpTool tool = new McpTool();
        tool.setId(id);
        tool.setStatus(status);
        baseMapper.updateById(tool);

        // 刷新连接
        langChain4jMcpToolProviderService.refreshClient(id);
    }

    @Override
    public McpToolTestResult testTool(Long id) {
        McpTool tool = baseMapper.selectById(id);
        if (tool == null) {
            return McpToolTestResult.fail("工具不存在");
        }

        // 根据工具类型选择不同的测试逻辑
        if (BuiltinToolRegistry.TYPE_BUILTIN.equals(tool.getType())) {
            // 内置工具 - 直接验证是否在注册表中
            return testBuiltinTool(tool);
        } else {
            // MCP 工具 (LOCAL/REMOTE) - 测试连接
            return testMcpTool(tool);
        }
    }

    /**
     * 测试内置工具
     * 内置工具不需要网络连接，只需验证是否在注册表中
     *
     * @param tool 工具信息
     * @return 测试结果
     */
    private McpToolTestResult testBuiltinTool(McpTool tool) {
        try {
            boolean isRegistered = builtinToolRegistry.hasTool(tool.getName());
            if (isRegistered) {
                return McpToolTestResult.success(
                    String.format("内置工具 [%s] 已注册，可正常使用", tool.getName()),
                    1,
                    List.of(tool.getName())
                );
            } else {
                return McpToolTestResult.fail(
                    String.format("内置工具 [%s] 未在注册表中找到，请检查工具名称是否正确", tool.getName())
                );
            }
        } catch (Exception e) {
            log.error("测试内置工具失败: {} - {}", tool.getName(), e.getMessage());
            return McpToolTestResult.fail("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试MCP工具连接
     *
     * @param tool 工具信息
     * @return 测试结果
     */
    private McpToolTestResult testMcpTool(McpTool tool) {
        try {
            boolean isHealthy = langChain4jMcpToolProviderService.checkToolHealth(tool.getId());
            if (isHealthy) {
                return McpToolTestResult.success(
                    String.format("MCP工具 [%s] 连接测试成功", tool.getName()),
                    1,
                    List.of(tool.getName())
                );
            } else {
                return McpToolTestResult.fail(
                    String.format("MCP工具 [%s] 连接测试失败", tool.getName())
                );
            }
        } catch (Exception e) {
            log.error("测试MCP工具失败: {} - {}", tool.getName(), e.getMessage());
            return McpToolTestResult.fail("测试失败: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<McpTool> buildQueryWrapper(McpToolBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<McpTool> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(bo.getType()), McpTool::getType, bo.getType())
            .eq(StringUtils.hasText(bo.getStatus()), McpTool::getStatus, bo.getStatus())
            .like(StringUtils.hasText(bo.getName()), McpTool::getName, bo.getName())
            .like(StringUtils.hasText(bo.getDescription()), McpTool::getDescription, bo.getDescription());
        return wrapper;
    }
}
