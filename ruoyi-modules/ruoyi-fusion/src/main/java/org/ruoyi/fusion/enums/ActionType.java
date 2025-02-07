package org.ruoyi.fusion.enums;

import lombok.Getter;

/**
 * @author WangLe
 */
@Getter
public enum ActionType {
    IN_PAINT("Inpaint"),   // 局部重绘操作
    RE_ROLL("reroll"),     // 重绘操作
    UP_SAMPLE("upsample"), // 放大操作
    ZOOM("zoom"),         // 变焦操作
    UPSCALE("upscale"),   // 高清放大操作
    VARIATION("variation"); // 变化操作

    private final String action;

    ActionType(String action) {
        this.action = action;
    }

    public static ActionType fromCustomId(String customId) {
        for (ActionType type : values()) {
            if (type.getAction().equals(customId)) {
                return type;
            }
        }
        return null;
    }
}

