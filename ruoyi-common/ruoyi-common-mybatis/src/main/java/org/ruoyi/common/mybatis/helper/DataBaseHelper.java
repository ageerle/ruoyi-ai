package org.ruoyi.common.mybatis.helper;

import cn.hutool.core.convert.Convert;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.mybatis.enums.DataBaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库助手
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataBaseHelper {

    private static final DynamicRoutingDataSource DS = SpringUtils.getBean(DynamicRoutingDataSource.class);

    /**
     * 获取当前数据源对应的数据库类型
     * <p>
     * 通过 DynamicRoutingDataSource 获取当前线程绑定的数据源，
     * 然后从数据源获取数据库连接，利用连接的元数据获取数据库产品名称，
     * 最后调用 DataBaseType.find 方法将数据库名称转换为对应的枚举类型
     *
     * @return 当前数据库对应的 DataBaseType 枚举，找不到时默认返回 MY_SQL
     * @throws ServiceException 当获取数据库连接或元数据出现异常时抛出业务异常
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

    /**
     * 根据当前数据库类型，生成兼容的 FIND_IN_SET 语句片段
     * <p>
     * 用于判断指定值是否存在于逗号分隔的字符串列中，SQL写法根据不同数据库方言自动切换：
     * - Oracle 使用 instr 函数
     * - PostgreSQL 使用 strpos 函数
     * - SQL Server 使用 charindex 函数
     * - 其他默认使用 MySQL 的 find_in_set 函数
     *
     * @param var1 要查找的值（支持任意类型，内部会转换成字符串）
     * @param var2 存储逗号分隔值的数据库列名
     * @return 适用于当前数据库的 SQL 条件字符串，通常用于 where 或 apply 中拼接
     */
    public static String findInSet(Object var1, String var2) {
        String var = Convert.toStr(var1);
        return switch (getDataBaseType()) {
            // instr(',0,100,101,' , ',100,') <> 0
            case ORACLE -> "instr(','||%s||',' , ',%s,') <> 0".formatted(var2, var);
            // (select strpos(',0,100,101,' , ',100,')) <> 0
            case POSTGRE_SQL -> "(select strpos(','||%s||',' , ',%s,')) <> 0".formatted(var2, var);
            // charindex(',100,' , ',0,100,101,') <> 0
            case SQL_SERVER -> "charindex(',%s,' , ','+%s+',') <> 0".formatted(var, var2);
            // find_in_set(100 , '0,100,101')
            default -> "find_in_set('%s' , %s) <> 0".formatted(var, var2);
        };
    }

    /**
     * 获取当前加载的数据库名
     */
    public static List<String> getDataSourceNameList() {
        return new ArrayList<>(DS.getDataSources().keySet());
    }
}
