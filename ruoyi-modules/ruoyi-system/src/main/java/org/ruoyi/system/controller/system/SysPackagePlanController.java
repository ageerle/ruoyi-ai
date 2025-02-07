package org.ruoyi.system.controller.system;

import java.util.List;

import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.log.enums.BusinessType;

import org.ruoyi.system.domain.vo.SysPackagePlanVo;
import org.ruoyi.system.domain.bo.SysPackagePlanBo;
import org.ruoyi.system.service.ISysPackagePlanService;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 套餐管理
 *
 * @author Lion Li
 * @date 2024-05-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/packagePlan")
public class SysPackagePlanController extends BaseController {

    private final ISysPackagePlanService sysPackagePlanService;

    /**
     * 查询套餐管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<SysPackagePlanVo> list(SysPackagePlanBo bo, PageQuery pageQuery) {
        return sysPackagePlanService.queryPageList(bo, pageQuery);
    }

    @GetMapping("/listPlan")
    public R<List<SysPackagePlanVo>> listPlan() {
        return R.ok(sysPackagePlanService.queryList(new SysPackagePlanBo()));
    }

    /**
     * 导出套餐管理列表
     */
    @SaCheckPermission("system:packagePlan:export")
    @Log(title = "套餐管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysPackagePlanBo bo, HttpServletResponse response) {
        List<SysPackagePlanVo> list = sysPackagePlanService.queryList(bo);
        ExcelUtil.exportExcel(list, "套餐管理", SysPackagePlanVo.class, response);
    }

    /**
     * 获取套餐管理详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:packagePlan:query")
    @GetMapping("/{id}")
    public R<SysPackagePlanVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(sysPackagePlanService.queryById(id));
    }




    /**
     * 新增套餐管理
     */
    @SaCheckPermission("system:packagePlan:add")
    @Log(title = "套餐管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysPackagePlanBo bo) {
        return toAjax(sysPackagePlanService.insertByBo(bo));
    }

    /**
     * 修改套餐管理
     */
    @SaCheckPermission("system:packagePlan:edit")
    @Log(title = "套餐管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysPackagePlanBo bo) {
        return toAjax(sysPackagePlanService.updateByBo(bo));
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
        return toAjax(sysPackagePlanService.deleteWithValidByIds(List.of(ids), true));
    }
}
