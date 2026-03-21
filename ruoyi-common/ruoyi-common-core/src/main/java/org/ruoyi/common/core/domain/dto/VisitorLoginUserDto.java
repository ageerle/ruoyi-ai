package org.ruoyi.common.core.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.domain.model.LoginUser;

import java.io.Serial;

/**
 * 小程序登录用户信息
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class VisitorLoginUserDto extends LoginUser {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * openid
     */
    private String openid;


}
