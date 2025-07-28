package org.ruoyi.generator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 数据模型字段对象 dev_schema_field
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dev_schema_field")
public class SchemaField extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 模型ID
     */
    private Long schemaId;

    /**
     * 模型名称
     */
    private String schemaName;

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
     * 查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围）
     */
    private String queryType;

    /**
     * 显示类型（input输入框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件、image图片上传、upload文件上传、editor富文本编辑器）
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

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 租户编号
     */
    private String tenantId;

}