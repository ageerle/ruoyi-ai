package org.ruoyi.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiModelStatus implements BaseEnum {
    ACTIVE(1, "启用"),
    INACTIVE(2, "停用");

    private final Integer value;
    private final String desc;
}
