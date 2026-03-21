package org.ruoyi.agent.domain;

import lombok.Data;

/**
 * 数据库列（字段）信息
 * 描述表中单个列的详细信息
 */
@Data
public class ColumnInfo {

    /**
     * 列名
     */
    private String columnName;

    /**
     * 列数据类型
     * 示例：VARCHAR(100), INT, DECIMAL(10,2), DATE 等
     */
    private String columnType;

    /**
     * 是否可为空
     */
    private boolean nullable = true;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 列注释/说明
     */
    private String columnComment;

    /**
     * 是否是主键
     */
    private boolean primaryKey = false;

    /**
     * 是否自增
     */
    private boolean autoIncrement = false;

    /**
     * 是否有索引
     */
    private boolean indexed = false;
}
