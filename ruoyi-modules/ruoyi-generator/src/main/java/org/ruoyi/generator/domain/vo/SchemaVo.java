package org.ruoyi.generator.domain.vo;


import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.generator.domain.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据模型视图对象 SchemaVo
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@AutoMapper(target = Schema.class)
public class SchemaVo implements Serializable {

    /**
     * 主键
     */
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
     * 表注释
     */
    private String comment;

    /**
     * 存储引擎
     */
    private String engine;

    /**
     * 列表字段
     */
    private String listKeys;

    /**
     * 搜索表单字段
     */
    private String searchFormKeys;

    /**
     * 表单设计
     */
    private String designer;

    /**
     * 状态
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

}