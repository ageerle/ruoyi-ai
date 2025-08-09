package org.ruoyi.system.domain.bo;

import org.ruoyi.system.domain.SysDemo;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.util.Date;
import java.io.Serializable;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import java.io.Serializable;
import java.io.Serializable;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

/**
 * dome管理业务对象 sys_demo
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
@Data

@AutoMapper(target = SysDemo.class, reverseConvertGenerate = false)
public class SysDemoBo implements Serializable {

        /**
         * ID
         */
            @NotNull(message = "ID不能为空", groups = {  EditGroup.class })
    private Integer id;

        /**
         * 系统代码
         */
            @NotBlank(message = "系统代码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sysCode;

        /**
         * 系统名称
         */
            @NotBlank(message = "系统名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sysName;

        /**
         * 系统状态
         */
            @NotNull(message = "系统状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer sysStatus;

        /**
         * 创建部门
         */
    private Integer createDept;

        /**
         * 创建者
         */
    private Integer createBy;

        /**
         * 创建时间
         */
    private Date createTime;

        /**
         * 更新者
         */
    private Integer updateBy;

        /**
         * 更新时间
         */
    private Date updateTime;

        /**
         * 备注
         */
    private String remark;


}
