package org.ruoyi.generator.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.generator.domain.SchemaField;

/**
 * 数据模型字段业务对象 SchemaFieldBo
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SchemaField.class, reverseConvertGenerate = false)
public class SchemaFieldBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long schemaId;

    /**
     * 模型名称
     */
    @NotNull(message = "模型名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String schemaName;

    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 字段编码
     */
    @NotBlank(message = "字段编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String code;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 是否主键
     */
    private String isPk;

    /**
     * 是否必填
     */
    private String isRequired;

    /**
     * 是否唯一
     */
    private String isUnique;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 字段长度
     */
    private Integer length;

    /**
     * 小数位数
     */
    private Integer scale;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否列表显示（0否 1是）
     */
    private String isList;

    /**
     * 是否查询字段（0否 1是）
     */
    private String isQuery;

    /**
     * 是否插入字段（0否 1是）
     */
    private String isInsert;

    /**
     * 是否编辑字段（0否 1是）
     */
    private String isEdit;

    /**
     * 查询方式
     */
    private String queryType;

    /**
     * 显示类型
     */
    private String htmlType;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 状态
     */
    private String status;

    /**
     * 扩展JSON
     */
    private String extendJson;

    /**
     * 备注
     */
    private String remark;

}