package com.xmzs.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.validate.AddGroup;
import com.xmzs.common.core.validate.EditGroup;
import com.xmzs.common.excel.utils.ExcelUtil;
import com.xmzs.common.idempotent.annotation.RepeatSubmit;
import com.xmzs.common.log.annotation.Log;
import com.xmzs.common.log.enums.BusinessType;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.web.core.BaseController;
import com.xmzs.system.domain.bo.SysModelBo;
import com.xmzs.system.domain.vo.SysModelVo;
import com.xmzs.system.service.ISysModelService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统模型
 *
 * @author Lion Li
 * @date 2024-04-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/model")
public class SysModelController extends BaseController {

    private final ISysModelService sysModelService;

    /**
     * 查询系统模型列表
     */
    @GetMapping("/list")
    public TableDataInfo<SysModelVo> list(SysModelBo bo, PageQuery pageQuery) {
        return sysModelService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询系统模型列表
     */
    @GetMapping("/modelList")
    public R<List<SysModelVo>> modelList(SysModelBo bo) {
        bo.setModelShow("0");
        return R.ok(sysModelService.queryList(bo));
    }

    /**
     * 导出系统模型列表
     */
    @SaCheckPermission("system:model:export")
    @Log(title = "系统模型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysModelBo bo, HttpServletResponse response) {
        List<SysModelVo> list = sysModelService.queryList(bo);
        ExcelUtil.exportExcel(list, "系统模型", SysModelVo.class, response);
    }

    /**
     * 获取系统模型详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:model:query")
    @GetMapping("/{id}")
    public R<SysModelVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(sysModelService.queryById(id));
    }

    /**
     * 新增系统模型
     */
    @SaCheckPermission("system:model:add")
    @Log(title = "系统模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysModelBo bo) {
        return toAjax(sysModelService.insertByBo(bo));
    }

    /**
     * 修改系统模型
     */
    @SaCheckPermission("system:model:edit")
    @Log(title = "系统模型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysModelBo bo) {
        return toAjax(sysModelService.updateByBo(bo));
    }

    /**
     * 删除系统模型
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:model:remove")
    @Log(title = "系统模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysModelService.deleteWithValidByIds(List.of(ids), true));
    }
}
