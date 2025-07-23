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
import org.ruoyi.generator.domain.bo.SchemaBo;
import org.ruoyi.generator.domain.vo.SchemaVo;
import org.ruoyi.generator.service.SchemaService;
import org.ruoyi.helper.DataBaseHelper;
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
 * 数据模型
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dev/schema")
public class SchemaController extends BaseController {

    private final SchemaService schemaService;

    /**
     * 查询数据模型列表
     */
    @SaCheckPermission("dev:schema:list")
    @GetMapping("/list")
    public TableDataInfo<SchemaVo> list(SchemaBo bo, PageQuery pageQuery) {
        return schemaService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取数据模型详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("dev:schema:query")
    @GetMapping("/{id}")
    public R<SchemaVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(schemaService.queryById(id));
    }

    /**
     * 新增数据模型
     */
    @SaCheckPermission("dev:schema:add")
    @Log(title = "数据模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SchemaBo bo) {
        return toAjax(schemaService.insertByBo(bo));
    }

    /**
     * 修改数据模型
     */
    @SaCheckPermission("dev:schema:edit")
    @Log(title = "数据模型", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SchemaBo bo) {
        return toAjax(schemaService.updateByBo(bo));
    }

    /**
     * 删除数据模型
     *
     * @param ids 主键串
     */
    @SaCheckPermission("dev:schema:remove")
    @Log(title = "数据模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(schemaService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询数据源表名
     */
    @SaCheckPermission("dev:schema:getTableNameList")
    @GetMapping(value = "/getDataNames")
    public R<Object> getCurrentDataSourceTableNameList() {
        return R.ok(DataBaseHelper.getCurrentDataSourceTableNameList());
    }
}