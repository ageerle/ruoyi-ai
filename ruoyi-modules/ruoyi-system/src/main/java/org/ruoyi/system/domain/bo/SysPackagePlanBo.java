package org.ruoyi.system.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.system.domain.SysPackagePlan;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 套餐管理业务对象 sys_package_plan
 *
 * @author Lion Li
 * @date 2024-05-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysPackagePlan.class, reverseConvertGenerate = false)
public class SysPackagePlanBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 套餐名称
     */
    @NotBlank(message = "套餐名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 套餐价格
     */
    @NotNull(message = "套餐价格不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal price;

    /**
     * 有效时间
     */
    @NotNull(message = "有效时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long duration;

    /**
     * 计划详情
     */
    @NotBlank(message = "计划详情不能为空", groups = { AddGroup.class, EditGroup.class })
    private String planDetail;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
