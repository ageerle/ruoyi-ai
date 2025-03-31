package org.ruoyi.common.sensitive.core;

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
     * 密钥脱敏
     */
    SKY(s -> DesensitizedUtil.idCardNum(s, 0, 1)),

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
    BANK_CARD(DesensitizedUtil::bankCard);

    //可自行添加其他脱敏策略

    private final Function<String, String> desensitizer;

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}
