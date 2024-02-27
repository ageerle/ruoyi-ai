package com.xmzs.common.wechat.utils.enums.parameters;

/**
 * 状态通知
 * <p>
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum StatusNotifyParaEnum {

    CODE("Code", "3"),
    FROM_USERNAME("FromUserName", ""),
    TO_USERNAME("ToUserName", ""),
    CLIENT_MSG_ID("ClientMsgId", ""); //时间戳

    private String para;
    private String value;

    StatusNotifyParaEnum(String para, String value) {
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
