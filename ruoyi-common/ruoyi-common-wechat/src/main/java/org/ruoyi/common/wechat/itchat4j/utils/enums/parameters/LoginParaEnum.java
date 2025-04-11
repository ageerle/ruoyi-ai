package org.ruoyi.common.wechat.itchat4j.utils.enums.parameters;

/**
 * 登陆
 * <p>
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum LoginParaEnum {

    LOGIN_ICON("loginicon", "true"),
    UUID("uuid", ""),
    TIP("tip", "0"),
    R("r", ""),
    _1("_", "");

    private String para;
    private String value;

    LoginParaEnum(String para, String value) {
        this.para = para;
        this.value = value;
    }

    public String para() {
        return para;
    }

    public String value() {
        return value;
    }
}
