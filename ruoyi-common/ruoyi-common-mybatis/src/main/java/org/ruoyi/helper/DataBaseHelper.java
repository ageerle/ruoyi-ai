package org.ruoyi.helper;

import cn.hutool.core.convert.Convert;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.enums.DataBaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库助手
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataBaseHelper {

    private static final DynamicRoutingDataSource DS = SpringUtils.getBean(DynamicRoutingDataSource.class);

    /**
     * 获取当前数据库类型
     */
    public static DataBaseType getDataBaseType() {
        DataSource dataSource = DS.determineDataSource();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            return DataBaseType.find(databaseProductName);
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public static boolean isMySql() {
        return DataBaseType.MY_SQL == getDataBaseType();
    }

    public static boolean isOracle() {
        return DataBaseType.ORACLE == getDataBaseType();
    }

    public static boolean isPostgerSql() {
        return DataBaseType.POSTGRE_SQL == getDataBaseType();
    }

    public static boolean isSqlServer() {
        return DataBaseType.SQL_SERVER == getDataBaseType();
    }

    public static String findInSet(Object var1, String var2) {
        DataBaseType dataBasyType = getDataBaseType();
        String var = Convert.toStr(var1);
        if (dataBasyType == DataBaseType.SQL_SERVER) {
            // charindex(',100,' , ',0,100,101,') <> 0
            return "charindex(',%s,' , ','+%s+',') <> 0".formatted(var, var2);
        } else if (dataBasyType == DataBaseType.POSTGRE_SQL) {
            // (select strpos(',0,100,101,' , ',100,')) <> 0
            return "(select strpos(','||%s||',' , ',%s,')) <> 0".formatted(var2, var);
        } else if (dataBasyType == DataBaseType.ORACLE) {
            // instr(',0,100,101,' , ',100,') <> 0
            return "instr(','||%s||',' , ',%s,') <> 0".formatted(var2, var);
        }
        // find_in_set(100 , '0,100,101')
        return "find_in_set('%s' , %s) <> 0".formatted(var, var2);
    }

    /**
     * 获取当前加载的数据库名
     */
    public static List<String> getDataSourceNameList() {
        return new ArrayList<>(DS.getDataSources().keySet());
    }

    /**
     * 获取当前连接的所有表名称
     */
    public static List<String> getCurrentDataSourceTableNameList() {
        DataSource dataSource = DS.determineDataSource();
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();
            
            // 获取所有表名
            try (var resultSet = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    tableNames.add(tableName);
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("获取表名称失败: " + e.getMessage());
        }
        return tableNames;
    }

    /**
     * 获取指定表的字段信息
     *
     * @param tableName 表名
     * @return 字段信息列表
     */
    public static List<Map<String, Object>> getTableColumnInfo(String tableName) {
        DataSource dataSource = DS.determineDataSource();
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();
            
            // 获取表字段信息
            try (ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, "%")) {
                while (resultSet.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", resultSet.getString("COLUMN_NAME"));
                    column.put("columnComment", resultSet.getString("REMARKS"));
                    column.put("dataType", resultSet.getString("TYPE_NAME"));
                    column.put("columnSize", resultSet.getInt("COLUMN_SIZE"));
                    column.put("isNullable", "YES".equals(resultSet.getString("IS_NULLABLE")));
                    column.put("ordinalPosition", resultSet.getInt("ORDINAL_POSITION"));
                    
                    // 设置默认值
                    String defaultValue = resultSet.getString("COLUMN_DEF");
                    column.put("columnDefault", defaultValue);
                    
                    columns.add(column);
                }
            }
            
            // 获取主键信息
            try (ResultSet pkResultSet = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                List<String> primaryKeys = new ArrayList<>();
                while (pkResultSet.next()) {
                    primaryKeys.add(pkResultSet.getString("COLUMN_NAME"));
                }
                
                // 标记主键字段
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("columnName");
                    column.put("isPrimaryKey", primaryKeys.contains(columnName));
                }
            }
            
        } catch (SQLException e) {
            throw new ServiceException("获取表字段信息失败: " + e.getMessage());
        }
        
        return columns;
    }
}
