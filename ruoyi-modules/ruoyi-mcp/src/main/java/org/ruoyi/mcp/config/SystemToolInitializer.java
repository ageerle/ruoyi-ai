package org.ruoyi.mcp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.mcp.domain.entity.McpTool;
import org.ruoyi.mcp.enums.McpToolStatus;
import org.ruoyi.mcp.mapper.McpToolMapper;
import org.ruoyi.mcp.service.core.BuiltinToolDefinition;
import org.ruoyi.mcp.service.core.BuiltinToolRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统工具初始化器
 * 在应用启动时，将系统内置工具同步到数据库
 * 这样可以统一管理所有工具，支持动态启用/禁用
 *
 * @author ruoyi team
 */
@Slf4j
@Component
@Order(999) // 确保在其他初始化器之后执行
@RequiredArgsConstructor
public class SystemToolInitializer implements ApplicationRunner {

    private final McpToolMapper mcpToolMapper;
    private final BuiltinToolRegistry builtinToolRegistry;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("开始同步系统内置工具到数据库...");

        int addedCount = 0;
        int existingCount = 0;

        for (BuiltinToolDefinition tool : builtinToolRegistry.getAllBuiltinTools()) {
            try {
                boolean added = syncBuiltinTool(tool);
                if (added) {
                    addedCount++;
                } else {
                    existingCount++;
                }
            } catch (Exception e) {
                log.error("同步内置工具失败: {}", tool.name(), e);
            }
        }

        log.info("系统内置工具同步完成: 新增 {} 个, 已存在 {} 个", addedCount, existingCount);
    }

    /**
     * 同步单个内置工具到数据库
     *
     * @param tool 工具定义
     * @return 是否新增（true=新增, false=已存在）
     */
    private boolean syncBuiltinTool(BuiltinToolDefinition tool) {
        // 检查是否已存在
        LambdaQueryWrapper<McpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpTool::getName, tool.name())
            .eq(McpTool::getType, BuiltinToolRegistry.TYPE_BUILTIN);

        McpTool existing = mcpToolMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在，更新描述信息（保留状态不变）
            if (!tool.description().equals(existing.getDescription())) {
                existing.setDescription(tool.description());
                mcpToolMapper.updateById(existing);
                log.debug("更新内置工具描述: {}", tool.name());
            }
            return false;
        }

        // 新增
        McpTool newTool = new McpTool();
        newTool.setName(tool.name());
        newTool.setDescription(tool.description());
        newTool.setType(BuiltinToolRegistry.TYPE_BUILTIN);
        newTool.setStatus(McpToolStatus.ENABLED.getValue()); // 默认启用
        newTool.setConfigJson(null);  // 内置工具不需要配置
        mcpToolMapper.insert(newTool);

        log.info("新增内置工具: {} ({})", tool.name(), tool.displayName());
        return true;
    }
}
