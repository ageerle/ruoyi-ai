package org.ruoyi.generator.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据模型对象 dev_schema
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@TableName("dev_schema")
public class Schema implements Serializable {


    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 分组ID
     */
    private Long schemaGroupId;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型编码
     */
    private String code;

    /**
     * 表名
     */
    private String tableName;

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
     * 创建部门
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createDept;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}