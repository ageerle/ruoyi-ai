package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.domain.KnowledgeRoleRelation;

/**
 * 知识库角色与知识库关联业务对象 knowledge_role_relation
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeRoleRelation.class, reverseConvertGenerate = false)
public class KnowledgeRoleRelationBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = {AddGroup.class, EditGroup.class})
    private String remark;

    /**
     * 知识库角色id
     */
    @NotNull(message = "知识库角色id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long knowledgeRoleId;

    /**
     * 知识库id
     */
    @NotNull(message = "知识库id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long knowledgeId;


}
