package org.ruoyi.common.core.service;


/**
 * 通用 参数配置服务
 */
public interface ConfigService {

    /**
     * 根据配置类型和配置key获取值
     *
     * @param category
     * @param configKey
     * @return
     */
    String getConfigValue(String category,String configKey);



}
