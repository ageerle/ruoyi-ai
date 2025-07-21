package org.ruoyi.generator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.generator.domain.SchemaField;
import org.ruoyi.generator.domain.bo.SchemaFieldBo;
import org.ruoyi.generator.domain.vo.SchemaFieldVo;
import org.ruoyi.generator.domain.vo.SchemaGroupVo;
import org.ruoyi.generator.domain.vo.SchemaVo;
import org.ruoyi.generator.mapper.SchemaFieldMapper;
import org.ruoyi.generator.service.ISchemaFieldService;
import org.ruoyi.generator.service.ISchemaGroupService;
import org.ruoyi.generator.service.ISchemaService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据模型字段Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SchemaFieldServiceImpl implements ISchemaFieldService {

    private final SchemaFieldMapper baseMapper;
    private final ISchemaService schemaService;
    private final ISchemaGroupService schemaGroupService;

    /**
     * 查询数据模型字段
     */
    @Override
    public SchemaFieldVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询数据模型字段列表
     */
    @Override
    public TableDataInfo<SchemaFieldVo> queryPageList(SchemaFieldBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SchemaField> lqw = buildQueryWrapper(bo);
        Page<SchemaFieldVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    private LambdaQueryWrapper<SchemaField> buildQueryWrapper(SchemaFieldBo bo) {

        LambdaQueryWrapper<SchemaField> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getSchemaId() != null, SchemaField::getSchemaId, bo.getSchemaId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), SchemaField::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), SchemaField::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), SchemaField::getType, bo.getType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsPk()), SchemaField::getIsPk, bo.getIsPk());
        lqw.eq(StringUtils.isNotBlank(bo.getIsRequired()), SchemaField::getIsRequired, bo.getIsRequired());
        lqw.eq(StringUtils.isNotBlank(bo.getIsList()), SchemaField::getIsList, bo.getIsList());
        lqw.eq(StringUtils.isNotBlank(bo.getIsQuery()), SchemaField::getIsQuery, bo.getIsQuery());
        lqw.eq(StringUtils.isNotBlank(bo.getIsInsert()), SchemaField::getIsInsert, bo.getIsInsert());
        lqw.eq(StringUtils.isNotBlank(bo.getIsEdit()), SchemaField::getIsEdit, bo.getIsEdit());
        lqw.eq(StringUtils.isNotBlank(bo.getQueryType()), SchemaField::getQueryType, bo.getQueryType());
        lqw.eq(StringUtils.isNotBlank(bo.getHtmlType()), SchemaField::getHtmlType, bo.getHtmlType());
        lqw.like(StringUtils.isNotBlank(bo.getDictType()), SchemaField::getDictType, bo.getDictType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SchemaField::getStatus, bo.getStatus());
        lqw.orderByAsc(SchemaField::getSort);
        return lqw;
    }

    /**
     * 新增数据模型字段
     */
    @Override
    public Boolean insertByBo(SchemaFieldBo bo) {
        SchemaField add = MapstructUtils.convert(bo, SchemaField.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改数据模型字段
     */
    @Override
    public Boolean updateByBo(SchemaFieldBo bo) {
        SchemaField update = MapstructUtils.convert(bo, SchemaField.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SchemaField entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除数据模型字段
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 批量更新字段配置
     */
    @Override
    public Boolean batchUpdateFieldConfig(List<SchemaFieldBo> fields) {
        if (fields == null || fields.isEmpty()) {
            return false;
        }

        for (SchemaFieldBo field : fields) {
            SchemaField update = MapstructUtils.convert(field, SchemaField.class);
            validEntityBeforeSave(update);
            baseMapper.updateById(update);
        }
        return true;
    }

    /**
     * 根据模型ID查询字段列表
     */
    @Override
    public List<SchemaFieldVo> queryListBySchemaId(Long schemaId) {
        LambdaQueryWrapper<SchemaField> lqw = Wrappers.lambdaQuery();
        lqw.eq(SchemaField::getSchemaId, schemaId);
        lqw.eq(SchemaField::getStatus, "0"); // 只查询正常状态的字段
        lqw.orderByAsc(SchemaField::getSort);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据表名获取代码生成元数据
     */
    @Override
    public Object getMetaDataByTableName(String tableName) {
        // 根据表名查询Schema
        SchemaVo schema = schemaService.queryByTableName(tableName);
        if (schema == null) {
            return null;
        }
        SchemaGroupVo schemaGroupVo = schemaGroupService.queryById(schema.getSchemaGroupId());
        if (schemaGroupVo == null) {
            return null;
        }
        // 查询Schema对应的字段列表
        List<SchemaFieldVo> fields = queryListBySchemaId(schema.getId());
        if (CollUtil.isEmpty(fields)) {
            return null;
        }

        // 构建代码生成器需要的数据结构
        Map<String, Object> result = new HashMap<>();
        result.put("schemaGroupCode", schemaGroupVo.getCode());
        result.put("tableName", schema.getTableName());
        result.put("tableComment", schema.getComment());
        result.put("className", toCamelCase(schema.getTableName(), true));
        result.put("tableCamelName", StrUtil.toCamelCase(schema.getTableName()));
        result.put("functionName", schema.getName());
        result.put("schemaName", schema.getName());

        // 查找主键字段
        SchemaFieldVo pkField = fields.stream()
                .filter(field -> "1".equals(field.getIsPk()))
                .findFirst()
                .orElse(null);

        if (pkField != null) {
            Map<String, Object> pkColumn = new HashMap<>();
            pkColumn.put("columnName", pkField.getCode());
            pkColumn.put("columnComment", pkField.getName());
            pkColumn.put("javaField", toCamelCase(pkField.getCode(), false));
            pkColumn.put("javaType", getJavaType(pkField.getType()));
            result.put("pkColumn", pkColumn);
        }

        // 构建字段列表
        List<Map<String, Object>> columns = new ArrayList<>();
        for (SchemaFieldVo field : fields) {
            Map<String, Object> column = new HashMap<>();
            column.put("columnName", field.getCode());
            column.put("columnComment", field.getName());
            column.put("javaField", toCamelCase(field.getCode(), false));
            column.put("javaType", getJavaType(field.getType()));
            column.put("htmlType", field.getHtmlType());
            column.put("isPk", "1".equals(field.getIsPk()));
            column.put("isRequired", "1".equals(field.getIsRequired()));
            column.put("isInsert", "1".equals(field.getIsInsert()));
            column.put("isEdit", "1".equals(field.getIsEdit()));
            column.put("isList", "1".equals(field.getIsList()));
            column.put("isQuery", "1".equals(field.getIsQuery()));
            column.put("component", getComponentType(field.getHtmlType()));
            column.put("remark", field.getRemark());
            columns.add(column);
        }
        result.put("columns", columns);

        return result;
    }

    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String str, boolean firstUpperCase) {
        if (StringUtils.isBlank(str)) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = str.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (i == 0 && !firstUpperCase) {
                result.append(part);
            } else {
                result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return result.toString();
    }

    /**
     * 获取Java类型
     */
    private String getJavaType(String dbType) {
        if (StringUtils.isBlank(dbType)) {
            return "String";
        }

        String type = dbType.toLowerCase();
        if (type.contains("int") || type.contains("tinyint") || type.contains("smallint")) {
            return "Integer";
        } else if (type.contains("bigint")) {
            return "Long";
        } else if (type.contains("decimal") || type.contains("numeric") || type.contains("float") || type.contains(
                "double")) {
            return "BigDecimal";
        } else if (type.contains("date") || type.contains("time")) {
            return "Date";
        } else if (type.contains("bit") || type.contains("boolean")) {
            return "Boolean";
        } else {
            return "String";
        }
    }

    /**
     * 获取组件类型
     */
    private String getComponentType(String htmlType) {
        if (StringUtils.isBlank(htmlType)) {
            return "input";
        }

        return switch (htmlType) {
            case "textarea" -> "textarea";
            case "select" -> "select";
            case "radio" -> "radio";
            case "checkbox" -> "checkbox";
            case "datetime" -> "datetime";
            case "date" -> "date";
            default -> "input";
        };
    }
}