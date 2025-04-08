package org.ruoyi.system.controller.system;

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
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 访客管理
 *
 * @author Lion Li
 * @date 2024-07-14
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/visitorUsage")
public class ChatVisitorUsageController extends BaseController {

    private final IChatVisitorUsageService chatVisitorUsageService;

    /**
     * 查询访客管理列表
     */
    @SaCheckPermission("system:visitorUsage:list")
    @GetMapping("/list")
    public TableDataInfo<ChatVisitorUsageVo> list(ChatVisitorUsageBo bo, PageQuery pageQuery) {
        return chatVisitorUsageService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出访客管理列表
     */
    @SaCheckPermission("system:visitorUsage:export")
    @Log(title = "访客管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatVisitorUsageBo bo, HttpServletResponse response) {
        List<ChatVisitorUsageVo> list = chatVisitorUsageService.queryList(bo);
        ExcelUtil.exportExcel(list, "访客管理", ChatVisitorUsageVo.class, response);
    }

    /**
     * 获取访客管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:visitorUsage:query")
    @GetMapping("/{id}")
    public R<ChatVisitorUsageVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatVisitorUsageService.queryById(id));
    }

    /**
     * 新增访客管理
     */
    @SaCheckPermission("system:visitorUsage:add")
    @Log(title = "访客管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatVisitorUsageBo bo) {
        return toAjax(chatVisitorUsageService.insertByBo(bo));
    }

    /**
     * 修改访客管理
     */
    @SaCheckPermission("system:visitorUsage:edit")
    @Log(title = "访客管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatVisitorUsageBo bo) {
        return toAjax(chatVisitorUsageService.updateByBo(bo));
    }

    /**
     * 删除访客管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:visitorUsage:remove")
    @Log(title = "访客管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatVisitorUsageService.deleteWithValidByIds(List.of(ids), true));
    }
}
