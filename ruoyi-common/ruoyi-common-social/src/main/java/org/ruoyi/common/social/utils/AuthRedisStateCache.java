package org.ruoyi.common.social.utils;

import lombok.AllArgsConstructor;
import me.zhyd.oauth.cache.AuthStateCache;
import org.ruoyi.common.core.constant.GlobalConstants;
import org.ruoyi.common.redis.utils.RedisUtils;

import java.time.Duration;

/**
 * 授权状态缓存
 */
@AllArgsConstructor
public class AuthRedisStateCache implements AuthStateCache {

    /**
     * 存入缓存
     *
     * @param key   缓存key
     * @param value 缓存内容
     */
    @Override
    public void cache(String key, String value) {
        // 授权超时时间 默认三分钟
        RedisUtils.setCacheObject(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, value, Duration.ofMinutes(3));
    }

    /**
     * 存入缓存
     *
     * @param key     缓存key
     * @param value   缓存内容
     * @param timeout 指定缓存过期时间(毫秒)
     */
    @Override
    public void cache(String key, String value, long timeout) {
        RedisUtils.setCacheObject(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, value, Duration.ofMillis(timeout));
    }

    /**
     * 获取缓存内容
     *
     * @param key 缓存key
     * @return 缓存内容
     */
    @Override
    public String get(String key) {
        return RedisUtils.getCacheObject(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key);
    }

    /**
     * 是否存在key，如果对应key的value值已过期，也返回false
     *
     * @param key 缓存key
     * @return true：存在key，并且value没过期；false：key不存在或者已过期
     */
    @Override
    public boolean containsKey(String key) {
        return RedisUtils.hasKey(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key);
    }
}
