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
import org.ruoyi.generator.domain.bo.SchemaFieldBo;
import org.ruoyi.generator.domain.vo.SchemaFieldVo;
import org.ruoyi.generator.service.SchemaFieldService;
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
 * 数据模型字段
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dev/schemaField")
public class SchemaFieldController extends BaseController {

    private final SchemaFieldService schemaFieldService;

    /**
     * 查询数据模型字段列表
     */
    @SaCheckPermission("dev:schemaField:list")
    @GetMapping("/list")
    public TableDataInfo<SchemaFieldVo> list(SchemaFieldBo bo, PageQuery pageQuery) {
        return schemaFieldService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取数据模型字段详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("dev:schemaField:query")
    @GetMapping("/{id}")
    public R<SchemaFieldVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable Long id) {
        return R.ok(schemaFieldService.queryById(id));
    }

    /**
     * 新增数据模型字段
     */
    @SaCheckPermission("dev:schemaField:add")
    @Log(title = "数据模型字段", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SchemaFieldBo bo) {
        return toAjax(schemaFieldService.insertByBo(bo));
    }

    /**
     * 修改数据模型字段
     */
    @SaCheckPermission("dev:schemaField:edit")
    @Log(title = "数据模型字段", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SchemaFieldBo bo) {
        return toAjax(schemaFieldService.updateByBo(bo));
    }

    /**
     * 删除数据模型字段
     *
     * @param ids 主键串
     */
    @SaCheckPermission("dev:schemaField:remove")
    @Log(title = "数据模型字段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(schemaFieldService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 批量更新字段配置
     *
     * @param fields 字段配置列表
     */
    @SaCheckPermission("dev:schemaField:edit")
    @Log(title = "批量更新字段配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/batchUpdate")
    public R<Void> batchUpdateFieldConfig(@Validated(EditGroup.class) @RequestBody List<SchemaFieldBo> fields) {
        return toAjax(schemaFieldService.batchUpdateFieldConfig(fields));
    }

    /**
     * 根据模型ID查询字段列表
     *
     * @param schemaId 模型ID
     */
    @SaCheckPermission("dev:schemaField:list")
    @GetMapping("/listBySchemaId/{schemaId}")
    public R<List<SchemaFieldVo>> listBySchemaId(@NotNull(message = "模型ID不能为空") @PathVariable Long schemaId) {
        return R.ok(schemaFieldService.queryListBySchemaId(schemaId));
    }
}