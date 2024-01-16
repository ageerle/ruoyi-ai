package com.xmzs.common.core.domain.model;

import com.xmzs.common.core.constant.UserConstants;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录对象
 *
 * @author Lion Li
 */

@Data
public class LoginBody {

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
   // @Length(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    // @Length(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH, message = "{user.password.length.valid}")
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 唯一标识
     */
    private String uuid;

}
