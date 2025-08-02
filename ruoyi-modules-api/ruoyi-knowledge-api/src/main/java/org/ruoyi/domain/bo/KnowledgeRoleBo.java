package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.domain.KnowledgeRole;

import java.util.List;

/**
 * 知识库角色业务对象 knowledge_role
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeRole.class, reverseConvertGenerate = false)
public class KnowledgeRoleBo extends BaseEntity {

    /**
     * 知识库角色id
     */
    @NotNull(message = "知识库角色id不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 知识库角色组id
     */
    @NotNull(message = "知识库角色组id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /**
     * 知识库角色name
     */
    @NotBlank(message = "知识库角色name不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 知识库id列表
     */
    private List<Long> knowledgeIds;

}
