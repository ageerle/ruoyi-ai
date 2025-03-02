package org.ruoyi.common.wechat.web.base;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 错误管理
 * @author WesleyOne
 * @create 2018/7/28
 */
public enum BaseError {

    UNPERMISSION("09","没有操作权限"),
    UNLOGIN("01","未登录或登录过期"),
    OPERATION_ERR("8899","操作失败!"),
    NORMAL_ERR("8999","参数异常"),
    SYSTEM_ERR("9999","系统异常");


    private String code;
    private String msg;
    BaseError(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    private static final Map<String, BaseError> lookup = new HashMap<String, BaseError>();
    static {
        for (BaseError s : EnumSet.allOf(BaseError.class))
            lookup.put(s.getMsg(), s);
    }

    /**
     * 获取枚举的值（整数值、字符串值等）
     * @return
     */
    public String getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }


    /**
     * 根据值（整数值、字符串值等）获取相应的枚举类型
     * @param code
     * @return
     */
    public static BaseError fromValue(String code) {
        return lookup.get(code);
    }
}
