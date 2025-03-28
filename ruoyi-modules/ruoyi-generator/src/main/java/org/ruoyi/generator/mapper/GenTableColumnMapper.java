package org.ruoyi.generator.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.ruoyi.generator.domain.GenTableColumn;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 业务字段 数据层
 *
 * @author Lion Li
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface GenTableColumnMapper extends BaseMapperPlus<GenTableColumn, GenTableColumn> {
    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @return 列信息
     */
    List<GenTableColumn> selectDbTableColumnsByName(String tableName);

}
