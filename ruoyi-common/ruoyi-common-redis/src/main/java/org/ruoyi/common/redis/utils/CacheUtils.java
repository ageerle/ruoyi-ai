package org.ruoyi.common.redis.utils;

import org.ruoyi.common.core.utils.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.RMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Set;

/**
 * 缓存操作工具类 {@link }
 *
 * @author Michelle.Chung
 * @date 2022/8/13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked"})
public class CacheUtils {

    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    /**
     * 获取缓存组内所有的KEY
     *
     * @param cacheNames 缓存组名称
     */
    public static Set<Object> keys(String cacheNames) {
        RMap<Object, Object> rmap = (RMap<Object, Object>) CACHE_MANAGER.getCache(cacheNames).getNativeCache();
        return rmap.keySet();
    }

    /**
     * 获取缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    public static <T> T get(String cacheNames, Object key) {
        Cache.ValueWrapper wrapper = CACHE_MANAGER.getCache(cacheNames).get(key);
        return wrapper != null ? (T) wrapper.get() : null;
    }

    /**
     * 保存缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     * @param value      缓存值
     */
    public static void put(String cacheNames, Object key, Object value) {
        CACHE_MANAGER.getCache(cacheNames).put(key, value);
    }

    /**
     * 删除缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    public static void evict(String cacheNames, Object key) {
        CACHE_MANAGER.getCache(cacheNames).evict(key);
    }

    /**
     * 清空缓存值
     *
     * @param cacheNames 缓存组名称
     */
    public static void clear(String cacheNames) {
        CACHE_MANAGER.getCache(cacheNames).clear();
    }

}
