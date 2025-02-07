package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 用户信息视图对象 sys_user
 *
 * @author Michelle.Chung
 */
@Data
public class SysUserOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户账号
     */
    private String name;

}
