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
import org.ruoyi.system.domain.vo.StoreEmployeeVo;
import org.ruoyi.system.domain.bo.StoreEmployeeBo;
import org.ruoyi.system.service.StoreEmployeeService;
import org.ruoyi.core.page.TableDataInfo;

/**
 * 员工分配
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/store/storeEmployee")
public class StoreEmployeeController extends BaseController {

    private final StoreEmployeeService storeEmployeeService;

/**
 * 查询员工分配列表
 */
@SaCheckPermission("store:storeEmployee:list")
@GetMapping("/list")
    public TableDataInfo<StoreEmployeeVo> list(StoreEmployeeBo bo, PageQuery pageQuery) {
        return storeEmployeeService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出员工分配列表
     */
    @SaCheckPermission("store:storeEmployee:export")
    @Log(title = "员工分配", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StoreEmployeeBo bo, HttpServletResponse response) {
        List<StoreEmployeeVo> list = storeEmployeeService.queryList(bo);
        ExcelUtil.exportExcel(list, "员工分配", StoreEmployeeVo.class, response);
    }

    /**
     * 获取员工分配详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("store:storeEmployee:query")
    @GetMapping("/{id}")
    public R<StoreEmployeeVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(storeEmployeeService.queryById(id));
    }

    /**
     * 新增员工分配
     */
    @SaCheckPermission("store:storeEmployee:add")
    @Log(title = "员工分配", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody StoreEmployeeBo bo) {
        return toAjax(storeEmployeeService.insertByBo(bo));
    }

    /**
     * 修改员工分配
     */
    @SaCheckPermission("store:storeEmployee:edit")
    @Log(title = "员工分配", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StoreEmployeeBo bo) {
        return toAjax(storeEmployeeService.updateByBo(bo));
    }

    /**
     * 删除员工分配
     *
     * @param ids 主键串
     */
    @SaCheckPermission("store:storeEmployee:remove")
    @Log(title = "员工分配", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(storeEmployeeService.deleteWithValidByIds(List.of(ids), true));
    }
}
