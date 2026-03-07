package org.ruoyi.mcp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.mcp.domain.bo.McpToolBo;
import org.ruoyi.mcp.domain.dto.McpToolListResult;
import org.ruoyi.mcp.domain.dto.McpToolTestResult;
import org.ruoyi.mcp.domain.vo.McpToolVo;
import org.ruoyi.mcp.service.IMcpToolService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP 工具管理 Controller
 *
 * @author ruoyi team
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mcp/tool")
public class McpToolController extends BaseController {

    private final IMcpToolService mcpToolService;

    /**
     * 查询 MCP 工具列表
     */
    @SaCheckPermission("mcp:tool:list")
    @GetMapping("/list")
    public TableDataInfo<McpToolVo> list(McpToolBo bo, PageQuery pageQuery) {
        return mcpToolService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询 MCP 工具列表（不分页）
     */
    @SaCheckPermission("mcp:tool:list")
    @GetMapping("/all")
    public McpToolListResult listAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status) {
        return mcpToolService.listTools(keyword, type, status);
    }

    /**
     * 导出 MCP 工具列表
     */
    @SaCheckPermission("mcp:tool:export")
    @Log(title = "MCP工具管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(McpToolBo bo, HttpServletResponse response) {
        List<McpToolVo> list = mcpToolService.queryList(bo);
        ExcelUtil.exportExcel(list, "MCP工具", McpToolVo.class, response);
    }

    /**
     * 根据工具ID获取详细信息
     *
     * @param id 工具ID
     */
    @SaCheckPermission("mcp:tool:query")
    @GetMapping("/{id}")
    public R<McpToolVo> getInfo(@PathVariable Long id) {
        return R.ok(mcpToolService.selectById(id));
    }

    /**
     * 新增 MCP 工具
     */
    @SaCheckPermission("mcp:tool:add")
    @Log(title = "MCP工具管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody McpToolBo bo) {
        mcpToolService.insert(bo);
        return R.ok();
    }

    /**
     * 修改 MCP 工具
     */
    @SaCheckPermission("mcp:tool:edit")
    @Log(title = "MCP工具管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody McpToolBo bo) {
        mcpToolService.update(bo);
        return R.ok();
    }

    /**
     * 删除 MCP 工具
     *
     * @param ids 工具ID串
     */
    @SaCheckPermission("mcp:tool:remove")
    @Log(title = "MCP工具管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        mcpToolService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 更新工具状态
     */
    @SaCheckPermission("mcp:tool:edit")
    @Log(title = "MCP工具管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        mcpToolService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 测试工具连接
     */
    @SaCheckPermission("mcp:tool:query")
    @PostMapping("/{id}/test")
    public R<McpToolTestResult> testTool(@PathVariable Long id) {
        return R.ok(mcpToolService.testTool(id));
    }
}
