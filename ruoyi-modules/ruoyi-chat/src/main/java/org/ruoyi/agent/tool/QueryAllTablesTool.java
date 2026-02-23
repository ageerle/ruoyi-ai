package org.ruoyi.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询数据库所有表的 Tool
 * 获取指定数据库中所有表的列表
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "agent.mysql.enabled", havingValue = "true")
public class QueryAllTablesTool {

    @Autowired(required = false)
    private DataSource agentDataSource;

    /**
     * 查询数据库中所有表
     * 返回数据库中存在的所有表的列表
     *
     * @return 包含所有表信息的结果
     */
    @Tool("Query all tables in the database and return table names and basic information")
    public String queryAllTables() {
        try {
            if (agentDataSource == null) {
                return "Error: Database datasource not configured";
            }

            try (Connection connection = agentDataSource.getConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"});

                List<String> tableNames = new ArrayList<>();
                List<String> tableDetails = new ArrayList<>();

                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    String tableComment = resultSet.getString("REMARKS");
                    String tableType = resultSet.getString("TABLE_TYPE");

                    tableNames.add(tableName);
                    tableDetails.add(String.format("- %s (%s) - %s",
                        tableName, tableType, tableComment != null ? tableComment : "No comment"));
                }
                resultSet.close();

                if (tableNames.isEmpty()) {
                    return "No tables found in database";
                }

                StringBuilder result = new StringBuilder();
                result.append("Found ").append(tableNames.size()).append(" tables:\n");
                for (String detail : tableDetails) {
                    result.append(detail).append("\n");
                }

                log.info("Successfully queried {} tables", tableNames.size());
                return result.toString();
            }
        } catch (Exception e) {
            log.error("Error querying all tables", e);
            return "Error: " + e.getMessage();
        }
    }
}
