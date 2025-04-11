package org.ruoyi.common.wechat.itchat4j.utils.enums.parameters;

/**
 *
 * 基本请求参数
 * 1. webWxInit      初始化
 * 2. wxStatusNotify 微信状态通知
 *
 * <p>
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum BaseParaEnum {

    Uin("Uin", "wxuin"),
    Sid("Sid", "wxsid"),
    Skey("Skey", "skey"),
    DeviceID("DeviceID", "pass_ticket");

    private String para;
    private String value;

    BaseParaEnum(String para, String value) {
        this.para = para;
        this.value = value;
    }

    public String para() {
        return para;
    }


    public Object value() {
        return value;
    }

}
