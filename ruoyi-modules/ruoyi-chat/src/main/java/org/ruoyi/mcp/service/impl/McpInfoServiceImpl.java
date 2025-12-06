package org.ruoyi.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.McpInfo;
import org.ruoyi.domain.bo.McpInfoBo;
import org.ruoyi.domain.vo.McpInfoVo;
import org.ruoyi.mapper.McpInfoMapper;
import org.ruoyi.mcp.config.McpConfig;
import org.ruoyi.mcp.config.McpServerConfig;
import org.ruoyi.mcp.domain.McpInfoRequest;
import org.ruoyi.mcp.service.McpInfoService;
import org.springframework.stereotype.Service;

import java.util.*;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        McpInfo add = MapstructUtils.convert(bo, McpInfo.class);
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
        McpInfo update = MapstructUtils.convert(bo, McpInfo.class);
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

    /**
     * 根据服务器名称获取工具配置
     */
    @Override
    public McpServerConfig getToolConfigByName(String serverName) {
        McpInfo tool = baseMapper.selectByServerName(serverName);
        if (tool != null) {
            return convertToMcpServerConfig(tool);
        }
        return null;
    }

    /**
     * 获取所有活跃的 MCP 工具配置
     */
    @Override
    public McpConfig getAllActiveMcpConfig() {
        List<McpInfo> activeTools = baseMapper.selectActiveServers();
        Map<String, McpServerConfig> servers = new HashMap<>();

        for (McpInfo tool : activeTools) {
            McpServerConfig serverConfig = convertToMcpServerConfig(tool);
            servers.put(tool.getServerName(), serverConfig);
        }

        McpConfig config = new McpConfig();
        config.setMcpServers(servers);
        return config;
    }

    /**
     * 获取所有活跃服务器名称
     */
    @Override
    public List<String> getActiveServerNames() {
        return baseMapper.selectActiveServerNames();
    }

    /**
     * 保存或更新 MCP 工具配置
     */
    @Override
    public McpInfo saveToolConfig(McpInfoRequest request) {
        McpInfo existingTool = baseMapper.selectByServerName(request.getServerName());

        McpInfo tool;
        if (existingTool != null) {
            tool = existingTool;
        } else {
            tool = new McpInfo();
        }

        tool.setServerName(request.getServerName());
        tool.setCommand(request.getCommand());

        try {
            tool.setArguments(objectMapper.writeValueAsString(request.getArgs()));
            if (request.getEnv() != null) {
                tool.setEnv(objectMapper.writeValueAsString(request.getEnv()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON data", e);
        }

        tool.setDescription(request.getDescription());
        tool.setStatus(true); // 默认启用

        if (existingTool != null) {
            baseMapper.updateById(tool);
        } else {
            baseMapper.insert(tool);
        }

        return tool;
    }

    /**
     * 删除工具配置
     */
    @Override
    public boolean deleteToolConfig(String serverName) {
        return baseMapper.deleteByServerName(serverName) > 0;
    }

    /**
     * 更新工具状态
     */
    @Override
    public boolean updateToolStatus(String serverName, Boolean status) {
        return baseMapper.updateActiveStatus(serverName, status) > 0;
    }

    /**
     * 启用工具
     */
    @Override
    public boolean enableTool(String serverName) {
        return updateToolStatus(serverName, true);
    }

    /**
     * 禁用工具
     */
    @Override
    public boolean disableTool(String serverName) {
        return updateToolStatus(serverName, false);
    }

    private McpServerConfig convertToMcpServerConfig(McpInfo tool) {
        McpServerConfig config = new McpServerConfig();
        config.setCommand(tool.getCommand());

        try {
            // 解析 args
            if (tool.getArguments() != null && !tool.getArguments().isEmpty()) {
                List<String> args = objectMapper.readValue(tool.getArguments(), new TypeReference<List<String>>() {
                });
                config.setArgs(args);
            } else {
                config.setArgs(new ArrayList<>());
            }

            // 解析 env
            if (tool.getEnv() != null && !tool.getEnv().isEmpty()) {
                Map<String, String> env = objectMapper.readValue(tool.getEnv(), new TypeReference<Map<String, String>>() {
                });
                config.setEnv(env);
            } else {
                config.setEnv(new HashMap<>());
            }

        } catch (Exception e) {
            config.setArgs(new ArrayList<>());
            config.setEnv(new HashMap<>());
        }

        return config;
    }
}
