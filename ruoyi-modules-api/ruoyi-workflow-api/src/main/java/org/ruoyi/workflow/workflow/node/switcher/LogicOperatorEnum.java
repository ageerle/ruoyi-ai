package org.ruoyi.workflow.workflow.node.switcher;

public enum LogicOperatorEnum {
    AND("and"),

    OR("or");

    private final String name;

    LogicOperatorEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
