package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.constant.UserConstants;
import org.ruoyi.common.core.xss.Xss;
import org.ruoyi.common.sensitive.annotation.Sensitive;
import org.ruoyi.common.sensitive.core.SensitiveStrategy;
import org.ruoyi.core.domain.BaseEntity;
import org.ruoyi.system.domain.SysUser;

/**
 * 用户信息业务对象 sys_user
 *
 * @author Michelle.Chung
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysUser.class, reverseConvertGenerate = false)
public class SysUserBo extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户账号
     */
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过{max}个字符")
    private String userName;

    /**
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * 手机号码
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

    /**
     * 密码
     */
    private String password;


    /**
     * 用户套餐
     */
    private String userPlan;

    /**
     * 帐号状态（0正常 1停用）
     */
    private String status;

    /**
     * 微信头像
     */
    private String avatar;

    /**
     * 备注
     */
    private String remark;

    /**
     * 注册域名
     */
    private String domainName;
    /**
     * 角色组
     */
    private Long[] roleIds;

    /**
     * 岗位组
     */
    private Long[] postIds;

    /**
     * 数据权限 当前角色ID
     */
    private Long roleId;

    /**
     * 普通用户的标识,对当前开发者帐号唯一。一个openid对应一个公众号或小程序
     */
    private String openId;

    /**
     * 用户等级
     */
    private String userGrade;

    /**
     * 用户余额
     */
    private Double userBalance;
    /**
     * 知识库角色组类型（role/roleGroup）
     */
    private String kroleGroupType;
    /**
     * 知识库角色组id（role/roleGroup）
     */
    private String kroleGroupIds;

    public SysUserBo(Long userId) {
        this.userId = userId;
    }

    public boolean isSuperAdmin() {
        return UserConstants.SUPER_ADMIN_ID.equals(this.userId);
    }

}
