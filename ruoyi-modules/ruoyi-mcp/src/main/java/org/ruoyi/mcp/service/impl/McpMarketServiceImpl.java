package org.ruoyi.mcp.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.mcp.domain.bo.McpMarketBo;
import org.ruoyi.mcp.domain.dto.McpMarketListResult;
import org.ruoyi.mcp.domain.dto.McpMarketRefreshResult;
import org.ruoyi.mcp.domain.dto.McpMarketToolListResult;
import org.ruoyi.mcp.domain.entity.McpMarket;
import org.ruoyi.mcp.domain.entity.McpMarketTool;
import org.ruoyi.mcp.domain.entity.McpTool;
import org.ruoyi.mcp.domain.vo.McpMarketVo;
import org.ruoyi.mcp.enums.McpToolStatus;
import org.ruoyi.mcp.mapper.McpMarketMapper;
import org.ruoyi.mcp.mapper.McpMarketToolMapper;
import org.ruoyi.mcp.mapper.McpToolMapper;
import org.ruoyi.mcp.service.IMcpMarketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 市场服务实现
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpMarketServiceImpl implements IMcpMarketService {

    private final McpMarketMapper baseMapper;
    private final McpMarketToolMapper mcpMarketToolMapper;
    private final McpToolMapper mcpToolMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TableDataInfo<McpMarketVo> selectPageList(McpMarketBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<McpMarket> wrapper = buildQueryWrapper(bo);
        Page<McpMarketVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public McpMarketListResult listMarkets(String keyword, String status) {
        LambdaQueryWrapper<McpMarket> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(McpMarket::getName, keyword)
                .or()
                .like(McpMarket::getDescription, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(McpMarket::getStatus, status);
        }

        wrapper.orderByDesc(McpMarket::getUpdateTime);

        List<McpMarket> list = baseMapper.selectList(wrapper);

        return McpMarketListResult.of(list);
    }

    @Override
    public List<McpMarketVo> queryList(McpMarketBo bo) {
        LambdaQueryWrapper<McpMarket> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public McpMarketVo selectById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Transactional
    public String insert(McpMarketBo bo) {
        McpMarket market = MapstructUtils.convert(bo, McpMarket.class);
        if (market.getStatus() == null) {
            market.setStatus(McpToolStatus.ENABLED.getValue());
        }
        baseMapper.insert(market);
        return String.valueOf(market.getId());
    }

    @Override
    @Transactional
    public String update(McpMarketBo bo) {
        McpMarket market = MapstructUtils.convert(bo, McpMarket.class);
        baseMapper.updateById(market);
        return String.valueOf(market.getId());
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            // 先删除关联的市场工具
            LambdaQueryWrapper<McpMarketTool> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(McpMarketTool::getMarketId, id);
            mcpMarketToolMapper.delete(wrapper);
        }

        // 删除市场
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        McpMarket market = new McpMarket();
        market.setId(id);
        market.setStatus(status);
        baseMapper.updateById(market);
    }

    @Override
    public McpMarketToolListResult getMarketTools(Long marketId, int page, int size) {
        LambdaQueryWrapper<McpMarketTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpMarketTool::getMarketId, marketId);
        wrapper.orderByDesc(McpMarketTool::getCreateTime);

        Page<McpMarketTool> pageResult = mcpMarketToolMapper.selectPage(new Page<>(page, size), wrapper);

        return McpMarketToolListResult.of(
            pageResult.getRecords(),
            pageResult.getTotal(),
            (int) pageResult.getCurrent(),
            (int) pageResult.getSize()
        );
    }

    @Override
    @Transactional
    public McpMarketRefreshResult refreshMarketTools(Long marketId) {
        McpMarket market = baseMapper.selectById(marketId);
        if (market == null) {
            throw new ServiceException("市场不存在");
        }

        int addedCount = 0;
        int updatedCount = 0;

        try {
            // 从市场 URL 获取工具列表（使用hutool的HttpUtil）
            HttpResponse response = HttpRequest.get(market.getUrl())
                .timeout(30000) // 30秒超时
                .execute();
            String responseBody = response.body();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 假设响应格式为 { "data": [...] } 或直接是数组
            JsonNode toolsNode = rootNode.has("data") ? rootNode.get("data") : rootNode;

            if (toolsNode.isArray()) {
                // 获取现有工具
                LambdaQueryWrapper<McpMarketTool> existingWrapper = new LambdaQueryWrapper<>();
                existingWrapper.eq(McpMarketTool::getMarketId, marketId);
                List<McpMarketTool> existingTools = mcpMarketToolMapper.selectList(existingWrapper);

                // 创建现有工具的名称到ID映射
                Map<String, McpMarketTool> existingToolMap = existingTools.stream()
                    .collect(Collectors.toMap(McpMarketTool::getToolName, t -> t));

                // 处理新工具
                for (JsonNode toolNode : toolsNode) {
                    String toolName = getTextValue(toolNode, "name", "title");
                    McpMarketTool existingTool = existingToolMap.get(toolName);

                    if (existingTool != null) {
                        // 更新现有工具
                        existingTool.setToolDescription(getTextValue(toolNode, "description", "desc"));
                        existingTool.setToolVersion(getTextValue(toolNode, "version"));
                        existingTool.setToolMetadata(toolNode.toString());
                        mcpMarketToolMapper.updateById(existingTool);
                        updatedCount++;
                    } else {
                        // 插入新工具
                        McpMarketTool tool = new McpMarketTool();
                        tool.setMarketId(marketId);
                        tool.setToolName(toolName);
                        tool.setToolDescription(getTextValue(toolNode, "description", "desc"));
                        tool.setToolVersion(getTextValue(toolNode, "version"));
                        tool.setToolMetadata(toolNode.toString());
                        tool.setIsLoaded(false);
                        mcpMarketToolMapper.insert(tool);
                        addedCount++;
                    }
                }
            }

            log.info("Successfully refreshed market tools for market: {}, added: {}, updated: {}",
                market.getName(), addedCount, updatedCount);

            return McpMarketRefreshResult.builder()
                .success(true)
                .message("刷新成功")
                .addedCount(addedCount)
                .updatedCount(updatedCount)
                .build();
        } catch (Exception e) {
            log.error("Failed to refresh market tools for market {}: {}", marketId, e.getMessage());
            return McpMarketRefreshResult.builder()
                .success(false)
                .message("刷新市场工具列表失败: " + e.getMessage())
                .addedCount(0)
                .updatedCount(0)
                .build();
        }
    }

    /**
     * 从 JSON 节点获取文本值，尝试多个字段名
     */
    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void loadToolToLocal(Long toolId) {
        McpMarketTool marketTool = mcpMarketToolMapper.selectById(toolId);
        if (marketTool == null) {
            throw new ServiceException("市场工具不存在");
        }

        if (marketTool.getIsLoaded()) {
            throw new ServiceException("工具已加载到本地");
        }

        try {
            // 解析工具元数据
            JsonNode metadata = objectMapper.readTree(marketTool.getToolMetadata());

            // 创建本地工具
            McpTool localTool = new McpTool();
            localTool.setName(marketTool.getToolName());
            localTool.setDescription(marketTool.getToolDescription());

            // 根据元数据判断类型
            if (metadata.has("baseUrl") || metadata.has("url")) {
                localTool.setType("REMOTE");
                String baseUrl = metadata.has("baseUrl") ? metadata.get("baseUrl").asText() :
                    metadata.has("url") ? metadata.get("url").asText() : null;
                localTool.setConfigJson(objectMapper.writeValueAsString(Map.of("baseUrl", baseUrl != null ? baseUrl : "")));
            } else {
                localTool.setType("LOCAL");
                // 构建本地工具配置
                Map<String, Object> config = new HashMap<>();
                if (metadata.has("command")) {
                    config.put("command", metadata.get("command").asText());
                }
                if (metadata.has("args") && metadata.get("args").isArray()) {
                    config.put("args", objectMapper.convertValue(metadata.get("args"), List.class));
                }
                if (metadata.has("env") && metadata.get("env").isObject()) {
                    config.put("env", objectMapper.convertValue(metadata.get("env"), Map.class));
                }
                // 如果有 npm 包名，使用 npx 启动
                if (metadata.has("package") || metadata.has("npmPackage")) {
                    String packageName = metadata.has("package") ? metadata.get("package").asText() :
                        metadata.get("npmPackage").asText();
                    config.put("command", "npx");
                    config.put("args", List.of("-y", packageName));
                }
                localTool.setConfigJson(objectMapper.writeValueAsString(config));
            }

            localTool.setStatus(McpToolStatus.ENABLED.getValue());
            mcpToolMapper.insert(localTool);

            // 更新市场工具状态
            marketTool.setIsLoaded(true);
            marketTool.setLocalToolId(localTool.getId());
            mcpMarketToolMapper.updateById(marketTool);

            log.info("Successfully loaded tool {} to local", marketTool.getToolName());
        } catch (Exception e) {
            log.error("Failed to load tool to local: {}", e.getMessage());
            throw new ServiceException("加载工具到本地失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int batchLoadTools(List<Long> toolIds) {
        int successCount = 0;
        for (Long toolId : toolIds) {
            try {
                loadToolToLocal(toolId);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to load tool {}: {}", toolId, e.getMessage());
            }
        }
        return successCount;
    }

    private LambdaQueryWrapper<McpMarket> buildQueryWrapper(McpMarketBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<McpMarket> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(bo.getStatus()), McpMarket::getStatus, bo.getStatus())
            .like(StringUtils.hasText(bo.getName()), McpMarket::getName, bo.getName())
            .like(StringUtils.hasText(bo.getDescription()), McpMarket::getDescription, bo.getDescription());
        return wrapper;
    }
}
