package org.ruoyi.system.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 编辑用户
 */
@Data
public class UserRequest {
    /**
     * 用户名称
     */
    @NotNull(message = "用户名称不能为空")
    private String nickName;

}
