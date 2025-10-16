package org.ruoyi.workflow.util;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisTemplateUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean lock(String key, String clientId, int lockExpireInSecond) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, clientId, lockExpireInSecond, TimeUnit.SECONDS));
    }

    public boolean unlock(String key, String clientId) {
        boolean result = false;
        if (clientId.equals(stringRedisTemplate.opsForValue().get(key))) {
            result = Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        }
        return result;
    }

}
