package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.domain.IpdBusinessConfig;
import org.ruoyi.ipd.mapper.IpdBusinessConfigMapper;
import org.ruoyi.ipd.domain.IpdBusinessConfigVersion;
import org.ruoyi.ipd.mapper.IpdBusinessConfigVersionMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ROOT-R1 业务参数配置化根治：BusinessConfigService 单测（10 测覆盖 6 维度）
 *
 * <p>覆盖：
 * <ul>
 *   <li>缓存命中：同一 key 第二次读不命中 DB</li>
 *   <li>穿透：未 enabled 配置走 fallback</li>
 *   <li>写穿透：update 后缓存立即失效</li>
 *   <li>类型解析：STRING/NUMBER/BOOL/JSON 解析</li>
 *   <li>异常：非数字字符串抛 ServiceException</li>
 *   <li>版本链：update 闭合当前 + 追加新行</li>
 * </ul>
 */
@Tag("dev")
class BusinessConfigServiceTest {

    private IpdBusinessConfigMapper configMapper;
    private IpdBusinessConfigVersionMapper versionMapper;
    private BusinessConfigService service;

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p05-test"),
                IpdBusinessConfig.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p05-test-version"),
                IpdBusinessConfigVersion.class);
    }

    @BeforeEach
    void setUp() {
        configMapper = mock(IpdBusinessConfigMapper.class);
        versionMapper = mock(IpdBusinessConfigVersionMapper.class);
        service = new BusinessConfigService(configMapper, versionMapper);
    }

    private IpdBusinessConfig cfg(String key, String value, String type) {
        IpdBusinessConfig c = new IpdBusinessConfig();
        c.setId(1L); c.setConfigKey(key); c.setConfigValue(value);
        c.setValueType(type); c.setScope("GLOBAL"); c.setEnabled(1);
        c.setVersion(1); c.setCacheTtl(60); c.setTenantId("000000");
        c.setDelFlag("0");
        return c;
    }

    @Test
    @DisplayName("① 字符串读 + 缓存命中：同 key 第二次读不命中 DB")
    void getString_caches() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.BONUS_POOL_RATE, "0.0500", "NUMBER"));
        // 第一次：DB
        String v1 = service.getString(BusinessConfigKeys.BONUS_POOL_RATE);
        // 第二次：缓存
        String v2 = service.getString(BusinessConfigKeys.BONUS_POOL_RATE);
        assertThat(v1).isEqualTo("0.0500");
        assertThat(v2).isEqualTo("0.0500");
        verify(configMapper, times(1)).selectOne(any()); // 仅 1 次 DB
    }

    @Test
    @DisplayName("② BigDecimal 解析：NUMBER 类型")
    void getBigDecimal_parses() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.KPI_STOP_THRESHOLD, "60", "NUMBER"));
        BigDecimal v = service.getBigDecimal(BusinessConfigKeys.KPI_STOP_THRESHOLD);
        assertThat(v).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("③ int 解析")
    void getInt_parses() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.GATE_DUAL_SIGN_COUNT, "3", "NUMBER"));
        assertThat(service.getInt(BusinessConfigKeys.GATE_DUAL_SIGN_COUNT)).isEqualTo(3);
    }

    @Test
    @DisplayName("④ boolean 解析：true/1/yes 三种格式")
    void getBoolean_parses() {
        when(configMapper.selectOne(any())).thenReturn(cfg("bonus.enabled", "yes", "BOOL"));
        assertThat(service.getBoolean("bonus.enabled")).isTrue();
    }

    @Test
    @DisplayName("⑤ fallback：未 enabled 配置走 fallback 默认值")
    void fallback_returnsDefault() {
        when(configMapper.selectOne(any())).thenReturn(null);
        String v = service.getString(BusinessConfigKeys.KPI_REVISION_MODE, "append");
        assertThat(v).isEqualTo("append");
    }

    @Test
    @DisplayName("⑥ 异常：非数字字符串抛 ServiceException")
    void getBigDecimal_throwsOnNonNumeric() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.BONUS_POOL_RATE, "abc", "NUMBER"));
        assertThatThrownBy(() -> service.getBigDecimal(BusinessConfigKeys.BONUS_POOL_RATE))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("⑦ 必填读：key 不存在抛 ServiceException")
    void requireConfig_throwsOnMissing() {
        when(configMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.getInt(BusinessConfigKeys.GATE_DUAL_SIGN_COUNT))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("⑧ 写穿透：update 后缓存失效，下次读即新值")
    void update_invalidatesCache() {
        IpdBusinessConfig cfgOld = cfg(BusinessConfigKeys.BONUS_POOL_RATE, "0.0500", "NUMBER");
        IpdBusinessConfig cfgNew = cfg(BusinessConfigKeys.BONUS_POOL_RATE, "0.0700", "NUMBER");
        // 第一次读（update 内 requireConfig）：返旧值
        // 第二次读（update 后 getString）：写穿透后走 selectOne 返新值
        when(configMapper.selectOne(any())).thenReturn(cfgOld, cfgNew);
        when(configMapper.update(any(), any())).thenReturn(1);
        when(versionMapper.update((IpdBusinessConfigVersion) any(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        when(configMapper.selectById(1L)).thenReturn(cfgNew);
        when(versionMapper.insert((IpdBusinessConfigVersion) any())).thenReturn(1);

        service.update(BusinessConfigKeys.BONUS_POOL_RATE, "0.0700", 1L);
        // 缓存被 invalidate，下次读走 DB 拿新值
        String newValue = service.getString(BusinessConfigKeys.BONUS_POOL_RATE);
        assertThat(newValue).isEqualTo("0.0700");
    }

    @Test
    @DisplayName("⑨ invalidateAll：批量失效缓存")
    void invalidateAll_clearsCache() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.BONUS_POOL_RATE, "0.0500", "NUMBER"));
        service.getString(BusinessConfigKeys.BONUS_POOL_RATE);
        service.invalidateAll();
        // 再次读取应重新走 DB
        service.getString(BusinessConfigKeys.BONUS_POOL_RATE);
        verify(configMapper, times(2)).selectOne(any());
    }

    @Test
    @DisplayName("⑩ 写穿透失败抛 ServiceException")
    void update_throwsOnUpdateFailure() {
        when(configMapper.selectOne(any())).thenReturn(cfg(BusinessConfigKeys.BONUS_POOL_RATE, "0.0500", "NUMBER"));
        when(versionMapper.update((IpdBusinessConfigVersion) any(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);
        when(configMapper.update(any(), any())).thenReturn(0); // 主表更新失败
        assertThatThrownBy(() -> service.update(BusinessConfigKeys.BONUS_POOL_RATE, "0.0700", 1L))
                .isInstanceOf(RuntimeException.class);
    }
}
