package org.ruoyi.generator.domain.vo;


import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ruoyi.generator.domain.SchemaField;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据模型字段视图对象 SchemaFieldVo
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@AutoMapper(target = SchemaField.class)
public class SchemaFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模型ID
     */
    private Long schemaId;

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段编码
     */
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
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 是否列表显示（0否 1是）
     */
    @Schema(description = "是否列表显示")
    private String isList;

    /**
     * 是否查询字段（0否 1是）
     */
    @Schema(description = "是否查询字段")
    private String isQuery;

    /**
     * 是否插入字段（0否 1是）
     */
    @Schema(description = "是否插入字段")
    private String isInsert;

    /**
     * 是否编辑字段（0否 1是）
     */
    @Schema(description = "是否编辑字段")
    private String isEdit;

    /**
     * 查询方式
     */
    @Schema(description = "查询方式")
    private String queryType;

    /**
     * 显示类型
     */
    @Schema(description = "显示类型")
    private String htmlType;

    /**
     * 字典类型
     */
    @Schema(description = "字典类型")
    private String dictType;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 扩展JSON
     */
    private String extendJson;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

}