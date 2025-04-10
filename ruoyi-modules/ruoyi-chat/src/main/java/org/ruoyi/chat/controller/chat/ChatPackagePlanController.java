package org.ruoyi.chat.controller.chat;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatPackagePlanBo;
import org.ruoyi.domain.vo.ChatPackagePlanVo;
import org.ruoyi.service.IChatPackagePlanService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;

/**
 * 套餐管理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/packagePlan")
public class ChatPackagePlanController extends BaseController {

    private final IChatPackagePlanService chatPackagePlanService;

    /**
     * 查询套餐管理列表
     */
    @SaCheckPermission("system:packagePlan:list")
    @GetMapping("/list")
    public TableDataInfo<ChatPackagePlanVo> list(ChatPackagePlanBo bo, PageQuery pageQuery) {
        return chatPackagePlanService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出套餐管理列表
     */
    @SaCheckPermission("system:packagePlan:export")
    @Log(title = "套餐管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ChatPackagePlanBo bo, HttpServletResponse response) {
        List<ChatPackagePlanVo> list = chatPackagePlanService.queryList(bo);
        ExcelUtil.exportExcel(list, "套餐管理", ChatPackagePlanVo.class, response);
    }

    /**
     * 获取套餐管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:packagePlan:query")
    @GetMapping("/{id}")
    public R<ChatPackagePlanVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(chatPackagePlanService.queryById(id));
    }

    /**
     * 新增套餐管理
     */
    @SaCheckPermission("system:packagePlan:add")
    @Log(title = "套餐管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ChatPackagePlanBo bo) {
        return toAjax(chatPackagePlanService.insertByBo(bo));
    }

    /**
     * 修改套餐管理
     */
    @SaCheckPermission("system:packagePlan:edit")
    @Log(title = "套餐管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatPackagePlanBo bo) {
        return toAjax(chatPackagePlanService.updateByBo(bo));
    }

    /**
     * 删除套餐管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:packagePlan:remove")
    @Log(title = "套餐管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatPackagePlanService.deleteWithValidByIds(List.of(ids), true));
    }
}
