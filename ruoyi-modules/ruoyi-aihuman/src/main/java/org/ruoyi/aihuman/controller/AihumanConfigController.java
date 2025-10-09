package org.ruoyi.aihuman.controller;

import java.util.List;

import cn.dev33.satoken.annotation.SaIgnore;
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
import org.ruoyi.aihuman.domain.vo.AihumanConfigVo;
import org.ruoyi.aihuman.domain.bo.AihumanConfigBo;
import org.ruoyi.aihuman.service.AihumanConfigService;
import org.ruoyi.core.page.TableDataInfo;

/**
 * 交互数字人配置
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */

//临时免登录
@SaIgnore

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/aihuman/aihumanConfig")
public class AihumanConfigController extends BaseController {

    private final AihumanConfigService aihumanConfigService;

/**
 * 查询交互数字人配置列表
 */
@SaCheckPermission("aihuman:aihumanConfig:list")
@GetMapping("/list")
    public TableDataInfo<AihumanConfigVo> list(AihumanConfigBo bo, PageQuery pageQuery) {
        return aihumanConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出交互数字人配置列表
     */
    @SaCheckPermission("aihuman:aihumanConfig:export")
    @Log(title = "交互数字人配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(AihumanConfigBo bo, HttpServletResponse response) {
        List<AihumanConfigVo> list = aihumanConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "交互数字人配置", AihumanConfigVo.class, response);
    }

    /**
     * 获取交互数字人配置详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("aihuman:aihumanConfig:query")
    @GetMapping("/{id}")
    public R<AihumanConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Integer id) {
        return R.ok(aihumanConfigService.queryById(id));
    }

    /**
     * 新增交互数字人配置
     */
    @SaCheckPermission("aihuman:aihumanConfig:add")
    @Log(title = "交互数字人配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AihumanConfigBo bo) {
        return toAjax(aihumanConfigService.insertByBo(bo));
    }

    /**
     * 修改交互数字人配置
     */
    @SaCheckPermission("aihuman:aihumanConfig:edit")
    @Log(title = "交互数字人配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AihumanConfigBo bo) {
        return toAjax(aihumanConfigService.updateByBo(bo));
    }

    /**
     * 删除交互数字人配置
     *
     * @param ids 主键串
     */
    @SaCheckPermission("aihuman:aihumanConfig:remove")
    @Log(title = "交互数字人配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Integer[] ids) {
        return toAjax(aihumanConfigService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询已发布的交互数字人配置列表
     * 只返回 publish = 1 的数据
     */
    @GetMapping("/publishedList")
    public TableDataInfo<AihumanConfigVo> publishedList(PageQuery pageQuery) {
        // 创建查询条件对象并设置publish=1
        AihumanConfigBo bo = new AihumanConfigBo();
        bo.setPublish(1);
        // 调用现有的查询方法，传入预设了publish=1条件的bo对象
        return aihumanConfigService.queryPageList(bo, pageQuery);
    }


}
