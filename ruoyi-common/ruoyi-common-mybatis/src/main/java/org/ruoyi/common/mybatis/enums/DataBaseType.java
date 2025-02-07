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

    private final String type;

    public static DataBaseType find(String databaseProductName) {
        if (StringUtils.isBlank(databaseProductName)) {
            return null;
        }
        for (DataBaseType type : values()) {
            if (type.getType().equals(databaseProductName)) {
                return type;
            }
        }
        return null;
    }
}
