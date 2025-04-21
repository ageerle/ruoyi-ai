package org.ruoyi.enums;

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
     * @return 对应的数据库类型枚举值，如果未找到则返回 null
     */
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
