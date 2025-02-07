package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
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
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.bo.SysPackagePlanBo;
import org.ruoyi.system.domain.vo.SysModelVo;
import org.ruoyi.system.domain.vo.SysPackagePlanVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.service.ISysModelService;
import org.ruoyi.system.service.ISysPackagePlanService;
import org.ruoyi.system.service.ISysUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
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

    private final ISysPackagePlanService sysPackagePlanService;

    private final ISysUserService userService;


    /**
     * 查询系统模型列表 - 全部
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
        List<SysModelVo> sysModelVos = sysModelService.queryList(bo);
        SysPackagePlanBo sysPackagePlanBo = new SysPackagePlanBo();
        if (StpUtil.isLogin()) {
            Long userId = LoginHelper.getLoginUser().getUserId();
            SysUserVo sysUserVo = userService.selectUserById(userId);
            if ("0".equals(sysUserVo.getUserGrade())){
                sysPackagePlanBo.setName("Free");
                SysPackagePlanVo sysPackagePlanVo = sysPackagePlanService.queryList(sysPackagePlanBo).get(0);
                List<String> array = new ArrayList<>(Arrays.asList(sysPackagePlanVo.getPlanDetail().split(",")));
                sysModelVos.removeIf(model -> !array.contains(model.getModelName()));
            }
        }else {
            sysPackagePlanBo.setName("Visitor");
            SysPackagePlanVo sysPackagePlanVo = sysPackagePlanService.queryList(sysPackagePlanBo).get(0);
            List<String> array = new ArrayList<>(Arrays.asList(sysPackagePlanVo.getPlanDetail().split(",")));
            sysModelVos.removeIf(model -> !array.contains(model.getModelName()));
        }
        return R.ok(sysModelVos);
    }

    /**
     * 导出系统模型列表
     */
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
    @Log(title = "系统模型", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysModelBo bo) {
        return toAjax(sysModelService.insertByBo(bo));
    }

    /**
     * 修改系统模型
     */
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
