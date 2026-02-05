package org.ruoyi.common.sensitive.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * 脱敏策略
 *
 * @author Yjoioooo
 * @version 3.6.0
 */
@AllArgsConstructor
public enum SensitiveStrategy {

    /**
     * 身份证脱敏
     */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 3, 4)),

    /**
     * 手机号脱敏
     */
    PHONE(DesensitizedUtil::mobilePhone),

    /**
     * 地址脱敏
     */
    ADDRESS(s -> DesensitizedUtil.address(s, 8)),

    /**
     * 邮箱脱敏
     */
    EMAIL(DesensitizedUtil::email),

    /**
     * 银行卡
     */
    BANK_CARD(DesensitizedUtil::bankCard),

    /**
     * 中文名
     */
    CHINESE_NAME(DesensitizedUtil::chineseName),

    /**
     * 固定电话
     */
    FIXED_PHONE(DesensitizedUtil::fixedPhone),

    /**
     * 用户ID
     */
    USER_ID(s -> Convert.toStr(DesensitizedUtil.userId())),

    /**
     * 密码
     */
    PASSWORD(DesensitizedUtil::password),

    /**
     * ipv4
     */
    IPV4(DesensitizedUtil::ipv4),

    /**
     * ipv6
     */
    IPV6(DesensitizedUtil::ipv6),

    /**
     * 中国大陆车牌，包含普通车辆、新能源车辆
     */
    CAR_LICENSE(DesensitizedUtil::carLicense),

    /**
     * 只显示第一个字符
     */
    FIRST_MASK(DesensitizedUtil::firstMask),

    /**
     * 清空为""
     */
    CLEAR(s -> DesensitizedUtil.clear()),

    /**
     * 清空为null
     */
    CLEAR_TO_NULL(s -> DesensitizedUtil.clearToNull());

    //可自行添加其他脱敏策略

    private final Function<String, String> desensitizer;

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}
