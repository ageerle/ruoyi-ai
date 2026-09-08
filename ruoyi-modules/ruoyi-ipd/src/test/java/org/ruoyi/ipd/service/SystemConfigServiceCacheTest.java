package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-02（立即生效方案）：SystemConfigService 只读缓存 + 写穿透失效。
 *
 * <p>背景：KPI/奖金/Gate 热路径每次 getValue 都 selectOne，日均 10 万+ 次 DB 读。
 * <p>语义（用户裁决：配置变更立即生效，不允许 TTL 窗口）：
 * <ul>
 *   <li>读：ConcurrentHashMap 缓存命中 0 查库；miss 装载 1 次</li>
 *   <li>写路径（后台端 P0-3.2 落地时）：提交后调 invalidate(key)/invalidateAll() → 下次读即新值</li>
 *   <li>空结果也缓存（防恶意 key 穿透）</li>
 * </ul>
 *
 * <p>PERF-P2-5（B-FIX-PACK-3 治理）：Caffeine 替换 ConcurrentHashMap——
 * <ul>
 *   <li>内存边界：maximumSize 防止 prod 部署后异常/恶意 key 推爆堆</li>
 *   <li>TTL：expireAfterWrite 5 分钟自动清理（写穿透失效仍立即生效，不等 TTL）</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceCacheTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    /** P0-3.3 版本链依赖（缓存语义测试不触及，仅满足构造器） */
    @Mock
    private SystemConfigVersionMapper systemConfigVersionMapper;

    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        service = new SystemConfigService(systemConfigMapper, systemConfigVersionMapper);
    }

    private SystemConfig row(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Test
    @DisplayName("同 key 两次 getValue：仅 1 次 DB 查询（缓存命中）")
    void getValue_sameKey_hitsCache() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row("k1", "v1"));

        assertThat(service.getValue("k1", "dft")).isEqualTo("v1");
        assertThat(service.getValue("k1", "dft")).isEqualTo("v1");

        verify(systemConfigMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("不同 defaultValue 不触发重复查库")
    void getValue_differentDefault_stillCached() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row("bonus.base", "1000"));

        assertThat(service.getIntValue("bonus.base", 500)).isEqualTo(1000);
        assertThat(service.getIntValue("bonus.base", 999)).isEqualTo(1000);

        verify(systemConfigMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("invalidate(key) 后再读：重新查库返回新值（立即生效语义）")
    void invalidate_thenRead_returnsFreshValue() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(row("k1", "old"))
            .thenReturn(row("k1", "new"));

        assertThat(service.getValue("k1", null)).isEqualTo("old");

        service.invalidate("k1"); // 写路径提交后调用

        assertThat(service.getValue("k1", null)).isEqualTo("new");
        verify(systemConfigMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("DB 无该 key：空结果也缓存，两次读仅 1 次查库（防穿透）+ 返回默认值")
    void getValue_missingKey_cachedNullAndReturnsDefault() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThat(service.getValue("ghost", "fallback")).isEqualTo("fallback");
        assertThat(service.getValue("ghost", "fallback2")).isEqualTo("fallback2");

        verify(systemConfigMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("invalidateAll() 清全部 key")
    void invalidateAll_clearsEverything() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(row("a", "1")).thenReturn(row("b", "2"))
            .thenReturn(row("a", "1b")).thenReturn(row("b", "2b"));

        service.getValue("a", null);
        service.getValue("b", null);

        service.invalidateAll();

        assertThat(service.getValue("a", null)).isEqualTo("1b");
        assertThat(service.getValue("b", null)).isEqualTo("2b");
        verify(systemConfigMapper, times(4)).selectOne(any(LambdaQueryWrapper.class));
    }

    // ───────────────────────────── PERF-P2-5: Caffeine 边界 + TTL ─────────────────────────────

    @Test
    @DisplayName("PERF-P2-5 maximumSize：注入容量 cache，插入远超上限的键，驱逐生效 + 最终 size 上限受控")
    void maximumSize_evictsOverflow() {
        // 注入 capacity=50 + recordStats 的 cache，便于 <1s 内完成 + 验证驱逐计数
        // executor(Runnable::run)：强制 Caffeine 在当前线程同步做维护。默认走
        // ForkJoinPool.commonPool() 异步维护，写满 200 键后立即断言 estimatedSize() /
        // evictionCount() 会读到滞后值。本机实测（2026-09-08 04:07 与 04:17）同一份代码
        // 两次全量跑分别得 21 红与 20 红，差别正是本用例 —— 属 flaky 而非契约问题。
        // 同步化只消除时序不确定性，被测契约（驱逐生效 + size 受控）与断言阈值均未改。
        Cache<String, Optional<String>> smallCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .executor(Runnable::run)
            .build();
        service.setCache(smallCache);

        // 填 200 个 key（4× 上限）触发 W-TinyLFU 驱逐
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(inv -> row("any", "v"));
        for (int i = 0; i < 200; i++) {
            service.getValue("k" + i, null);
        }

        // 驱逐必须发生（stats.evictionCount > 0）且最终 size 受最大窗口约束（远小于 200）
        // 注：W-TinyLFU 在冷启动 ramp-up 期间允许短暂超过 maximumSize，故用 1.5× 上限作为软上限
        assertThat(smallCache.stats().evictionCount()).isGreaterThan(0L);
        assertThat(smallCache.estimatedSize()).isLessThanOrEqualTo(75L); // 50 * 1.5 软上限
        assertThat(smallCache.estimatedSize()).isLessThan(200L);         // 远小于总写入数
    }

    @Test
    @DisplayName("PERF-P2-5 expireAfterWrite TTL：注入短 TTL cache，超时后重新查库")
    void expireAfterWrite_evictsAfterTtl() throws InterruptedException {
        // 注入 200ms TTL，sleep 500ms 验证自动失效
        Cache<String, Optional<String>> shortTtlCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMillis(200))
            .build();
        service.setCache(shortTtlCache);

        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(row("k1", "v1"))
            .thenReturn(row("k1", "v2"));

        assertThat(service.getValue("k1", null)).isEqualTo("v1");
        // 立即读：缓存命中，不查库
        assertThat(service.getValue("k1", null)).isEqualTo("v1");
        verify(systemConfigMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));

        Thread.sleep(500); // 超过 200ms TTL

        // TTL 过期：必须重新查库
        assertThat(service.getValue("k1", null)).isEqualTo("v2");
        verify(systemConfigMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("PERF-P2-5 recordStats：注入 statsCache，可读 estimatedSize/命中率")
    void recordStats_exposesCacheStats() {
        Cache<String, Optional<String>> statsCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
        service.setCache(statsCache);

        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row("k1", "v1"));

        service.getValue("k1", null);
        service.getValue("k1", null);
        service.getValue("k1", null);

        // 1 次装载 + 2 次命中；estimatedSize 应为 1
        assertThat(statsCache.estimatedSize()).isEqualTo(1L);
        assertThat(statsCache.stats().hitCount()).isEqualTo(2L);
        assertThat(statsCache.stats().missCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("PERF-P2-5 写穿透优先于 TTL：invalidate 后立即失效，不等 TTL")
    void invalidateBeatsTtl_invalidateIsImmediate() {
        // 注入 60 秒 TTL 的 cache，验证 invalidate() 不等 TTL
        Cache<String, Optional<String>> longTtlCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();
        service.setCache(longTtlCache);

        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(row("k1", "old"))
            .thenReturn(row("k1", "new"));

        service.getValue("k1", null);
        service.invalidate("k1"); // 写穿透应立即生效（不等 60s TTL）
        assertThat(service.getValue("k1", null)).isEqualTo("new");
        verify(systemConfigMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }
}
