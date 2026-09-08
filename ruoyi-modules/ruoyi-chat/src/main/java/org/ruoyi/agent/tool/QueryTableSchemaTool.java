package org.ruoyi.agent.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.ruoyi.agent.manager.TableSchemaManager;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;

@Component
@Slf4j
public class QueryTableSchemaTool implements BuiltinToolProvider {

    // 使用延迟初始化，避免在构造函数中调用 SpringUtils.getBean()
    DataSource getDataSource() {
        return SpringUtils.getBean(DataSource.class);
    }

    TableSchemaManager getTableSchemaManager() {
        return SpringUtils.getBean(TableSchemaManager.class);
    }

    @Tool("Query the CREATE TABLE statement (DDL) for a specific table by table name")
    public String queryTableSchema(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Error: Table name cannot be empty";
        }

        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return "Error: Invalid table name format";
        }

        try {
            if (getTableSchemaManager().getAllowedTableNames().stream()
                .noneMatch(allowed -> allowed.equalsIgnoreCase(tableName))) {
                return "Error: Table is not in AGENT_ALLOWED_TABLES";
            }
        } catch (Exception e) {
            log.error("sql_tool operation=QUERY_TABLE_SCHEMA status=FAILED errorType={}", e.getClass().getName());
            return "Error: Database schema query failed";
        }
        String sql = "SHOW CREATE TABLE `" + tableName + "`";
        // 2. 只在进入受 finally 保护的数据库区段后推入数据源上下文。
        DynamicDataSourceContextHolder.push("agent");

        try (Connection connection = getDataSource().getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getString("Create Table");
            }
            return "Table not found: " + tableName;

        } catch (Exception e) {
            log.error("sql_tool operation=QUERY_TABLE_SCHEMA status=FAILED errorType={}",
                e.getClass().getName());
            return "Error: Database schema query failed";
        } finally {
            // 3. 必须在 finally 中清除上下文，防止污染其他请求
            DynamicDataSourceContextHolder.poll();
        }
    }

    @Override
    public String getToolName() {
        return "query_table_schema";
    }

    @Override
    public String getDisplayName() {
        return "查询表结构";
    }

    @Override
    public String getDescription() {
        return "Query the CREATE TABLE statement (DDL) for a specific table by table name";
    }
}
