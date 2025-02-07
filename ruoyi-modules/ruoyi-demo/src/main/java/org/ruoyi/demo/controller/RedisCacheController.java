package org.ruoyi.demo.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CacheNames;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * spring-cache 演示案例
 *
 * @author Lion Li
 */
// 类级别 缓存统一配置
//@CacheConfig(cacheNames = CacheNames.DEMO_CACHE)
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/cache")
public class RedisCacheController {

    /**
     * 测试 @Cacheable
     * <p>
     * 表示这个方法有了缓存的功能,方法的返回值会被缓存下来
     * 下一次调用该方法前,会去检查是否缓存中已经有值
     * 如果有就直接返回,不调用方法
     * 如果没有,就调用方法,然后把结果缓存起来
     * 这个注解「一般用在查询方法上」
     * <p>
     * 重点说明: 缓存注解严谨与其他筛选数据功能一起使用
     * 例如: 数据权限注解 会造成 缓存击穿 与 数据不一致问题
     * <p>
     * cacheNames 命名规则 查看 {@link CacheNames} 注释 支持多参数
     */
    @Cacheable(cacheNames = "demo:cache#60s#10m#20", key = "#key", condition = "#key != null")
    @GetMapping("/test1")
    public R<String> test1(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * 测试 @CachePut
     * <p>
     * 加了@CachePut注解的方法,会把方法的返回值put到缓存里面缓存起来,供其它地方使用
     * 它「通常用在新增或者实时更新方法上」
     * <p>
     * cacheNames 命名规则 查看 {@link CacheNames} 注释 支持多参数
     */
    @CachePut(cacheNames = CacheNames.DEMO_CACHE, key = "#key", condition = "#key != null")
    @GetMapping("/test2")
    public R<String> test2(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * 测试 @CacheEvict
     * <p>
     * 使用了CacheEvict注解的方法,会清空指定缓存
     * 「一般用在删除的方法上」
     * <p>
     * cacheNames 命名规则 查看 {@link CacheNames} 注释 支持多参数
     */
    @CacheEvict(cacheNames = CacheNames.DEMO_CACHE, key = "#key", condition = "#key != null")
    @GetMapping("/test3")
    public R<String> test3(String key, String value) {
        return R.ok("操作成功", value);
    }

    /**
     * 测试设置过期时间
     * 手动设置过期时间10秒
     * 11秒后获取 判断是否相等
     */
    @GetMapping("/test6")
    public R<Boolean> test6(String key, String value) {
        RedisUtils.setCacheObject(key, value);
        boolean flag = RedisUtils.expire(key, Duration.ofSeconds(10));
        System.out.println("***********" + flag);
        try {
            Thread.sleep(11 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Object obj = RedisUtils.getCacheObject(key);
        return R.ok(value.equals(obj));
    }

}
