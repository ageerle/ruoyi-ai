package org.ruoyi.workflow.domain.bo;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.workflow.domain.FlowSpel;

/**
 * 流程spel达式定义业务对象 flow_spel
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FlowSpel.class, reverseConvertGenerate = false)
public class FlowSpelBo extends BaseEntity {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数
     */
    private String methodParams;

    /**
     * 预览spel值
     */
    @NotBlank(message = "预览spel值不能为空", groups = { AddGroup.class, EditGroup.class })
    private String viewSpel;

    /**
     * 状态（0正常 1停用）
     */
    @NotBlank(message = "状态（0正常 1停用）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 备注
     */
    private String remark;

}
