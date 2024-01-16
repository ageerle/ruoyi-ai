package com.xmzs.common.core.service;

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

}
