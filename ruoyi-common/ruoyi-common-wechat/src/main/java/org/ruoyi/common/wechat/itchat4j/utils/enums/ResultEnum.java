package org.ruoyi.common.wechat.itchat4j.utils.enums;

import lombok.Getter;

/**
 * 返回结构枚举类
 * <p>
 * Created by xiaoxiaomo on 2017/5/6.
 */
@Getter
public enum ResultEnum {

    SUCCESS("200", "成功"),
    WAIT_CONFIRM("201", "请在手机上点击确认"),
    WAIT_SCAN("400", "请扫描二维码");


    private final String code;
    private final String msg;

    ResultEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
