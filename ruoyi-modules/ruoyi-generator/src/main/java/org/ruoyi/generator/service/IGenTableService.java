package org.ruoyi.generator.service;

/**
 * 业务 服务层
 *
 * @author Lion Li
 */
public interface IGenTableService {


    /**
     * 批量生成代码（下载方式）
     *
     * @param tableIds 表数组
     * @return 数据
     */
    byte[] downloadCode(String[] tableIds);

}
