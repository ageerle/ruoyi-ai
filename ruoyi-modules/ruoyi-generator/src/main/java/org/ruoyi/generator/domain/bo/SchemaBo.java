package org.ruoyi.generator.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.generator.domain.Schema;

/**
 * 数据模型业务对象 SchemaBo
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Schema.class, reverseConvertGenerate = false)
public class SchemaBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 分组ID
     */
    private Long schemaGroupId;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 模型编码
     */
    @NotBlank(message = "模型编码不能为空", groups = {AddGroup.class, EditGroup.class})
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

}