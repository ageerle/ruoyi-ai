package org.ruoyi.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.generator.domain.bo.SchemaGroupBo;
import org.ruoyi.generator.domain.vo.SchemaGroupVo;
import org.ruoyi.generator.service.SchemaGroupService;
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
 * 数据模型分组
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dev/schemaGroup")
public class SchemaGroupController extends BaseController {

    private final SchemaGroupService schemaGroupService;

    /**
     * 查询数据模型分组列表
     */
    @SaCheckPermission("dev:schemaGroup:list")
    @GetMapping("/list")
    public TableDataInfo<SchemaGroupVo> list(SchemaGroupBo bo, PageQuery pageQuery) {
        return schemaGroupService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取数据模型分组选择列表
     */
    @SaCheckPermission("dev:schemaGroup:select")
    @GetMapping("/select")
    public R<List<SchemaGroupVo>> select() {
        SchemaGroupBo bo = new SchemaGroupBo();
        List<SchemaGroupVo> list = schemaGroupService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 获取数据模型分组详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("dev:schemaGroup:query")
    @GetMapping("/{id}")
    public R<SchemaGroupVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(schemaGroupService.queryById(id));
    }

    /**
     * 新增数据模型分组
     */
    @SaCheckPermission("dev:schemaGroup:add")
    @Log(title = "数据模型分组", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SchemaGroupBo bo) {
        return toAjax(schemaGroupService.insertByBo(bo));
    }

    /**
     * 修改数据模型分组
     */
    @SaCheckPermission("dev:schemaGroup:edit")
    @Log(title = "数据模型分组", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SchemaGroupBo bo) {
        return toAjax(schemaGroupService.updateByBo(bo));
    }

    /**
     * 删除数据模型分组
     *
     * @param ids 主键串
     */
    @SaCheckPermission("dev:schemaGroup:remove")
    @Log(title = "数据模型分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(schemaGroupService.deleteWithValidByIds(List.of(ids), true));
    }

}