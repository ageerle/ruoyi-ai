package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.SysUserModel;

/**
 * 【请填写功能名称】业务对象 sys_user_model
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysUserModel.class, reverseConvertGenerate = false)
public class SysUserModelBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 模型id
     */
    @NotNull(message = "模型id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long mid;

    /**
     * 用户组id
     */
    @NotNull(message = "用户组id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long gid;


}
