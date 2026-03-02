package org.ruoyi.agent.manager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.ruoyi.agent.domain.ColumnInfo;
import org.ruoyi.agent.domain.TableStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.baomidou.dynamic.datasource.annotation.DS;

import lombok.extern.slf4j.Slf4j;

/**
 * 表结构管理器
 * 负责获取和缓存数据库表结构信息
 *
 * 特点：
 * - 应用启动时自动初始化所有表结构
 * - 使用内存缓存 (ConcurrentHashMap) 确保高性能
 * - 支持按需刷新单个表的结构
 * - 延迟初始化：失败时在首次查询时重试
 */
@Slf4j
@Component
@DS("agent")
public class TableSchemaManager {
 
    @Autowired(required = false)
    private DataSource agentDataSource;

    @Value("${AGENT_ALLOWED_TABLES}")
    private String allowedTables;

    /**
     * 表结构缓存 (表名 -> 表结构)
     * 使用 ConcurrentHashMap 支持高并发访问
     */
    private final Map<String, TableStructure> schemaCache = new ConcurrentHashMap<>();

    /**
     * 缓存初始化标志
     */
    private volatile boolean initialized = false;

    /**
     * 初始化表结构缓存
     * Spring 会自动在 Bean 创建后调用此方法
     */
    public void initializeSchema() {
        if (agentDataSource == null) {
            log.warn("Agent datasource not configured, schema initialization skipped");
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                log.info("Initializing database schema cache...");
                loadAllowedTableSchemas();
                initialized = true;
                log.info("Schema cache initialized with {} tables", schemaCache.size());
              
            } catch (Exception e) {
                log.error("Failed to initialize schema cache", e);
            }
        }
    }

    /**
     * 加载所有允许的表的结构信息
     */
    private void loadAllowedTableSchemas() throws SQLException {
        List<String> allowedTables = getAllowedTableNames();
        for (String tableName : allowedTables) {
            try {
                TableStructure schema = loadTableSchema(tableName);
                if (schema != null) {
                    schemaCache.put(tableName, schema);
                    log.debug("Loaded schema for table: {}", tableName);
                }
            } catch (Exception e) {
                log.warn("Failed to load schema for table: {}", tableName, e);
            }
        }
    }

    /**
     * 从数据库加载指定表的结构信息
     */
    private TableStructure loadTableSchema(String tableName) throws SQLException {
        if (!isValidIdentifier(tableName) || !isTableAllowed(tableName)) {
            return null;
        }

        TableStructure table = new TableStructure();
        table.setTableName(tableName);

        try (Connection conn = agentDataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取表注释
            try (ResultSet tableRs = metaData.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (tableRs.next()) {
                    table.setTableComment(tableRs.getString("REMARKS"));
                    table.setTableType(tableRs.getString("TABLE_TYPE"));
                }
            }

            // 获取列信息
            List<ColumnInfo> columns = new ArrayList<>();
            try (ResultSet colRs = metaData.getColumns(conn.getCatalog(), null, tableName, null)) {
                while (colRs.next()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(colRs.getString("COLUMN_NAME"));
                    col.setColumnType(colRs.getString("TYPE_NAME"));

                    int columnSize = colRs.getInt("COLUMN_SIZE");
                    if (columnSize > 0 && !isNumericType(col.getColumnType())) {
                        col.setColumnType(col.getColumnType() + "(" + columnSize + ")");
                    }

                    col.setNullable("YES".equalsIgnoreCase(colRs.getString("IS_NULLABLE")));
                    col.setDefaultValue(colRs.getString("COLUMN_DEF"));
                    col.setColumnComment(colRs.getString("REMARKS"));
                    col.setAutoIncrement("YES".equalsIgnoreCase(colRs.getString("IS_AUTOINCREMENT")));

                    columns.add(col);
                }
            }

            // 获取主键信息
            try (ResultSet pkRs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                if (pkRs.next()) {
                    String pkName = pkRs.getString("COLUMN_NAME");
                    table.setPrimaryKey(pkName);
                    for (ColumnInfo col : columns) {
                        if (col.getColumnName().equals(pkName)) {
                            col.setPrimaryKey(true);
                            break;
                        }
                    }
                }
            }

            // 获取索引信息
            try (ResultSet indexRs = metaData.getIndexInfo(conn.getCatalog(), null, tableName, false, false)) {
                Set<String> indexedColumns = new HashSet<>();
                while (indexRs.next()) {
                    String colName = indexRs.getString("COLUMN_NAME");
                    if (colName != null) {
                        indexedColumns.add(colName);
                    }
                }
                for (ColumnInfo col : columns) {
                    if (indexedColumns.contains(col.getColumnName())) {
                        col.setIndexed(true);
                    }
                }
            }

            table.setColumns(columns);
        }

        return table;
    }

    /**
     * 获取所有允许的表的结构信息
     */
    public List<TableStructure> getAllowedTableSchemas() {
        if (!initialized) {
            initializeSchema();
        }

        List<String> allowedTables = getAllowedTableNames();
        return allowedTables.stream()
                .map(schemaCache::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有允许的表名
     */
    public List<String> getAllowedTableNames() {
        if (allowedTables == null || allowedTables.trim().isEmpty()) {
            log.warn("AGENT_ALLOWED_TABLES not configured");
            return new ArrayList<>();
        }

        return Arrays.stream(allowedTables.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 刷新指定表的结构信息
     */
    public TableStructure refreshTableSchema(String tableName) throws SQLException {
        if (!isValidIdentifier(tableName) || !isTableAllowed(tableName)) {
            return null;
        }

        TableStructure schema = loadTableSchema(tableName);
        if (schema != null) {
            schemaCache.put(tableName, schema);
        }
        return schema;
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

    /**
     * 检查表是否在允许列表中
     */
    private boolean isTableAllowed(String tableName) {
        // String allowedTables = System.getenv("AGENT_ALLOWED_TABLES");
        if (allowedTables == null || allowedTables.trim().isEmpty()) {
            return false;
        }
        Set<String> tables = new HashSet<>(Arrays.asList(allowedTables.split(",")));
        return tables.contains(tableName.trim());
    }

    /**
     * 判断是否为数值类型
     */
    private boolean isNumericType(String typeName) {
        String upper = typeName.toUpperCase();
        return upper.contains("INT") || upper.contains("FLOAT") ||
               upper.contains("DOUBLE") || upper.contains("DECIMAL");
    }
}
