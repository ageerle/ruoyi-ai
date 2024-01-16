package com.xmzs.common.core.domain.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 短信登录对象
 *
 * @author Lion Li
 */

@Data
public class EmailLoginBody {

    /**
     * 租户ID
     */
    @NotBlank(message = "{tenant.number.not.blank}")
    private String tenantId;

    /**
     * 邮箱
     */
    @NotBlank(message = "{user.email.not.blank}")
    @Email(message = "{user.email.not.valid}")
    private String email;

    /**
     * 邮箱code
     */
    @NotBlank(message = "{email.code.not.blank}")
    private String emailCode;

}
