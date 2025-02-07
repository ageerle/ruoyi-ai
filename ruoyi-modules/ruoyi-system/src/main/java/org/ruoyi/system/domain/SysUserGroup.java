package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 【请填写功能名称】对象 sys_user_group
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_group")
public class SysUserGroup extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户组名称
     */
    private String groupName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本
     */
    @Version
    private Long version;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 更新IP
     */
    private String updateIp;


}
