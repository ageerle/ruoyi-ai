package org.ruoyi.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提示词模板分类
 *
 * @author evo
 */
@Getter
@AllArgsConstructor
public enum promptTemplateEnum {
    CHAT(1, "chat"),
    VECTOR(2, "vector"),
    ;

    private final Integer code;
    private final String desc;

}

