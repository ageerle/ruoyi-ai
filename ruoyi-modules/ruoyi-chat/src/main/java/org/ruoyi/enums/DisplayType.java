package org.ruoyi.enums;

import lombok.Getter;

/**
 * 是否显示
 *
 * @author ageerle@163.com
 * @date 2025-12-14
 */
@Getter
public enum DisplayType {
    HIDDEN("1", "不显示"),
    VISIBLE("0", "显示");

    private final String code;
    private final String description;

    DisplayType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
