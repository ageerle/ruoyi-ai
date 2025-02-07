package org.ruoyi.system.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.system.domain.SysUserGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 【请填写功能名称】业务对象 sys_user_group
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysUserGroup.class, reverseConvertGenerate = false)
public class SysUserGroupBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户组名称
     */
    @NotBlank(message = "用户组名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String groupName;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;

    /**
     * 更新IP
     */
    @NotBlank(message = "更新IP不能为空", groups = { AddGroup.class, EditGroup.class })
    private String updateIp;


}
