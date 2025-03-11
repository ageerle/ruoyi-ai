package org.ruoyi.system.domain.bo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 描述：用户密码修改bo
 *
 * @author ageerle@163.com
 * date 2025/3/9
 */
@Data
public class SysUserPasswordBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
