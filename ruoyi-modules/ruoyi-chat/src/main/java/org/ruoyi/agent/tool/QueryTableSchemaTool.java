package org.ruoyi.agent.tool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 查询表建表详情的 Tool
 * 根据表名查询该表的建表 SQL 语句
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "agent.mysql.enabled", havingValue = "true")
public class QueryTableSchemaTool {

    @Autowired(required = false)
    private DataSource agentDataSource;

    /**
     * 根据表名查询建表详情
     * 返回指定表的 CREATE TABLE 语句
     *
     * @param tableName 表名
     * @return 包含建表 SQL 的结果
     */
    @Tool("Query the CREATE TABLE statement (DDL) for a specific table by table name")
    public String queryTableSchema(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Error: Table name cannot be empty";
        }

        // 验证表名有效性，防止 SQL 注入
        if (!isValidIdentifier(tableName)) {
            return "Error: Invalid table name format";
        }

        try {
            if (agentDataSource == null) {
                return "Error: Database datasource not configured";
            }
            try (Connection connection = agentDataSource.getConnection()) {
                String sql = "SHOW CREATE TABLE " + tableName;
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String createTableSql = resultSet.getString("Create Table");
                    resultSet.close();
                    preparedStatement.close();

                    log.info("Successfully queried schema for table: {}", tableName);
                    return "CREATE TABLE DDL for " + tableName + ":\n\n" + createTableSql;
                }

                resultSet.close();
                preparedStatement.close();
                return "Error: Table not found or not accessible: " + tableName;
            }
        } catch (Exception e) {
            log.error("Error querying table schema for table: {}", tableName, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 验证是否为有效的 SQL 标识符
     */
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        return identifier.matches("^[a-zA-Z0-9_\\.]+$");
    }
}
