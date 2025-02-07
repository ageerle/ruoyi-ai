package org.ruoyi.common.core.exception.user;

import java.io.Serial;

/**
 * 用户密码不正确或不符合规范异常类
 *
 * @author ruoyi
 */
public class UserPasswordNotMatchException extends UserException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserPasswordNotMatchException() {
        super("user.password.not.match");
    }
}
