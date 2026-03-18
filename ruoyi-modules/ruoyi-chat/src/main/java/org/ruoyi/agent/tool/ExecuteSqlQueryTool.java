package org.ruoyi.agent.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${AGENT_DATASOURCE}")
    private String agentDataSource;

    // 使用延迟初始化，避免在构造函数中调用 SpringUtils.getBean()
    private DataSource getDataSource() {
        return SpringUtils.getBean(DataSource.class);
    }

    /**
     * 执行 SELECT SQL 查询
     * 只允许执行 SELECT 查询，防止恶意的数据修改操作
     *
     * @param sql 要执行的 SELECT SQL 语句，例如：SELECT * FROM sys_user
     * @return 包含查询结果的字符串
     */
    @Tool("Execute a SELECT SQL query and return the results. Example: SELECT * FROM sys_user")
    public String executeSql(String sql) {
        // 2. 手动推入数据源上下文
        DynamicDataSourceContextHolder.push(agentDataSource);
        if (sql == null || sql.trim().isEmpty()) {
            return "Error: SQL query cannot be empty";
        }

        // 只允许执行 SELECT 查询，防止恶意操作
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            return "Error: Only SELECT queries are allowed for security reasons";
        }

        try {
            DataSource dataSource = getDataSource();
            if (dataSource == null) {
                return "Error: Database datasource not configured";
            }

            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    ResultSetMetaData metaData = resultSet.getMetaData();

                    List<Map<String, Object>> results = new ArrayList<>();
                    int columnCount = metaData.getColumnCount();

                    // 获取列名
                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnName(i));
                    }

                    // 获取数据行，限制最多1000行以防止内存溢出
                    int maxRows = 1000;
                    while (resultSet.next() && results.size() < maxRows) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(columnNames.get(i - 1), resultSet.getObject(i));
                        }
                        results.add(row);
                    }

                    return formatResults(results, columnNames);
                }
            }
        } catch (Exception e) {
            log.error("Error executing SQL: {}", sql, e);
            // 3. 必须在 finally 中清除上下文，防止污染其他请求
            DynamicDataSourceContextHolder.clear();
            return "Error: " + e.getMessage();
        } finally {
            // 3. 必须在 finally 中清除上下文，防止污染其他请求
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 格式化查询结果
     * 返回清晰的表格格式，展示关键数据
     */
    private String formatResults(List<Map<String, Object>> results, List<String> columnNames) {
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
        result.append("\n").append("Total: ").append(results.size()).append(" rows");
        if (displayRows < results.size()) {
            result.append(" (displayed ").append(displayRows).append(" rows)");
        }
        if (displayCols < columnNames.size()) {
            result.append("\nColumns: ").append(displayCols).append(" / ").append(columnNames.size());
        }

        log.info("Successfully executed SQL query, returned {} rows", results.size());
        return result.toString();
    }

    /**
     * 格式化列名，使其更易读
     */
    private String formatColumnName(String columnName) {
        // 将下划线替换为空格，首字母大写
        String formatted = columnName.replace("_", " ");
        if (formatted.length() > 15) {
            return formatted.substring(0, 12) + "...";
        }
        return formatted;
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
            return str.substring(0, 17) + "...";
        }
        return str;
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
        return "Execute a SELECT SQL query and return the results. Example: SELECT * FROM sys_user";
    }
}
