package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.KnowledgeRoleGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 知识库角色组视图对象 knowledge_role_group
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeRoleGroup.class)
public class KnowledgeRoleGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库角色组id
     */
    @ExcelProperty(value = "知识库角色组id")
    private Long id;

    /**
     * 知识库角色组name
     */
    @ExcelProperty(value = "知识库角色组名称")
    private String name;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新时间")
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
