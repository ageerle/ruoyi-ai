package com.xmzs.common.core.constant;

/**
 * 缓存的key 常量
 *
 * @author Lion Li
 */
public interface CacheConstants {

    /**
     * 在线用户 redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * 参数管理 cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

}
