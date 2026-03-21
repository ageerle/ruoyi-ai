package org.ruoyi.agent.domain;

import lombok.Data;
import java.util.List;

/**
 * 数据库查询对象
 * 用于构建 SELECT 查询条件
 */
@Data
public class Query {

    /**
     * 表名
     */
    private String table;

    /**
     * 选择的字段列表
     * 可以使用 "*" 表示所有字段
     */
    private List<String> select;

    /**
     * WHERE 条件列表
     * 多个条件之间用 AND 连接
     */
    private List<Condition> where;

    /**
     * 返回结果数量限制
     * 默认 100，最大 1000
     */
    private Integer limit = 100;

    /**
     * 获取安全的 LIMIT 值
     * @return 限制数量，最多 1000
     */
    public Integer getLimit() {
        if (limit == null) {
            limit = 100;
        }
        return Math.min(limit, 1000);
    }
}
