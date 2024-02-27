package com.xmzs.common.wechat.utils.enums;

/**
 * 返回结构枚举类
 * <p>
 * Created by xiaoxiaomo on 2017/5/6.
 */
public enum ResultEnum {

    SUCCESS("200", "成功"),
    WAIT_CONFIRM("201", "请在手机上点击确认"),
    WAIT_SCAN("400", "请扫描二维码");

    private String code;
    private String msg;

    ResultEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

//    public static MsgInfoEnum getCode(String code) {
//        switch (code) {
//            case "Text":
//                return MsgInfoEnum.TEXT;
//            default:
//                return MsgInfoEnum.VIDEO;
//        }
//    }

}
