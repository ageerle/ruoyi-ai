package org.ruoyi.generator.impl;

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
import org.ruoyi.generator.service.SchemaFieldService;
import org.ruoyi.generator.service.SchemaGroupService;
import org.ruoyi.generator.service.SchemaService;
import org.ruoyi.helper.DataBaseHelper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据模型字段Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SchemaFieldServiceImpl implements SchemaFieldService {

    private final SchemaFieldMapper baseMapper;
    private final SchemaService schemaService;
    private final SchemaGroupService schemaGroupService;

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
        if (Objects.isNull(bo.getSchemaId())) {
            return TableDataInfo.build();
        }
        LambdaQueryWrapper<SchemaField> lqw = buildQueryWrapper(bo);
        Page<SchemaFieldVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    private LambdaQueryWrapper<SchemaField> buildQueryWrapper(SchemaFieldBo bo) {
        LambdaQueryWrapper<SchemaField> lqw = Wrappers.lambdaQuery();
        lqw.eq(SchemaField::getSchemaId, bo.getSchemaId());
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
        lqw.orderByAsc(SchemaField::getSort);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据表名称查询字段列表
     */
    @Override
    public List<SchemaFieldVo> queryListByTableName(String tableName) {
        // 先根据表名查询Schema
        SchemaVo schema = schemaService.queryByTableName(tableName);
        if (schema == null) {
            return new ArrayList<>();
        }
        // 再根据Schema ID查询字段列表
        return queryListBySchemaId(schema.getId());
    }

    @Override
    public Boolean deleteWithValidBySchemaIds(Collection<Long> schemaIds, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        // 先根据Schema ID查询字段列表
        LambdaQueryWrapper<SchemaField> queryWrapper = Wrappers.lambdaQuery(SchemaField.class);
        queryWrapper.in(SchemaField::getSchemaId, schemaIds);
        List<SchemaField> fields = baseMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(fields)) {
            return false;
        }
        // 再根据字段ID删除
        return deleteWithValidByIds(fields.stream().map(SchemaField::getId).collect(Collectors.toList()), false);
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
        result.put("dictType", schema.getDictType());
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
            pkColumn.put("dictType", pkField.getDictType());

            pkColumn.put("columnComment", pkField.getName());
            pkColumn.put("javaField", StrUtil.toCamelCase(pkField.getCode()));
            pkColumn.put("javaType", getJavaType(pkField.getType()));
            result.put("pkColumn", pkColumn);
        }

        // 构建字段列表
        List<Map<String, Object>> columns = new ArrayList<>();
        for (SchemaFieldVo field : fields) {
            Map<String, Object> column = new HashMap<>();
            column.put("columnName", field.getCode());
            column.put("dictType", field.getDictType());
            column.put("columnComment", field.getName());
            column.put("javaField", StrUtil.toCamelCase(field.getCode()));
            column.put("javaType", getJavaType(field.getType()));
            column.put("htmlType", field.getHtmlType());
            column.put("isPk", field.getIsPk());
            column.put("isRequired", field.getIsRequired());
            column.put("isInsert", field.getIsInsert());
            column.put("isEdit", field.getIsEdit());
            column.put("isList", field.getIsList());
            column.put("isQuery", field.getIsQuery());
            column.put("component", getComponentType(field.getHtmlType(), field.getQueryType()));
            column.put("queryType", field.getQueryType());
            column.put("remark", field.getRemark());
            columns.add(column);
        }
        result.put("columns", columns);

        return result;
    }

    @Override
    public boolean batchInsertFieldsByTableName(Long schemaId, String tableName) {
        try {
            // 获取表的字段信息
            List<Map<String, Object>> columnInfos = DataBaseHelper.getTableColumnInfo(tableName);
            if (CollUtil.isEmpty(columnInfos)) {
                return false;
            }
            LambdaQueryWrapper<SchemaField> lqw = Wrappers.lambdaQuery();
            lqw.eq(SchemaField::getSchemaId, schemaId);
            // 检查是否已存在字段数据
            List<SchemaFieldVo> existingFields = baseMapper.selectVoList(lqw);
            if (CollUtil.isNotEmpty(existingFields)) {
                // 如果已存在字段，则不重复插入
                return true;
            }
            // 转换为 SchemaField 对象并批量插入
            List<SchemaField> fieldsToInsert = new ArrayList<>();
            int sort = 1;
            for (Map<String, Object> columnInfo : columnInfos) {
                SchemaField field = new SchemaField();
                field.setSchemaId(schemaId);
                field.setSchemaName(tableName);
                field.setDefaultValue((String) columnInfo.get("columnDefault"));
                field.setComment((String) columnInfo.get("columnComment"));
                field.setName((String) columnInfo.get("columnComment"));
                field.setDictType(StrUtil.toCamelCase((String) columnInfo.get("dictType")));
                field.setCode(StrUtil.toCamelCase((String) columnInfo.get("columnName")));
                field.setType((String) columnInfo.get("dataType"));
                field.setLength(Integer.valueOf(String.valueOf(columnInfo.get("columnSize"))));
                field.setIsPk((Boolean) columnInfo.get("isPrimaryKey") ? "1" : "0");
                field.setIsRequired(!(Boolean) columnInfo.get("isNullable") ? "1" : "0");
                if ("1".equals(field.getIsPk())) {
                    field.setIsInsert("0");
                    field.setIsEdit("0");
                } else {
                    field.setIsInsert("1");
                    field.setIsEdit("1");
                }
                field.setIsList("1");
                field.setIsQuery("1");
                field.setQueryType("EQ");
                field.setHtmlType(getDefaultHtmlType((String) columnInfo.get("dataType")));
                field.setSort(sort++);
                // 如果字段名为空，使用字段代码作为名称
                if (StringUtils.isBlank(field.getName())) {
                    field.setName(field.getCode());
                }
                fieldsToInsert.add(field);
            }
            // 批量插入
            fieldsToInsert.forEach(baseMapper::insert);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据数据库类型获取默认的HTML类型
     */
    private String getDefaultHtmlType(String dbType) {
        if (StringUtils.isBlank(dbType)) {
            return "input";
        }

        String type = dbType.toLowerCase();
        if (type.contains("text") || type.contains("longtext")) {
            return "textarea";
        } else if ("datetime".equals(type) || "timestamp".equals(type)) {
            return "datetime";
        } else if ("date".equals(type)) {
            return "date";
        } else if ("time".equals(type)) {
            return "time";
        } else if (type.contains("bit") || type.contains("boolean")) {
            return "radio";
        } else {
            return "input";
        }
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
        if (StrUtil.equalsAny(type, "int", "tinyint", "smallint")) {
            return "Integer";
        } else if (StrUtil.equalsAny(type, "bigint")) {
            return "Long";
        } else if (StrUtil.equalsAny(type, "decimal", "numeric", "float", "double")) {
            return "BigDecimal";
        } else if (StrUtil.equalsAny(type, "date", "datetime", "timestamp")) {
            return "Date";
        } else if (StrUtil.equalsAny(type, "bit", "boolean")) {
            return "Boolean";
        } else {
            return "String";
        }
    }

    /**
     * 获取组件类型
     */
    private String getComponentType(String htmlType, String queryType) {
        if (StringUtils.isBlank(htmlType)) {
            return "Input";
        }

        // 如果是范围查询且为日期时间类型，使用 RangePicker
        if ("BETWEEN".equals(queryType) &&
                ("datetime".equals(htmlType) || "date".equals(htmlType) || "time".equals(htmlType))) {
            return "RangePicker";
        }

        return switch (htmlType) {
            case "textarea" -> "Textarea";
            case "select" -> "Select";
            case "radio" -> "RadioGroup";
            case "checkbox" -> "CheckboxGroup";
            case "datetime", "date" -> "DatePicker";
            case "time" -> "TimePicker";
            case "imageUpload" -> "ImageUpload";
            case "fileUpload" -> "FileUpload";
            case "editor" -> "Editor";
            default -> "Input";
        };
    }


}