package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 【请填写功能名称】对象 sys_user_model
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_model")
public class SysUserModel extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 模型id
     */
    private Long mid;

    /**
     * 用户组id
     */
    private Long gid;


}
