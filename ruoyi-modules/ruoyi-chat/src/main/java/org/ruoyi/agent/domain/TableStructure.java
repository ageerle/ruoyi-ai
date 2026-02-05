package org.ruoyi.agent.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表结构信息
 * 包含表的所有字段、类型、约束等信息
 * Agent 使用此信息来理解数据库架构
 */
@Data
public class TableStructure {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释/说明
     */
    private String tableComment;

    /**
     * 字段列表
     */
    private List<ColumnInfo> columns = new ArrayList<>();

    /**
     * 主键字段名
     */
    private String primaryKey;

    /**
     * 获取字段总数
     * @return 字段数量
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * 获取格式化的表结构描述
     * 用于 Agent 理解表结构
     *
     * @return 格式化的表结构描述
     */
    public String getFormattedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("表: ").append(tableName);
        if (tableComment != null && !tableComment.isEmpty()) {
            sb.append("(").append(tableComment).append(")");
        }
        sb.append("\n字段:\n");
        for (ColumnInfo col : columns) {
            sb.append("  - ").append(col.getColumnName())
                    .append(" (").append(col.getColumnType()).append(")");
            if (!col.isNullable()) {
                sb.append(" NOT NULL");
            }
            if (col.getColumnComment() != null && !col.getColumnComment().isEmpty()) {
                sb.append(" // ").append(col.getColumnComment());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
