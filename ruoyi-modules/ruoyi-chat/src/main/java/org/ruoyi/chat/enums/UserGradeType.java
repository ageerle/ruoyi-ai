package org.ruoyi.chat.enums;

import lombok.Getter;

@Getter
public enum UserGradeType {
    UNPAID("0", "未付费"), // 未付费用户
    PAID("1", "已付费");   // 已付费用户

    private final String code;
    private final String description;

    UserGradeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserGradeType fromCode(String code) {
        for (UserGradeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
