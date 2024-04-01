package com.xmzs.web.domain.vo;

import com.xmzs.common.core.domain.model.LoginUser;
import lombok.Data;

/**
 * 登录返回信息
 *
 * @author Michelle.Chung
 */
@Data
public class LoginVo {
    private String token;
    private LoginUser userInfo;
}
