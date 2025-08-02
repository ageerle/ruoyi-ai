package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.KnowledgeRoleRelation;

import java.io.Serial;
import java.io.Serializable;


/**
 * 知识库角色与知识库关联视图对象 knowledge_role_relation
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeRoleRelation.class)
public class KnowledgeRoleRelationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 知识库角色id
     */
    @ExcelProperty(value = "知识库角色id")
    private Long knowledgeRoleId;

    /**
     * 知识库id
     */
    @ExcelProperty(value = "知识库id")
    private Long knowledgeId;


}
