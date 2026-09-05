package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;

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
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceCacheTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        service = new SystemConfigService(systemConfigMapper);
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
}
