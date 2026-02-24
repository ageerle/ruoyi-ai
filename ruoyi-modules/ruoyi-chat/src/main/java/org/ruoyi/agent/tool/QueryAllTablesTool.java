package org.ruoyi.agent.tool;

import java.util.List;

import org.ruoyi.agent.domain.TableStructure;
import org.ruoyi.agent.manager.TableSchemaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * 查询数据库所有表的 Tool
 * 获取指定数据库中所有表的列表
 */
@Slf4j
@Component
public class QueryAllTablesTool {

  
    @Autowired
    private TableSchemaManager tableSchemaManager; // 注入管理器
    /**
     * 查询数据库中所有表
     * 返回数据库中存在的所有表的列表
     *
     * @return 包含所有表信息的结果
     */
    @Tool("Query all tables in the database and return table names and basic information")
    public String queryAllTables() {
        try {
                 // 1. 从管理器获取所有允许的表结构信息（内部已包含初始化/缓存逻辑）
                List<TableStructure> tableSchemas = tableSchemaManager.getAllowedTableSchemas();
   
                if (tableSchemas == null || tableSchemas.isEmpty()) {
                    return "No tables found in database or cache is empty.";
                }
   
                // 2. 格式化结果
                StringBuilder result = new StringBuilder();
                result.append("Found ").append(tableSchemas.size()).append(" tables in cache:\n");
   
                for (TableStructure schema : tableSchemas) {
                    String tableName = schema.getTableName();
                    String tableType = schema.getTableType() != null ? schema.getTableType() : "TABLE";
                    String tableComment = schema.getTableComment();
   
                    result.append(String.format("- %s (%s) - %s\n",
                        tableName,
                        tableType,
                        tableComment != null ? tableComment : "No comment"));
                }
   
                log.info("Successfully retrieved {} tables from schema cache", tableSchemas.size());
                return result.toString();
   
            } catch (Exception e) {
                log.error("Error retrieving tables from cache", e);
                return "Error: " + e.getMessage();
            }
      
        
    }
}
