package org.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 用户个人信息
 *
 * @author Michelle.Chung
 */
@Data
public class ProfileVo {

    /**
     * 用户信息
     */
    private SysUserVo user;

    /**
     * 用户所属角色组
     */
    private String roleGroup;

    /**
     * 用户所属岗位组
     */
    private String postGroup;


}
