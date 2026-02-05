package org.ruoyi.generator.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceMonitor;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.util.ConfigTable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * anyline 适配 动态数据源改造
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class MyBatisDataSourceMonitor implements DataSourceMonitor {

    public MyBatisDataSourceMonitor() {
        // 调整执行模式为自定义
        ConfigTable.KEEP_ADAPTER = 2;
        // 禁用缓存
        ConfigTable.METADATA_CACHE_SCOPE = 0;
    }

    private final Map<String, String> features = new HashMap<>();

    /**
     * 数据源特征 用来定准 adapter 包含数据库或JDBC协议关键字<br/>
     * 一般会通过 产品名_url 合成 如果返回null 上层方法会通过driver_产品名_url合成
     *
     * @param datasource 数据源
     * @return String 返回null由上层自动提取
     */
    @Override
    public String feature(DataRuntime runtime, Object datasource) {
        String feature = null;
        if (datasource instanceof JdbcTemplate jdbc) {
            DataSource ds = jdbc.getDataSource();
            if (ds instanceof DynamicRoutingDataSource) {
                String key = DynamicDataSourceContextHolder.peek();
                feature = features.get(key);
                if (null == feature) {
                    Connection con = null;
                    try {
                        con = DataSourceUtils.getConnection(ds);
                        DatabaseMetaData meta = con.getMetaData();
                        String url = meta.getURL();
                        feature = meta.getDatabaseProductName().toLowerCase().replace(" ", "") + "_" + url;
                        features.put(key, feature);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        if (null != con && !DataSourceUtils.isConnectionTransactional(con, ds)) {
                            DataSourceUtils.releaseConnection(con, ds);
                        }
                    }
                }
            }
        }
        return feature;
    }

    /**
     * 数据源唯一标识 如果不实现则默认feature
     * @param datasource 数据源
     * @return String 返回null由上层自动提取
     */
    @Override
    public String key(DataRuntime runtime, Object datasource) {
        if(datasource instanceof JdbcTemplate jdbc){
            DataSource ds = jdbc.getDataSource();
            if(ds instanceof DynamicRoutingDataSource){
                return DynamicDataSourceContextHolder.peek();
            }
        }
        return runtime.getKey();
    }

    /**
     * ConfigTable.KEEP_ADAPTER=2 : 根据当前接口判断是否保持同一个数据源绑定同一个adapter<br/>
     * DynamicRoutingDataSource类型的返回false,因为同一个DynamicRoutingDataSource可能对应多类数据库, 如果项目中只有一种数据库 应该直接返回true
     *
     * @param datasource 数据源
     * @return boolean
     */
    @Override
    public boolean keepAdapter(DataRuntime runtime, Object datasource) {
        if (datasource instanceof JdbcTemplate jdbc) {
            DataSource ds = jdbc.getDataSource();
            return !(ds instanceof DynamicRoutingDataSource);
        }
        return true;
    }

}
