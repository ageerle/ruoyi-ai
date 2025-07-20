package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.KnowledgeRole;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 知识库角色视图对象 knowledge_role
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeRole.class)
public class KnowledgeRoleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库角色id
     */
    @ExcelProperty(value = "知识库角色id")
    private Long id;

    /**
     * 知识库角色组id
     */
    @ExcelProperty(value = "知识库角色组id")
    private Long groupId;

    /**
     * 知识库角色name
     */
    @ExcelProperty(value = "知识库角色name")
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


    /**
     * 知识库id列表
     */
    private List<Long> knowledgeIds;


    /**
     * 角色组名称
     */
    private String groupName;

}
