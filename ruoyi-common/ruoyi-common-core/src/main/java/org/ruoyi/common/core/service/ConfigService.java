package org.ruoyi.common.core.service;

/**
 * 通用 参数配置服务
 *
 * @author Lion Li
 */
public interface ConfigService {

    /**
     * 根据参数 key 获取参数值
     *
     * @param configKey 参数 key
     * @return 参数值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置类型和配置key获取值
     *
     * @param category 配置类型
     * @param configKey 配置key
     * @return 配置属性
     */
    String getConfigValue(String category, String configKey);

}
