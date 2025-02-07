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
import org.ruoyi.system.domain.vo.SysUserModelVo;
import org.ruoyi.system.domain.bo.SysUserModelBo;
import org.ruoyi.system.service.ISysUserModelService;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 【请填写功能名称】
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/userModel")
public class SysUserModelController extends BaseController {

    private final ISysUserModelService sysUserModelService;

    /**
     * 查询【请填写功能名称】列表
     */
    @SaCheckPermission("system:userModel:list")
    @GetMapping("/list")
    public TableDataInfo<SysUserModelVo> list(SysUserModelBo bo, PageQuery pageQuery) {
        return sysUserModelService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @SaCheckPermission("system:userModel:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysUserModelBo bo, HttpServletResponse response) {
        List<SysUserModelVo> list = sysUserModelService.queryList(bo);
        ExcelUtil.exportExcel(list, "【请填写功能名称】", SysUserModelVo.class, response);
    }

    /**
     * 获取【请填写功能名称】详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:userModel:query")
    @GetMapping("/{id}")
    public R<SysUserModelVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(sysUserModelService.queryById(id));
    }

    /**
     * 新增【请填写功能名称】
     */
    @SaCheckPermission("system:userModel:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysUserModelBo bo) {
        return toAjax(sysUserModelService.insertByBo(bo));
    }

    /**
     * 修改【请填写功能名称】
     */
    @SaCheckPermission("system:userModel:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysUserModelBo bo) {
        return toAjax(sysUserModelService.updateByBo(bo));
    }

    /**
     * 删除【请填写功能名称】
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:userModel:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysUserModelService.deleteWithValidByIds(List.of(ids), true));
    }
}
