package org.ruoyi.generator.service;

/**
 * 业务 服务层
 *
 * @author Lion Li
 */
public interface IGenTableService {

    /**
     * 基于表名称批量生成代码到classpath路径
     *
     * @param tableName 表名称数组
     */
    void generateCodeToClasspathByTableNames(String tableName);
}
