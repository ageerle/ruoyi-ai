package com.xmzs.web.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 用户登录
 */
@Data
public class EmailRequest {
    /**
     * 账号
     */
    @NotNull(message = "账号不能为空")
    private String username;

}
