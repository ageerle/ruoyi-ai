package com.xmzs.system.domain.bo;

import com.xmzs.common.core.validate.AddGroup;
import com.xmzs.common.core.validate.EditGroup;
import com.xmzs.common.mybatis.core.domain.BaseEntity;
import com.xmzs.system.domain.SysConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数配置业务对象 sys_config
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysConfig.class, reverseConvertGenerate = false)
public class SysConfigBo extends BaseEntity {

    /**
     * 参数主键
     */
    @NotNull(message = "参数主键不能为空", groups = { EditGroup.class })
    private Long configId;

    /**
     * 参数名称
     */
    @NotBlank(message = "参数名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "参数名称不能超过{max}个字符")
    private String configName;

    /**
     * 参数键名
     */
    @NotBlank(message = "参数键名不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "参数键名长度不能超过{max}个字符")
    private String configKey;

    /**
     * 参数键值
     */
    @NotBlank(message = "参数键值不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 500, message = "参数键值长度不能超过{max}个字符")
    private String configValue;

    /**
     * 系统内置（Y是 N否）
     */
    private String configType;

    /**
     * 备注
     */
    private String remark;


}
