package org.ruoyi.mcp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.McpInfo;
import org.ruoyi.domain.bo.McpInfoBo;
import org.ruoyi.domain.vo.McpInfoVo;
import org.ruoyi.mcp.config.McpServerConfig;
import org.ruoyi.mcp.domain.McpInfoRequest;
import org.ruoyi.mcp.service.McpInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP
 *
 * @author ageerle
 * @date Sat Aug 09 16:50:58 CST 2025
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/operator/mcpInfo")
public class McpInfoController extends BaseController {

    private final McpInfoService mcpInfoService;

    /**
     * 查询MCP列表
     */
    @SaCheckPermission("operator:mcpInfo:list")
    @GetMapping("/list")
    public TableDataInfo<McpInfoVo> list(McpInfoBo bo, PageQuery pageQuery) {
        return mcpInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出MCP列表
     */
    @SaCheckPermission("operator:mcpInfo:export")
    @Log(title = "MCP", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(McpInfoBo bo, HttpServletResponse response) {
        List<McpInfoVo> list = mcpInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "MCP", McpInfoVo.class, response);
    }

    /**
     * 获取MCP详细信息
     *
     * @param mcpId 主键
     */
    @SaCheckPermission("operator:mcpInfo:query")
    @GetMapping("/{mcpId}")
    public R<McpInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                @PathVariable Integer mcpId) {
        return R.ok(mcpInfoService.queryById(mcpId));
    }

    /**
     * 新增MCP
     */
    @SaCheckPermission("operator:mcpInfo:add")
    @Log(title = "MCP", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody McpInfoBo bo) {
        return toAjax(mcpInfoService.insertByBo(bo));
    }

    /**
     * 修改MCP
     */
    @SaCheckPermission("operator:mcpInfo:edit")
    @Log(title = "MCP", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody McpInfoBo bo) {
        return toAjax(mcpInfoService.updateByBo(bo));
    }

    /**
     * 删除MCP
     *
     * @param mcpIds 主键串
     */
    @SaCheckPermission("operator:mcpInfo:remove")
    @Log(title = "MCP", businessType = BusinessType.DELETE)
    @DeleteMapping("/{mcpIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Integer[] mcpIds) {
        return toAjax(mcpInfoService.deleteWithValidByIds(List.of(mcpIds), true));
    }

    /**
     * 添加或更新 MCP 工具
     */
    @PostMapping("/tools")
    public R<McpInfo> saveToolConfig(@RequestBody McpInfoRequest request) {
        return R.ok(mcpInfoService.saveToolConfig(request));
    }

    /**
     * 获取所有活跃服务器名称
     */
    @GetMapping("/tools/names")
    public R<List<String>> getActiveServerNames() {
        return R.ok(mcpInfoService.getActiveServerNames());
    }

    /**
     * 根据名称获取工具配置
     */
    @GetMapping("/tools/{serverName}")
    public R<McpServerConfig> getToolConfig(@PathVariable String serverName) {
        return R.ok(mcpInfoService.getToolConfigByName(serverName));
    }

    /**
     * 启用工具
     */
    @PostMapping("/tools/{serverName}/enable")
    public Map<String, Object> enableTool(@PathVariable String serverName) {
        boolean success = mcpInfoService.enableTool(serverName);
        return Map.of("success", success);
    }

    /**
     * 禁用工具
     */
    @PostMapping("/tools/{serverName}/disable")
    public Map<String, Object> disableTool(@PathVariable String serverName) {
        boolean success = mcpInfoService.disableTool(serverName);
        return Map.of("success", success);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/tools/{serverName}")
    public Map<String, Object> deleteTool(@PathVariable String serverName) {
        boolean success = mcpInfoService.deleteToolConfig(serverName);
        return Map.of("success", success);
    }
}
