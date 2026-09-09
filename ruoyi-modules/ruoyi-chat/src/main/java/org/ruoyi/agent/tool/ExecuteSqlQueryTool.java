package org.ruoyi.agent.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.sql.DataSource;

import org.ruoyi.agent.manager.TableSchemaManager;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;

/**
 * 执行 SQL 查询的 Tool
 * 执行指定的 SELECT SQL 查询并返回结果
 */
@Slf4j
@Component
public class ExecuteSqlQueryTool implements BuiltinToolProvider {

    // 使用延迟初始化，避免在构造函数中调用 SpringUtils.getBean()
    DataSource getDataSource() {
        return SpringUtils.getBean(DataSource.class);
    }

    TableSchemaManager getTableSchemaManager() {
        return SpringUtils.getBean(TableSchemaManager.class);
    }

    /**
     * 执行 SELECT SQL 查询
     * 只允许执行 SELECT 查询，防止恶意的数据修改操作
     *
     * @param sql 要执行的 SELECT SQL 语句，例如：SELECT * FROM sys_user
     * @return 包含查询结果的字符串
     */
    @Tool("Execute one read-only SELECT on allowed tables. Returns a Markdown table (at most 10 rows and 8 columns), not JSON. Aggregate and LIMIT in SQL; use explicit column aliases. Never infer omitted rows.")
    public String executeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "Error: SQL query cannot be empty";
        }

        // 只允许执行 SELECT 查询，防止恶意操作
        String upperSql = sql.trim().toUpperCase(Locale.ROOT);
        if (!upperSql.startsWith("SELECT")) {
            return "Error: Only SELECT queries are allowed for security reasons";
        }

        boolean routed = false;
        try {
            // 校验表白名单：未配置表时直接拒绝，已配置则校验 SQL 中引用的表
            TableSchemaManager schemaManager = getTableSchemaManager();
            List<String> allowedTables = schemaManager.getAllowedTableNames();
            if (allowedTables.isEmpty()) {
                return "Error: 当前未配置可查询的数据库表，无法执行任何SQL查询。请联系管理员配置 AGENT_ALLOWED_TABLES";
            }
            try {
                AgentSqlValidator.validate(sql, allowedTables);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
            DynamicDataSourceContextHolder.push("agent");
            routed = true;
            DataSource dataSource = getDataSource();
            if (dataSource == null) {
                return "Error: Database datasource not configured";
            }

            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setQueryTimeout(30);
                    preparedStatement.setMaxRows(1001);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {

                        ResultSetMetaData metaData = resultSet.getMetaData();

                        List<Map<String, Object>> results = new ArrayList<>();
                        int columnCount = metaData.getColumnCount();

                        // 获取列名
                        List<String> columnNames = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            columnNames.add(metaData.getColumnLabel(i));
                        }

                        // 获取数据行，限制最多1000行以防止内存溢出
                        int maxRows = 1000;
                        while (results.size() < maxRows && resultSet.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(columnNames.get(i - 1), resultSet.getObject(i));
                            }
                            results.add(row);
                        }

                        boolean truncated = results.size() == maxRows && resultSet.next();
                        return formatResults(results, columnNames, truncated);
                    }
                }
            }
        } catch (Exception e) {
            log.error("sql_tool operation=EXECUTE_SELECT status=FAILED errorType={}",
                e.getClass().getName());
            return "Error: Database query failed";
        } finally {
            // Restore the caller's route instead of clearing its entire datasource stack.
            if (routed) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }

    /**
     * 格式化查询结果
     * 返回清晰的表格格式，展示关键数据
     */
    private String formatResults(List<Map<String, Object>> results, List<String> columnNames, boolean truncated) {
        if (results.isEmpty()) {
            return "Query executed successfully, but no results returned";
        }

        StringBuilder result = new StringBuilder();

        // 限制显示的列数和行数，避免输出过大
        int displayCols = Math.min(columnNames.size(), 8);  // 最多显示8列
        int displayRows = Math.min(results.size(), 10);     // 最多显示10行

        List<String> displayColumns = columnNames.subList(0, displayCols);

        // 构建表头
        result.append("| ");
        for (String col : displayColumns) {
            result.append(formatColumnName(col)).append(" | ");
        }
        result.append("\n");

        // 构建分隔线
        result.append("|");
        for (int i = 0; i < displayCols; i++) {
            result.append(" --- |");
        }
        result.append("\n");

        // 构建数据行
        for (int i = 0; i < displayRows; i++) {
            result.append("| ");
            Map<String, Object> row = results.get(i);
            for (String column : displayColumns) {
                Object value = row.get(column);
                String displayValue = formatValue(value);
                result.append(displayValue).append(" | ");
            }
            result.append("\n");
        }

        // 统计信息
        result.append("\n").append(truncated ? "Read at least: " : "Total: ")
            .append(results.size()).append(" rows");
        if (displayRows < results.size()) {
            result.append(" (displayed ").append(displayRows).append(" rows)");
        }
        if (displayCols < columnNames.size()) {
            result.append("\nColumns: ").append(displayCols).append(" / ").append(columnNames.size());
        }
        if (truncated || displayRows < results.size() || displayCols < columnNames.size()) {
            result.append("\nResult truncated. Aggregate/filter in SQL before charting; do not infer omitted data.");
        }

        log.info("Successfully executed SQL query, returned {} rows", results.size());
        return result.toString();
    }

    /**
     * 格式化列名，使其更易读
     */
    private String formatColumnName(String columnName) {
        return escapeCell(columnName);
    }

    /**
     * 格式化单个值，适合表格显示
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "-";
        }
        String str = value.toString();
        // 限制列宽以保持表格整洁，长文本截断
        if (str.length() > 20) {
            return escapeCell(str.substring(0, 17) + "...");
        }
        return escapeCell(str);
    }

    private String escapeCell(String text) {
        return text.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    @Override
    public String getToolName() {
        return "execute_sql_query";
    }

    @Override
    public String getDisplayName() {
        return "执行SQL查询";
    }

    @Override
    public String getDescription() {
        return "Execute a read-only SELECT on allowed tables and return a bounded Markdown table";
    }
}
