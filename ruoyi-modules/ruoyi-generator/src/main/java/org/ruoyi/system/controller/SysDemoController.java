package org.ruoyi.system.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.system.domain.vo.SysDemoVo;
import org.ruoyi.system.domain.bo.SysDemoBo;
import org.ruoyi.system.service.SysDemoService;
import org.ruoyi.core.page.TableDataInfo;

/**
 * dome管理
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("dev/sysDemo")
public class SysDemoController extends BaseController {

    private final SysDemoService sysDemoService;

/**
 * 查询dome管理列表
 */
@SaCheckPermission("system:sysDemo:list")
@GetMapping("/list")
    public TableDataInfo<SysDemoVo> list(SysDemoBo bo, PageQuery pageQuery) {
        return sysDemoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出dome管理列表
     */
    @SaCheckPermission("system:sysDemo:export")
    @Log(title = "dome管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysDemoBo bo, HttpServletResponse response) {
        List<SysDemoVo> list = sysDemoService.queryList(bo);
        ExcelUtil.exportExcel(list, "dome管理", SysDemoVo.class, response);
    }

    /**
     * 获取dome管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:sysDemo:query")
    @GetMapping("/{id}")
    public R<SysDemoVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Integer id) {
        return R.ok(sysDemoService.queryById(id));
    }

    /**
     * 新增dome管理
     */
    @SaCheckPermission("system:sysDemo:add")
    @Log(title = "dome管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysDemoBo bo) {
        return toAjax(sysDemoService.insertByBo(bo));
    }

    /**
     * 修改dome管理
     */
    @SaCheckPermission("system:sysDemo:edit")
    @Log(title = "dome管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysDemoBo bo) {
        return toAjax(sysDemoService.updateByBo(bo));
    }

    /**
     * 删除dome管理
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:sysDemo:remove")
    @Log(title = "dome管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Integer[] ids) {
        return toAjax(sysDemoService.deleteWithValidByIds(List.of(ids), true));
    }
}
