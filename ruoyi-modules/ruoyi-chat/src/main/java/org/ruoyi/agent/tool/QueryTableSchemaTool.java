package org.ruoyi.agent.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class QueryTableSchemaTool {

    @Autowired(required = false)
    private DataSource dataSource;

    @Tool("Query the CREATE TABLE statement (DDL) for a specific table by table name")
    public String queryTableSchema(String tableName) {
        // 2. 手动推入数据源上下文
        DynamicDataSourceContextHolder.push("agent");
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Error: Table name cannot be empty";
        }

        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return "Error: Invalid table name format";
        }

        String sql = "SHOW CREATE TABLE `" + tableName + "`";

        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getString("Create Table");
            }
            return "Table not found: " + tableName;

        } catch (Exception e) {
               // 3. 必须在 finally 中清除上下文，防止污染其他请求
            DynamicDataSourceContextHolder.clear();
            log.error("Error querying table schema: {}", tableName, e);
            return "Error: " + e.getMessage();
        } finally {
            // 3. 必须在 finally 中清除上下文，防止污染其他请求
            DynamicDataSourceContextHolder.clear();
        }
    }
}
