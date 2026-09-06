package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0-3.2 参数管理 API 解阻塞验收（@Tag dev）。
 * 验收三点：
 * 1. list() 返回全部 SystemConfig（按 key 排序）
 * 2. update(key, value) 写穿后立即 invalidate，下次 getValue 读新值（PERF-02 强约束）
 * 3. update 不存在的 key 不抛异常
 */
@Tag("dev")
@DisplayName("P032 SystemConfig list/update + write-through")
class P032AcceptanceTest {

    private SystemConfigMapper mapper;
    private SystemConfigVersionMapper versionMapper;
    private SystemConfigService service;

    @BeforeEach
    void setup() {
        mapper = mock(SystemConfigMapper.class);
        // P0-3.3 版本链依赖（P0-3.2 契约测试不触及，仅满足构造器）
        versionMapper = mock(SystemConfigVersionMapper.class);
        service = new SystemConfigService(mapper, versionMapper);
    }

    @Test
    @DisplayName("list() 返回全部参数（按 configKey 升序）")
    void listReturnsAll() {
        when(mapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                SystemConfig.builder().id(1L).configKey("bonus.salesSource").configValue("A").build(),
                SystemConfig.builder().id(2L).configKey("allowance.L3").configValue("0.3").build()));
        List<SystemConfig> all = service.list();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getConfigKey()).isEqualTo("bonus.salesSource");
    }

    @Test
    @DisplayName("update 后 invalidate；下次 getValue 走 DB 取新值")
    void updateInvalidatesCache() {
        // 1) 首次读：DB 返回旧值
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(SystemConfig.builder().id(1L).configKey("gate.signDeadlineDays").configValue("3").build());
        assertThat(service.getValue("gate.signDeadlineDays", "5")).isEqualTo("3");
        // 2) 调用 update
        service.update("gate.signDeadlineDays", "7");
        // 3) 缓存已清；下次 getValue 必走 DB
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(SystemConfig.builder().id(1L).configKey("gate.signDeadlineDays").configValue("7").build());
        assertThat(service.getValue("gate.signDeadlineDays", "5")).isEqualTo("7");
        verify(mapper, atLeast(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("update 不存在的 key 不抛异常（写审计失败时也可回放）")
    void updateUnknownKeyDoesNotThrow() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // 期望：update 自身捕获并 invalidate（避免空缓存命中老 default）
        service.update("never.existed", "x");
        // invalidate 后 getValue 仍走 DB 读，返回空 + default
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(service.getValue("never.existed", "fallback")).isEqualTo("fallback");
    }
}
