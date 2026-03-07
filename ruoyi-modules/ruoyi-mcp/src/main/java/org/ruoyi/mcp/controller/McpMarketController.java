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
import org.ruoyi.mcp.domain.bo.McpMarketBo;
import org.ruoyi.mcp.domain.dto.McpMarketListResult;
import org.ruoyi.mcp.domain.dto.McpMarketToolListResult;
import org.ruoyi.mcp.domain.vo.McpMarketVo;
import org.ruoyi.mcp.service.IMcpMarketService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 市场管理 Controller
 *
 * @author ruoyi team
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mcp/market")
public class McpMarketController extends BaseController {

    private final IMcpMarketService mcpMarketService;

    /**
     * 查询市场列表
     */
    @SaCheckPermission("mcp:market:list")
    @GetMapping("/list")
    public TableDataInfo<McpMarketVo> list(McpMarketBo bo, PageQuery pageQuery) {
        return mcpMarketService.selectPageList(bo, pageQuery);
    }

    /**
     * 查询市场列表（不分页）
     */
    @SaCheckPermission("mcp:market:list")
    @GetMapping("/all")
    public McpMarketListResult listAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status) {
        return mcpMarketService.listMarkets(keyword, status);
    }

    /**
     * 导出 MCP 市场列表
     */
    @SaCheckPermission("mcp:market:export")
    @Log(title = "MCP市场管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(McpMarketBo bo, HttpServletResponse response) {
        List<McpMarketVo> list = mcpMarketService.queryList(bo);
        ExcelUtil.exportExcel(list, "MCP市场", McpMarketVo.class, response);
    }

    /**
     * 根据市场ID获取详细信息
     *
     * @param id 市场ID
     */
    @SaCheckPermission("mcp:market:query")
    @GetMapping("/{id}")
    public R<McpMarketVo> getInfo(@PathVariable Long id) {
        return R.ok(mcpMarketService.selectById(id));
    }

    /**
     * 新增市场
     */
    @SaCheckPermission("mcp:market:add")
    @Log(title = "MCP市场管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody McpMarketBo bo) {
        mcpMarketService.insert(bo);
        return R.ok();
    }

    /**
     * 修改市场
     */
    @SaCheckPermission("mcp:market:edit")
    @Log(title = "MCP市场管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody McpMarketBo bo) {
        mcpMarketService.update(bo);
        return R.ok();
    }

    /**
     * 删除市场
     *
     * @param ids 市场ID串
     */
    @SaCheckPermission("mcp:market:remove")
    @Log(title = "MCP市场管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        mcpMarketService.deleteByIds(List.of(ids));
        return R.ok();
    }

    /**
     * 更新市场状态
     */
    @SaCheckPermission("mcp:market:edit")
    @Log(title = "MCP市场管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        mcpMarketService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 获取市场工具列表（分页）
     */
    @SaCheckPermission("mcp:market:query")
    @GetMapping("/{marketId}/tools")
    public McpMarketToolListResult getMarketTools(
        @PathVariable Long marketId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        return mcpMarketService.getMarketTools(marketId, page, size);
    }

    /**
     * 刷新市场工具列表
     */
    @SaCheckPermission("mcp:market:edit")
    @Log(title = "MCP市场管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{marketId}/refresh")
    public R<org.ruoyi.mcp.domain.dto.McpMarketRefreshResult> refreshMarketTools(@PathVariable Long marketId) {
        return R.ok(mcpMarketService.refreshMarketTools(marketId));
    }

    /**
     * 加载单个工具到本地
     */
    @SaCheckPermission("mcp:market:add")
    @Log(title = "MCP市场管理", businessType = BusinessType.INSERT)
    @PostMapping("/tools/{toolId}/load")
    public R<Void> loadToolToLocal(@PathVariable Long toolId) {
        mcpMarketService.loadToolToLocal(toolId);
        return R.ok();
    }

    /**
     * 批量加载工具到本地
     */
    @SaCheckPermission("mcp:market:add")
    @Log(title = "MCP市场管理", businessType = BusinessType.INSERT)
    @PostMapping("/tools/batch-load")
    public R<Map<String, Object>> batchLoadTools(@RequestBody List<Long> toolIds) {
        int successCount = mcpMarketService.batchLoadTools(toolIds);
        return R.ok(Map.of("successCount", successCount));
    }
}
