package org.ruoyi.agent.domain;

import lombok.Data;

/**
 * 单个查询条件
 * 用于 WHERE 子句中的条件
 *
 * 示例：
 * Condition c = new Condition();
 * c.setField("order_date");
 * c.setOperator(">=");
 * c.setValue("2024-01-01");
 */
@Data
public class Condition {

    /**
     * 字段名
     * 仅允许：字母、数字、下划线
     */
    private String field;

    /**
     * 操作符
     * 支持：=, !=, <, >, <=, >=, LIKE, IN, BETWEEN, IS NULL, IS NOT NULL
     */
    private String operator;

    /**
     * 条件值
     * 类型可以是：String, Number, Boolean 等
     * 会自动转义防止 SQL 注入
     */
    private Object value;

    /**
     * 构造函数
     */
    public Condition() {
    }

    /**
     * 带参数的构造函数
     */
    public Condition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
}
