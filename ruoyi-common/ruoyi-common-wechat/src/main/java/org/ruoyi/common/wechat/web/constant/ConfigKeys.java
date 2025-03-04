package org.ruoyi.common.wechat.web.constant;

/**
 * @author WesleyOne
 * @create 2018/12/13
 */
public class ConfigKeys {

    /**
     * 默认全局关键字回复用昵称，该字段超过16字符防止与用户昵称冲突
     *
     * http://kf.qq.com/touch/wxappfaq/150910F322eY150910eIV32Q.html?platform=14
     * 微信昵称设置规则
     * 最多可设置16个汉字，可设置含有中文、英文、数字、符号组合的昵称，但不建议设置特殊字符。
     * 温馨提示：1个符号相当于一个汉字，2个数字/英文相当于1个汉字；
     */
    public static final String DEAFAULT_KEYWORD = "默认全局关键字回复用昵称-请勿修改";
    public static final String DEAFAULT_WELCOME = "默认群欢迎新人用-请勿修改";


}
