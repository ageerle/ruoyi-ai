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
import org.ruoyi.system.domain.bo.SysUserGroupBo;
import org.ruoyi.system.domain.vo.SysUserGroupVo;
import org.ruoyi.system.service.ISysUserGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【请填写功能名称】
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/userGroup")
public class SysUserGroupController extends BaseController {

    private final ISysUserGroupService sysUserGroupService;

    /**
     * 查询【请填写功能名称】列表
     */
    @SaCheckPermission("system:userGroup:list")
    @GetMapping("/list")
    public TableDataInfo<SysUserGroupVo> list(SysUserGroupBo bo, PageQuery pageQuery) {
        return sysUserGroupService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @SaCheckPermission("system:userGroup:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysUserGroupBo bo, HttpServletResponse response) {
        List<SysUserGroupVo> list = sysUserGroupService.queryList(bo);
        ExcelUtil.exportExcel(list, "【请填写功能名称】", SysUserGroupVo.class, response);
    }

    /**
     * 获取【请填写功能名称】详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:userGroup:query")
    @GetMapping("/{id}")
    public R<SysUserGroupVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(sysUserGroupService.queryById(id));
    }

    /**
     * 新增【请填写功能名称】
     */
    @SaCheckPermission("system:userGroup:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysUserGroupBo bo) {
        return toAjax(sysUserGroupService.insertByBo(bo));
    }

    /**
     * 修改【请填写功能名称】
     */
    @SaCheckPermission("system:userGroup:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysUserGroupBo bo) {
        return toAjax(sysUserGroupService.updateByBo(bo));
    }

    /**
     * 删除【请填写功能名称】
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:userGroup:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysUserGroupService.deleteWithValidByIds(List.of(ids), true));
    }
}
