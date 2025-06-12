package org.ruoyi.chat.controller.chat;

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
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.PromptTemplateBo;
import org.ruoyi.domain.vo.PromptTemplateVo;
import org.ruoyi.service.IPromptTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提示词模板
 *
 * @author evo
 * @date 2025-06-12
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/promptTemplate")
public class PromptTemplateController extends BaseController {

    private final IPromptTemplateService promptTemplateService;

    /**
     * 查询提示词模板列表
     */
    @SaCheckPermission("system:promptTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<PromptTemplateVo> list(PromptTemplateBo bo, PageQuery pageQuery) {
        return promptTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出提示词模板列表
     */
    @SaCheckPermission("system:promptTemplate:export")
    @Log(title = "提示词模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PromptTemplateBo bo, HttpServletResponse response) {
        List<PromptTemplateVo> list = promptTemplateService.queryList(bo);
        ExcelUtil.exportExcel(list, "提示词模板", PromptTemplateVo.class, response);
    }

    /**
     * 获取提示词模板详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:promptTemplate:query")
    @GetMapping("/{id}")
    public R<PromptTemplateVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(promptTemplateService.queryById(id));
    }

    /**
     * 新增提示词模板
     */
    @SaCheckPermission("system:promptTemplate:add")
    @Log(title = "提示词模板", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PromptTemplateBo bo) {
        return toAjax(promptTemplateService.insertByBo(bo));
    }

    /**
     * 修改提示词模板
     */
    @SaCheckPermission("system:promptTemplate:edit")
    @Log(title = "提示词模板", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PromptTemplateBo bo) {
        return toAjax(promptTemplateService.updateByBo(bo));
    }

    /**
     * 删除提示词模板
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:promptTemplate:remove")
    @Log(title = "提示词模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(promptTemplateService.deleteWithValidByIds(List.of(ids), true));
    }
}