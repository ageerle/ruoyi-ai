package org.ruoyi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计费类型枚举
 *
 * @author ageerle@163.com
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum BillingType {

    TOKEN("1", "token计费"),
    COUNT("2", "次数计费"),
    ;

    private final String code;
    private final String description;

}