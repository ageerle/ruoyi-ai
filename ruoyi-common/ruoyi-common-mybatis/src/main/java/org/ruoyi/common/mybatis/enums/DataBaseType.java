package org.ruoyi.common.mybatis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.utils.StringUtils;

/**
 * 数据库类型
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum DataBaseType {

    /**
     * MySQL
     */
    MY_SQL("MySQL"),

    /**
     * Oracle
     */
    ORACLE("Oracle"),

    /**
     * PostgreSQL
     */
    POSTGRE_SQL("PostgreSQL"),

    /**
     * SQL Server
     */
    SQL_SERVER("Microsoft SQL Server");

    /**
     * 数据库类型
     */
    private final String type;

    /**
     * 根据数据库产品名称查找对应的数据库类型
     *
     * @param databaseProductName 数据库产品名称
     * @return 对应的数据库类型枚举值
     */
    public static DataBaseType find(String databaseProductName) {
        if (StringUtils.isBlank(databaseProductName)) {
            return MY_SQL;
        }
        for (DataBaseType type : values()) {
            if (type.getType().equals(databaseProductName)) {
                return type;
            }
        }
        return MY_SQL;
    }

    /**
     * 判断是否为 MySQL 类型
     */
    public boolean isMySql() {
        return this == MY_SQL;
    }

    /**
     * 判断是否为 Oracle 类型
     */
    public boolean isOracle() {
        return this == ORACLE;
    }

    /**
     * 判断是否为 PostgreSQL 类型
     */
    public boolean isPostgreSql() {
        return this == POSTGRE_SQL;
    }

    /**
     * 判断是否为 SQL Server 类型
     */
    public boolean isSqlServer() {
        return this == SQL_SERVER;
    }

}
