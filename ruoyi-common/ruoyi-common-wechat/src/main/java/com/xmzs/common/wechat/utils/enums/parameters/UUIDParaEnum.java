package com.xmzs.common.wechat.utils.enums.parameters;

/**
 * UUID
 * <p>
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum UUIDParaEnum {

    APP_ID("appid", "wx782c26e4c19acffb"),
    FUN("fun", "new"),
    LANG("lang", "zh_CN"),
    REDIRECT_URL("redirect_uri",
            "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?mod=desktop"),
    _1("_", "时间戳");


    private String para;
    private String value;

    UUIDParaEnum(String para, String value) {
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
