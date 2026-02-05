package org.ruoyi.workflow.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.workflow.domain.FlowCategory;

/**
 * 流程分类业务对象 wf_category
 *
 * @author may
 * @date 2023-06-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FlowCategory.class, reverseConvertGenerate = false)
public class FlowCategoryBo extends BaseEntity {

    /**
     * 流程分类ID
     */
    @NotNull(message = "流程分类ID不能为空", groups = { EditGroup.class })
    private Long categoryId;

    /**
     * 父流程分类id
     */
    @NotNull(message = "父流程分类id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long parentId;

    /**
     * 流程分类名称
     */
    @NotBlank(message = "流程分类名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String categoryName;

    /**
     * 显示顺序
     */
    private Long orderNum;

}
