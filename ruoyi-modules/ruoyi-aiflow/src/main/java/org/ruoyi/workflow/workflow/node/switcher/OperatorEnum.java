package org.ruoyi.workflow.workflow.node.switcher;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OperatorEnum {
    CONTAINS("contains", "包含"),

    NOT_CONTAINS("not contains", "不包含"),

    START_WITH("start with", "开始内容是"),

    END_WITH("end with", "结束内容是"),

    EMPTY("empty", "为空"),

    NOT_EMPTY("not empty", "不为空"),

    EQUAL("=", "等于"),

    NOT_EQUAL("!=", "不等于"),

    GREATER(">", "大于"),

    GREATER_OR_EQUAL(">=", "大于或等于"),

    LESS("<", "小于"),

    LESS_OR_EQUAL("<=", "小于或等于");

    private final String name;

    private final String desc;

    OperatorEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static OperatorEnum getByName(String name) {
        return Arrays.stream(OperatorEnum.values()).filter(item -> item.name.equals(name)).findFirst().orElse(null);
    }
}
